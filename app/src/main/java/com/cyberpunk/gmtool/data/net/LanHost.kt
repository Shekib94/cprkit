package com.cyberpunk.gmtool.data.net

import android.util.Log
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CharacterIO
import com.cyberpunk.gmtool.data.gtr
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * سمت GM — «میزبان» سشن محلی.
 *
 * قواعد ثابت:
 *  • GM همیشه مرجع حقیقت است.
 *  • تا وقتی دکمه‌ی شبکه‌ی یک شخصیت زده نشود، آن شخصیت اصلاً درگیر شبکه نیست
 *    و GM مثل قبل دستی می‌گرداندش. بازی هرگز به‌خاطر شبکه متوقف نمی‌شود.
 *  • اطلاعات مخفی هرگز ارسال نمی‌شود — نه اینکه ارسال شود و مخفی بماند.
 *    این دیگر فقط یک قرارداد کلامی نیست: [LanTrust.sanitizeForPlayer] فیلدهای
 *    GM را پیش از ارسال پاک می‌کند.
 *  • آنچه GM تأیید می‌کند **diff سمت سرور** است، نه خلاصه‌ای که کلاینت نوشته؛
 *    و فیلدهای فقط-GM هرگز از پیشنهاد بازیکن پذیرفته نمی‌شوند ([LanTrust]).
 */
class LanHost(
    private val sessionId: Long,
    private val sessionName: String,
    /** برگه‌ی فعلی یک شخصیت را از ViewModel می‌گیرد. */
    private val characterProvider: (Int) -> Character?
) {
    private var server: TinyHttpServer? = null
    private val seq = AtomicLong(0)

    /** slotId → اطلاعات نقش */
    private val slots = ConcurrentHashMap<String, LanSlot>()

    /** token → slotId */
    private val tokens = ConcurrentHashMap<String, String>()

    /** slotId → صف رویدادهای در انتظار ارسال */
    private val outbox = ConcurrentHashMap<String, MutableList<LanEvent>>()

    /** درخواست‌های در انتظار تأیید GM */
    private val pending = ConcurrentHashMap<Long, PendingRequest>()

    /** وقتی روشن است، تغییرات ثبت می‌شوند ولی ارسال نمی‌شوند. */
    @Volatile var syncSuspended: Boolean = false
        private set

    @Volatile var lastError: String = ""
        private set

    // callbackها برای UI
    var onPendingChanged: (() -> Unit)? = null
    var onSlotsChanged: (() -> Unit)? = null

    val host: String get() = LanAddress.localIp()
    val isRunning: Boolean get() = server?.isRunning == true

    // ─────────────────────── چرخه‌ی حیات ───────────────────────

    fun start(): Boolean {
        if (isRunning) return true
        return try {
            server = TinyHttpServer(LanProtocol.PORT) { path, body -> route(path, body) }
                .also { it.start() }
            lastError = ""
            true
        } catch (e: Exception) {
            lastError = e.message ?: "خطا در راه‌اندازی سرور"
            server = null
            false
        }
    }

    fun stop() {
        broadcastAll(LanEvent(type = LanEvent.CLOSED, message = gtr("Session closed")))
        // فرصت کوتاه تا پاسخ‌های در حال پرواز به کلاینت‌ها برسد.
        // روی نخ اصلی هرگز sleep نمی‌کنیم: `stopHosting()` از کلیک UI صدا زده
        // می‌شود و ۱۵۰ms فریز رابط به‌خاطر یک پیام خداحافظی ارزشش را ندارد.
        // در آن حالت سرور بلافاصله بسته می‌شود؛ بازیکن در poll بعدی (~۹۰۰ms)
        // قطع را می‌فهمد و خودش به RECONNECTING می‌رود.
        val graceMs = if (isMainThread()) 0L else GRACEFUL_STOP_MS
        server?.stop(graceMs)
        server = null
        slots.clear(); tokens.clear(); outbox.clear(); pending.clear()
    }

    private fun isMainThread(): Boolean =
        android.os.Looper.myLooper() == android.os.Looper.getMainLooper()

    companion object {
        private const val TAG = "LanHost"
        private const val GRACEFUL_STOP_MS = 150L
        /** سقف طول خلاصه‌ای که کلاینت می‌فرستد. */
        private const val MAX_SUMMARY_CHARS = 240

        /** الفبای کد پیوستن: بدون I/O/0/1 تا خواندن دستی‌اش هم اشتباه‌ناپذیر باشد. */
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val CODE_LENGTH = 6
        private val secureRandom = java.security.SecureRandom()
    }

    // ─────────────────────── مدیریت نقش‌ها ───────────────────────

    fun slotsSnapshot(): List<LanSlot> = slots.values.sortedBy { it.label }

    fun pendingSnapshot(): List<PendingRequest> = pending.values.sortedBy { it.at }

    /** شخصیت را وارد شبکه می‌کند و بلیت QR می‌سازد. */
    fun enableSlot(characterId: Int, label: String): LanTicket {
        val slotId = "C$characterId"
        val existing = slots[slotId]
        val code = existing?.code ?: randomCode()
        slots[slotId] = (existing ?: LanSlot(slotId, characterId, label, code))
            .copy(label = label, code = code, networked = true)
        outbox.getOrPut(slotId) { mutableListOf() }
        onSlotsChanged?.invoke()
        return LanTicket(host, LanProtocol.PORT, sessionId, slotId, code, label)
    }

    /** شخصیت را از شبکه خارج می‌کند؛ GM دوباره دستی می‌گرداندش. */
    fun disableSlot(characterId: Int) {
        val slotId = "C$characterId"
        slots[slotId]?.let { s ->
            tokens.entries.removeAll { it.value == slotId }
            slots[slotId] = s.copy(networked = false, connected = false, deviceKey = "")
            outbox.remove(slotId)
        }
        onSlotsChanged?.invoke()
    }

    /**
     * بلیت «بازیکن جدید» — بازیکنی که شخصیتش روی گوشی GM نیست.
     *
     * هر فراخوانی یک slot **جدا** می‌سازد (NEW1، NEW2، …) چون سر میز هیچ‌وقت
     * نمی‌دانیم چند نفرِ تازه می‌آیند؛ سقف گذاشتن یعنی برگرداندن بازیکن.
     */
    fun newPlayerTicket(): LanTicket {
        var n = 1
        while (slots.containsKey("${LanTicket.NEW_PLAYER_PREFIX}$n")) n++
        return addNewPlayerSlot(gtr("new player %1s", n))
    }

    /** یک slot «بازیکن جدید» با برچسب دلخواه می‌سازد و بلیتش را برمی‌گرداند. */
    fun addNewPlayerSlot(label: String): LanTicket {
        var n = 1
        while (slots.containsKey("${LanTicket.NEW_PLAYER_PREFIX}$n")) n++
        val slotId = "${LanTicket.NEW_PLAYER_PREFIX}$n"
        val code = randomCode()
        slots[slotId] = LanSlot(slotId, 0, label, code, networked = true)
        outbox.getOrPut(slotId) { mutableListOf() }
        onSlotsChanged?.invoke()
        return LanTicket(host, LanProtocol.PORT, sessionId, slotId, code, label)
    }

    /**
     * حذف یک slot که هنوز کسی با آن وصل نشده (مثلاً «بازیکن جدید ۳» که
     * اشتباهی ساخته شد). slotهای وصل‌شده عمداً قابل حذف نیستند تا برگه‌ی
     * بازیکن وسط جلسه زیر پایش خالی نشود.
     */
    fun removeSlotIfUnused(slotId: String): Boolean {
        val s = slots[slotId] ?: return false
        if (s.connected) return false
        if (tokens.values.contains(slotId)) return false
        slots.remove(slotId)
        outbox.remove(slotId)
        onSlotsChanged?.invoke()
        return true
    }

    fun ticketFor(characterId: Int): LanTicket? {
        val s = slots["C$characterId"] ?: return null
        if (!s.networked) return null
        return LanTicket(host, LanProtocol.PORT, sessionId, s.slotId, s.code, s.label)
    }

    /**
     * بلیتِ هر slot با شناسه‌ی خودش — لازم برای slotهای «بازیکن جدید» که
     * characterId ندارند و قبلاً QRشان فقط یک بار، موقع ساخت، نشان داده می‌شد.
     */
    fun ticketForSlot(slotId: String): LanTicket? {
        val s = slots[slotId] ?: return null
        if (!s.networked) return null
        return LanTicket(host, LanProtocol.PORT, sessionId, s.slotId, s.code, s.label)
    }

    // ─────────────────────── ارسال به بازیکن‌ها ───────────────────────

    /** برگه‌ی به‌روزشده را برای بازیکنِ همان شخصیت می‌فرستد. */
    fun pushCharacter(character: Character) {
        if (syncSuspended) return
        val slot = slots["C${character.id}"] ?: return
        if (!slot.networked) return
        enqueue(slot.slotId, LanEvent(
            type = LanEvent.CHARACTER,
            payload = CharacterIO.singleToJson(sanitize(character))
        ))
    }

    /** چند شخصیت هم‌زمان (مثلاً پاداش سشن گروهی). */
    fun pushCharacters(characters: List<Character>) {
        characters.forEach { pushCharacter(it) }
    }

    /** پیام خصوصی درون‌داستانی فقط برای یک بازیکن. */
    fun whisper(characterId: Int, text: String) {
        val slot = slots["C$characterId"] ?: return
        enqueue(slot.slotId, LanEvent(type = LanEvent.WHISPER, message = text))
    }

    /**
     * اگر بیشتر از این مدت از یک بازیکن خبری نشود، اتصالش کهنه حساب می‌شود.
     * بازیکن هر ~۹۰۰ms پول می‌زند، پس ۲۰ ثانیه سکوت یعنی واقعاً رفته است.
     */
    private val STALE_AFTER_MS = 20_000L

    /**
     * آزاد کردن دستی یک نقش توسط GM.
     * برای وقتی که گوشی بازیکن مرده و بدون /bye رفته: نقش در شبکه می‌ماند
     * ولی دستگاه بعدی می‌تواند با همان QR وصل شود.
     */
    fun resetSlotConnection(characterId: Int) {
        val slotId = "C$characterId"
        val s = slots[slotId] ?: return
        tokens.entries.removeAll { it.value == slotId }
        slots[slotId] = s.copy(connected = false, deviceKey = "", lastSeen = 0L)
        onSlotsChanged?.invoke()
    }

    /** نقش‌هایی که مدتی سکوت کرده‌اند را آزاد می‌کند. از سمت UI صدا زده می‌شود. */
    fun sweepStaleConnections() {
        val now = System.currentTimeMillis()
        var any = false
        slots.forEach { (id, s) ->
            if (s.connected && s.lastSeen > 0 && now - s.lastSeen > STALE_AFTER_MS) {
                slots[id] = s.copy(connected = false)
                any = true
            }
        }
        if (any) onSlotsChanged?.invoke()
    }

    fun setSyncSuspended(value: Boolean) {
        val wasSuspended = syncSuspended
        syncSuspended = value
        if (wasSuspended && !value) {
            // آزادسازی: وضعیت کامل همه را دوباره بفرست
            slots.values.filter { it.networked && it.characterId != 0 }.forEach { s ->
                characterProvider(s.characterId)?.let { pushCharacter(it) }
            }
        }
    }

    // ─────────────────────── تأیید / رد درخواست ───────────────────────

    /**
     * درخواست را تأیید می‌کند.
     * برای درخواست‌های تغییر، [applyToGm] برگه‌ی پیشنهادی را روی گوشی GM اعمال می‌کند.
     */
    fun approve(requestId: Long, applyToGm: (Character) -> Unit): Boolean {
        val req = pending.remove(requestId) ?: return false
        onPendingChanged?.invoke()
        val proposed = CharacterIO.parseList(req.characterJson).firstOrNull() ?: return false

        if (req.isJoin) {
            // بازیکن جدید: شخصیت به فهرست GM اضافه می‌شود.
            // وضعیت‌های قواعدیِ سمت GM (مرگ، جراحت بحرانی، REP، یادداشت GM…)
            // پاک می‌شوند تا یک برگه‌ی از پیش ساخته‌شده نتواند با آن‌ها وارد شود.
            val cleaned = LanTrust.forNewPlayer(proposed)
            applyToGm(cleaned)
            enqueue(req.slotId, LanEvent(
                type = LanEvent.APPROVED,
                message = gtr("GM approved your connection")
            ))
            return true
        }

        // درخواست تغییر.
        // **مهم:** نسخه‌ی بازیکن بی‌کم‌وکاست روی GM نمی‌نشیند. diff در لحظه‌ی
        // درخواست محاسبه و به GM نشان داده شده، ولی برگه‌ی GM ممکن است از آن
        // لحظه عوض شده باشد؛ پس اینجا دوباره روی برگه‌ی *فعلی* GM ادغام می‌کنیم
        // و فیلدهای فقط-GM را از نسخه‌ی خودمان برمی‌گردانیم.
        val current = characterProvider(req.characterId)
        val merged = LanTrust.guardedMerge(current, proposed)
        if (merged.blocked.isNotEmpty()) {
            Log.w(TAG, "blocked GM-only fields from slot ${req.slotId}: ${merged.blocked}")
        }
        val fixed = merged.character.copy(id = req.characterId)
        applyToGm(fixed)
        val note = if (merged.blocked.isEmpty()) ""
            else " — " + gtr("GM-only fields ignored: %1s", merged.blocked.joinToString("، "))
        enqueue(req.slotId, LanEvent(
            type = LanEvent.APPROVED,
            message = gtr("approved: %1s", ChangeRequest.kindLabel(req.kind)) + note,
            payload = CharacterIO.singleToJson(sanitize(fixed))
        ))
        return true
    }

    fun reject(requestId: Long, reason: String = "") {
        val req = pending.remove(requestId) ?: return
        onPendingChanged?.invoke()
        // نسخه‌ی درست GM را برمی‌گردانیم تا گوشی بازیکن به حالت صحیح برگردد
        val current = characterProvider(req.characterId)
        enqueue(req.slotId, LanEvent(
            type = LanEvent.REJECTED,
            message = reason.ifBlank { gtr("GM rejected this change") },
            payload = current?.let { CharacterIO.singleToJson(sanitize(it)) }.orEmpty()
        ))
    }

    fun approveAll(applyToGm: (Character) -> Unit) {
        pendingSnapshot().filterNot { it.isJoin }.forEach { approve(it.id, applyToGm) }
    }

    // ─────────────────────── مسیریابی درخواست‌ها ───────────────────────

    private fun route(path: String, body: String): String = when (path) {
        LanProtocol.PATH_INFO -> LanProtocol.gson.toJson(
            LanInfo(sessionName = sessionName, open = true)
        )
        LanProtocol.PATH_JOIN -> handleJoin(body)
        LanProtocol.PATH_POLL -> handlePoll(body)
        LanProtocol.PATH_REQUEST -> handleRequest(body)
        LanProtocol.PATH_BYE -> handleBye(body)
        else -> LanProtocol.gson.toJson(SimpleResponse(false, "مسیر ناشناخته"))
    }

    private fun handleJoin(body: String): String {
        val req = runCatching { LanProtocol.gson.fromJson(body, JoinRequest::class.java) }.getOrNull()
            ?: return LanProtocol.gson.toJson(JoinResponse(error = "درخواست نامعتبر"))

        val slot = slots[req.slotId]
            ?: return LanProtocol.gson.toJson(JoinResponse(error = "این نقش در سشن نیست"))
        if (!slot.networked) {
            return LanProtocol.gson.toJson(JoinResponse(error = "این نقش هنوز وارد شبکه نشده"))
        }
        if (slot.code != req.code) {
            return LanProtocol.gson.toJson(JoinResponse(error = "کد نادرست"))
        }
        // اتصال دوم به یک نقش رد می‌شود — مگر همان دستگاه برگشته باشد.
        // ولی اگر دستگاه قبلی مدتی است سکوت کرده (باتری تمام، برنامه بسته)، دیگر
        // «وصل» حساب نمی‌شود؛ وگرنه بازیکن با گوشی دوم هیچ‌وقت نمی‌توانست برگردد.
        val stale = slot.lastSeen > 0 &&
            System.currentTimeMillis() - slot.lastSeen > STALE_AFTER_MS
        if (slot.connected && !stale && slot.deviceKey.isNotBlank() && slot.deviceKey != req.deviceKey) {
            return LanProtocol.gson.toJson(JoinResponse(error = "این نقش قبلاً به دستگاه دیگری وصل شده. اگر آن گوشی دیگر در دسترس نیست، GM می‌تواند اتصال را آزاد کند."))
        }

        val token = randomToken()
        tokens.entries.removeAll { it.value == slot.slotId }
        tokens[token] = slot.slotId
        slots[slot.slotId] = slot.copy(
            connected = true, deviceKey = req.deviceKey, lastSeen = System.currentTimeMillis()
        )
        onSlotsChanged?.invoke()

        // ── بازیکن جدید: برگه‌اش باید تأیید GM بگیرد ──
        if (LanTicket.isNewPlayerSlot(slot.slotId)) {
            val sent = req.characterJson?.let { CharacterIO.parseList(it).firstOrNull() }
                ?: return LanProtocol.gson.toJson(JoinResponse(error = "برگه‌ی شخصیت ارسال نشد"))
            val p = PendingRequest(
                slotId = slot.slotId,
                characterId = 0,
                characterName = sent.handle.ifBlank { sent.name },
                kind = "join",
                summary = "بازیکن جدید: ${sent.handle.ifBlank { sent.name }} — ${sent.role}",
                characterJson = CharacterIO.singleToJson(sent),
                isJoin = true
            )
            pending[p.id] = p
            onPendingChanged?.invoke()
            return LanProtocol.gson.toJson(JoinResponse(
                ok = true, token = token, pending = true,
                characterName = sent.handle.ifBlank { sent.name },
                sessionName = sessionName
            ))
        }

        // ── نقش موجود ──
        val gmChar = characterProvider(slot.characterId)
            ?: return LanProtocol.gson.toJson(JoinResponse(error = "شخصیت روی گوشی GM پیدا نشد"))

        // بازیکن همراه درخواستِ join، برگه‌ی محلیِ خودش را هم فرستاده (اگر داشته
        // باشد). تا پیش از این، نسخه‌ی GM بی‌صدا روی گوشی بازیکن بازنویسی می‌شد و
        // بازیکن حتی نمی‌فهمید چیزی عوض شده. حالا طبق طراحی میز: اگر دو برگه
        // فرق دارند، اختلاف با diff به صف تأیید GM می‌رود و GM انتخاب می‌کند
        // کدام گوشی منبع حقیقت است — تأیید یعنی نسخه‌ی بازیکن، رد یعنی نسخه‌ی GM.
        val playerSheet = req.characterJson?.let { CharacterIO.parseList(it).firstOrNull() }
        if (playerSheet != null) {
            val preview = LanTrust.guardedMerge(gmChar, playerSheet)
            if (preview.accepted.isNotEmpty()) {
                val conflict = PendingRequest(
                    slotId = slot.slotId,
                    characterId = slot.characterId,
                    characterName = gmChar.handle.ifBlank { gmChar.name },
                    kind = ChangeRequest.CONFLICT,
                    // خلاصه اینجا توسط خود GM ساخته می‌شود، نه کلاینت.
                    summary = gtr("approve = player's copy wins • reject = GM's copy wins"),
                    characterJson = CharacterIO.singleToJson(playerSheet),
                    isJoin = false,
                    diff = preview.accepted,
                    // uid متفاوت در این حالت «خرابی» نیست، فقط یعنی دو گوشی
                    // جداگانه برگه را ساخته‌اند؛ نشان‌دادنش به GM نویز است.
                    blocked = preview.blocked.filterNot { it == "sheetUid" }
                )
                pending[conflict.id] = conflict
                onPendingChanged?.invoke()
                Log.i(TAG, "sheet conflict on slot ${slot.slotId}: ${preview.accepted.size} fields differ")
                return LanProtocol.gson.toJson(JoinResponse(
                    ok = true,
                    token = token,
                    pending = true,
                    characterId = gmChar.id,
                    characterName = gmChar.handle.ifBlank { gmChar.name },
                    // تا وقتی GM تصمیم نگرفته، هیچ برگی روی گوشی بازیکن نوشته نشود.
                    characterJson = "",
                    sessionName = sessionName
                ))
            }
        }

        // بدون اختلاف، یا بازیکن برگه را ندارد: نسخه‌ی GM می‌رود.
        // این هم حالت «برگه را ندارم» را حل می‌کند و هم «نسخه‌ها یکی هستند» را.
        return LanProtocol.gson.toJson(JoinResponse(
            ok = true,
            token = token,
            characterId = gmChar.id,
            characterName = gmChar.handle.ifBlank { gmChar.name },
            characterJson = CharacterIO.singleToJson(sanitize(gmChar)),
            sessionName = sessionName
        ))
    }

    private fun handlePoll(body: String): String {
        val token = runCatching {
            LanProtocol.gson.fromJson(body, Map::class.java)["token"] as? String
        }.getOrNull().orEmpty()
        val slotId = tokens[token]
            ?: return LanProtocol.gson.toJson(PollResponse(false, "نشست منقضی شده"))

        slots[slotId]?.let {
            slots[slotId] = it.copy(connected = true, lastSeen = System.currentTimeMillis())
        }

        val box = outbox[slotId] ?: mutableListOf()
        val events: List<LanEvent>
        synchronized(box) {
            events = box.toList()
            box.clear()
        }
        return LanProtocol.gson.toJson(PollResponse(ok = true, events = events))
    }

    private fun handleRequest(body: String): String {
        val req = runCatching { LanProtocol.gson.fromJson(body, ChangeRequest::class.java) }.getOrNull()
            ?: return LanProtocol.gson.toJson(SimpleResponse(false, "درخواست نامعتبر"))
        val slotId = tokens[req.token]
            ?: return LanProtocol.gson.toJson(SimpleResponse(false, "نشست منقضی شده"))
        val slot = slots[slotId]
            ?: return LanProtocol.gson.toJson(SimpleResponse(false, "نقش پیدا نشد"))

        val proposed = CharacterIO.parseList(req.characterJson).firstOrNull()
            ?: return LanProtocol.gson.toJson(
                SimpleResponse(false, "برگه‌ی پیشنهادی خوانده نشد")
            )
        val current = characterProvider(slot.characterId)

        // پیش‌نمایش ادغام: همین حالا مشخص می‌شود چه چیزی پذیرفته و چه چیزی رد
        // می‌شود، تا GM بر اساس واقعیت تأیید کند نه خلاصه‌ی نوشته‌ی کلاینت.
        // (approve دوباره روی برگه‌ی آن‌لحظه‌ی GM ادغام می‌کند.)
        val preview = LanTrust.guardedMerge(current, proposed)

        val p = PendingRequest(
            slotId = slotId,
            characterId = slot.characterId,
            characterName = slot.label,
            kind = req.kind,
            // خلاصه‌ی کلاینت فقط راهنماست؛ برای جلوگیری از پرکردن UI با متن
            // دلخواه، طولش محدود می‌شود.
            summary = req.summary.take(MAX_SUMMARY_CHARS),
            characterJson = CharacterIO.singleToJson(preview.character),
            isJoin = false,
            diff = preview.accepted,
            blocked = preview.blocked
        )
        pending[p.id] = p
        onPendingChanged?.invoke()
        if (preview.blocked.isNotEmpty()) {
            Log.w(TAG, "request from slot $slotId touched GM-only fields: ${preview.blocked}")
        }
        return LanProtocol.gson.toJson(SimpleResponse(true))
    }

    private fun handleBye(body: String): String {
        val token = runCatching {
            LanProtocol.gson.fromJson(body, Map::class.java)["token"] as? String
        }.getOrNull().orEmpty()
        val slotId = tokens.remove(token)
        if (slotId != null) {
            slots[slotId]?.let { slots[slotId] = it.copy(connected = false) }
            onSlotsChanged?.invoke()
        }
        return LanProtocol.gson.toJson(SimpleResponse(true))
    }

    // ─────────────────────── کمکی ───────────────────────

    private fun enqueue(slotId: String, event: LanEvent) {
        val box = outbox.getOrPut(slotId) { mutableListOf() }
        synchronized(box) {
            box.add(event.copy(seq = seq.incrementAndGet()))
            // اگر بازیکن مدتی قطع بوده، صف را کوتاه نگه دار
            while (box.size > 60) box.removeAt(0)
        }
    }

    private fun broadcastAll(event: LanEvent) {
        slots.keys.forEach { enqueue(it, event) }
    }

    /**
     * برگه‌ای که به گوشی بازیکن می‌رود.
     *
     * اطلاعات مخفی GM (یادداشت GM، کتاب کمپین، اهداف پنهان، برگه‌ی NPCها)
     * در ساختار Campaign نگهداری می‌شوند و اصلاً از این مسیر عبور نمی‌کنند.
     * اما خودِ [Character] هم سه فیلد GM-Only دارد (`notes`، `npcCategory`،
     * `npcTier`) — به‌ویژه وقتی GM سهواً شبکه را روی برگه‌ی یک NPC روشن کند.
     * قبلاً این تابع `= c` بود، یعنی آن قرارداد فقط در کامنت وجود داشت؛
     * حالا واقعاً اجرا می‌شود.
     */
    private fun sanitize(c: Character): Character = LanTrust.sanitizeForPlayer(c)

    /**
     * کد شش‌رقمی روی QR.
     * از [java.security.SecureRandom] می‌آید نه `kotlin.random.Random` پیش‌فرض:
     * این کد تنها مانع پیوستن یک گوشی ناشناس به نقش یک بازیکن است و
     * باید غیرقابل پیش‌بینی باشد (۳۰ بیت آنتروپی، بدون I/O یا O در الفبا).
     */
    private fun randomCode(): String =
        (1..CODE_LENGTH).map { CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)] }
            .joinToString("")

    private fun randomToken(): String =
        java.util.UUID.randomUUID().toString().replace("-", "")
}
