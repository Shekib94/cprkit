package com.cyberpunk.gmtool.data.net

import android.content.Context
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CharacterIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * سمت بازیکن — «مهمان» سشن محلی.
 *
 * پس از اولین اسکن، یک کلید ماندگار ذخیره می‌شود؛
 * از آن پس اتصال مجدد **خودکار و بی‌صدا** است — بدون اسکن دوباره،
 * چه وسط جلسه قطع شود چه جلسه‌ی بعد.
 */
class LanClient(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("cpr_lan_client", Context.MODE_PRIVATE)

    private var pollJob: Job? = null

    @Volatile var state: LanConnState = LanConnState.OFFLINE
        private set
    @Volatile var lastError: String = ""
        private set
    @Volatile var sessionName: String = ""
        private set
    @Volatile var characterName: String = ""
        private set
    @Volatile var awaitingApproval: Boolean = false
        private set

    // callbackها
    var onCharacter: ((Character) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null
    var onWhisper: ((String) -> Unit)? = null
    var onStateChanged: (() -> Unit)? = null
    var onClosed: (() -> Unit)? = null

    // ─────────────── حافظه‌ی ماندگار برای اتصال مجدد ───────────────

    private var host: String
        get() = prefs.getString("host", "").orEmpty()
        set(v) { prefs.edit().putString("host", v).apply() }

    private var port: Int
        get() = prefs.getInt("port", LanProtocol.PORT)
        set(v) { prefs.edit().putInt("port", v).apply() }

    private var slotId: String
        get() = prefs.getString("slot", "").orEmpty()
        set(v) { prefs.edit().putString("slot", v).apply() }

    private var code: String
        get() = prefs.getString("code", "").orEmpty()
        set(v) { prefs.edit().putString("code", v).apply() }

    private var token: String
        get() = prefs.getString("token", "").orEmpty()
        set(v) { prefs.edit().putString("token", v).apply() }

    val deviceKey: String
        get() {
            var k = prefs.getString("device", "").orEmpty()
            if (k.isBlank()) {
                k = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
                prefs.edit().putString("device", k).apply()
            }
            return k
        }

    /**
     * id محلیِ برگه‌ای که این اتصال به آن گره خورده — ماندگار در prefs تا
     * اتصال مجددِ خودکار بعد از بستن برنامه هم هویت را گم نکند.
     */
    fun rememberLocalId(localId: Int) {
        prefs.edit().putInt("localId", localId).apply()
    }

    fun savedLocalId(): Int = prefs.getInt("localId", 0)

    /** آیا بلیت ذخیره‌شده‌ای برای اتصال مجدد خودکار داریم؟ */
    fun hasSavedTicket(): Boolean = host.isNotBlank() && slotId.isNotBlank() && code.isNotBlank()

    fun savedLabel(): String = prefs.getString("label", "").orEmpty()

    fun forget() {
        prefs.edit().clear().apply()
        stop()
    }

    // ─────────────────────── اتصال ───────────────────────

    /** اتصال با بلیت تازه‌اسکن‌شده. */
    suspend fun connectWithTicket(
        ticket: LanTicket,
        myCharacter: Character?,
        playerName: String
    ): JoinResponse = withContext(Dispatchers.IO) {
        host = ticket.host
        port = ticket.port
        slotId = ticket.slotId
        code = ticket.code
        prefs.edit().putString("label", ticket.label).apply()
        doJoin(myCharacter, playerName)
    }

    /** اتصال مجدد با بلیت ذخیره‌شده — بی‌صدا. */
    suspend fun reconnect(myCharacter: Character?, playerName: String): JoinResponse =
        withContext(Dispatchers.IO) {
            if (!hasSavedTicket()) {
                JoinResponse(error = "بلیتی ذخیره نشده")
            } else {
                doJoin(myCharacter, playerName)
            }
        }

    private fun doJoin(myCharacter: Character?, playerName: String): JoinResponse {
        setState(LanConnState.STARTING)

        // برگه‌ی محلی همیشه فرستاده می‌شود، نه فقط برای «بازیکن جدید»:
        //  • نقش جدید  → GM باید ببیند چه چیزی می‌خواهد وارد فهرستش شود.
        //  • نقش موجود → اگر بازیکن همان برگه را (با تفاوت‌هایی) داشته باشد،
        //    GM به‌جای بازنویسی بی‌صدا، اختلاف را می‌بیند و منبع حقیقت را
        //    انتخاب می‌کند. بدون این، گوشی بازیکن همیشه بازنده‌ی بی‌خبر بود.
        val payloadJson = myCharacter?.let { CharacterIO.singleToJson(it) }

        val body = LanProtocol.gson.toJson(
            JoinRequest(
                code = code,
                slotId = slotId,
                deviceKey = deviceKey,
                playerName = playerName,
                characterJson = payloadJson
            )
        )
        val raw = TinyHttpClient.post(host, port, LanProtocol.PATH_JOIN, body)
        if (raw == null) {
            lastError = "به گوشی GM وصل نشد"
            setState(LanConnState.ERROR)
            return JoinResponse(error = lastError)
        }
        val res = runCatching {
            LanProtocol.gson.fromJson(raw, JoinResponse::class.java)
        }.getOrNull() ?: JoinResponse(error = "پاسخ نامعتبر")

        if (!res.ok) {
            lastError = res.error
            setState(LanConnState.ERROR)
            return res
        }

        token = res.token
        sessionName = res.sessionName
        characterName = res.characterName
        awaitingApproval = res.pending

        // برگه‌ای که GM فرستاده را اعمال کن.
        // این هم حالت «برگه را ندارم» را پوشش می‌دهد و هم «نسخه‌ها فرق دارند».
        if (res.characterJson.isNotBlank()) {
            CharacterIO.parseList(res.characterJson).firstOrNull()?.let { onCharacter?.invoke(it) }
        }

        setState(LanConnState.ONLINE)
        return res
    }

    // ─────────────────────── حلقه‌ی دریافت ───────────────────────

    /**
     * حلقه‌ی دریافت رویداد + اتصال مجدد خودکار.
     * اگر ارتباط قطع شود، خودش تلاش می‌کند برگردد؛ هیچ کاری از کاربر لازم نیست.
     */
    fun startLoop(
        scope: CoroutineScope,
        myCharacterProvider: () -> Character?,
        playerName: String
    ) {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch(Dispatchers.IO) {
            var backoff = 1000L
            while (isActive) {
                if (token.isBlank()) {
                    // تلاش برای اتصال مجدد
                    if (hasSavedTicket()) {
                        setState(LanConnState.RECONNECTING)
                        val r = doJoin(myCharacterProvider(), playerName)
                        if (r.ok) backoff = 1000L else { delay(backoff); backoff = (backoff * 2).coerceAtMost(15_000L) }
                    } else {
                        delay(2000); continue
                    }
                }

                val raw = TinyHttpClient.post(
                    host, port, LanProtocol.PATH_POLL,
                    """{"token":"$token"}""", timeoutMs = 10_000
                )
                if (raw == null) {
                    setState(LanConnState.RECONNECTING)
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(15_000L)
                    continue
                }
                backoff = 1000L

                val res = runCatching {
                    LanProtocol.gson.fromJson(raw, PollResponse::class.java)
                }.getOrNull()

                if (res == null || !res.ok) {
                    // نشست منقضی شده — با بلیت ذخیره‌شده دوباره وصل شو
                    token = ""
                    setState(LanConnState.RECONNECTING)
                    delay(1000)
                    continue
                }

                setState(LanConnState.ONLINE)
                res.events.forEach { handleEvent(it) }
                delay(900)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        setState(LanConnState.OFFLINE)
    }

    fun leave(scope: CoroutineScope) {
        val t = token
        scope.launch(Dispatchers.IO) {
            if (t.isNotBlank()) TinyHttpClient.post(host, port, LanProtocol.PATH_BYE, """{"token":"$t"}""")
        }
        token = ""
        stop()
    }

    private fun handleEvent(e: LanEvent) {
        when (e.type) {
            LanEvent.CHARACTER -> {
                CharacterIO.parseList(e.payload).firstOrNull()?.let { onCharacter?.invoke(it) }
            }
            LanEvent.APPROVED -> {
                awaitingApproval = false
                if (e.payload.isNotBlank()) {
                    CharacterIO.parseList(e.payload).firstOrNull()?.let { onCharacter?.invoke(it) }
                }
                onMessage?.invoke(e.message.ifBlank { "تأیید شد" })
                onStateChanged?.invoke()
            }
            LanEvent.REJECTED -> {
                // «منتظر تأیید GM» باید با رد شدن هم تمام شود، وگرنه بازیکن
                // بعد از تصمیم GM (مثلاً در تعارض برگه‌ها) روی حالت انتظار می‌ماند.
                awaitingApproval = false
                if (e.payload.isNotBlank()) {
                    CharacterIO.parseList(e.payload).firstOrNull()?.let { onCharacter?.invoke(it) }
                }
                onMessage?.invoke(e.message.ifBlank { "رد شد" })
            }
            LanEvent.WHISPER -> onWhisper?.invoke(e.message)
            LanEvent.CLOSED -> {
                onMessage?.invoke(e.message.ifBlank { "سشن بسته شد" })
                token = ""
                stop()
                onClosed?.invoke()
            }
        }
    }

    // ─────────────────────── ارسال درخواست تغییر ───────────────────────

    /** بازیکن هرگز مستقیم تغییر نمی‌دهد؛ همیشه درخواست می‌فرستد. */
    suspend fun sendRequest(kind: String, summary: String, proposed: Character): Boolean =
        withContext(Dispatchers.IO) {
            if (token.isBlank()) return@withContext false
            val body = LanProtocol.gson.toJson(
                ChangeRequest(
                    token = token,
                    kind = kind,
                    summary = summary,
                    characterJson = CharacterIO.singleToJson(proposed)
                )
            )
            val raw = TinyHttpClient.post(host, port, LanProtocol.PATH_REQUEST, body)
                ?: return@withContext false
            runCatching {
                LanProtocol.gson.fromJson(raw, SimpleResponse::class.java).ok
            }.getOrDefault(false)
        }

    private fun setState(s: LanConnState) {
        if (state != s) {
            state = s
            onStateChanged?.invoke()
        }
    }
}
