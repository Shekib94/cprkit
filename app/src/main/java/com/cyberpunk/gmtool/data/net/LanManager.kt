package com.cyberpunk.gmtool.data.net

import android.content.Context
import com.cyberpunk.gmtool.data.Character
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * تنها نقطه‌ی ورود سشن محلی برای لایه‌ی UI و ViewModel.
 *
 * نقش دستگاه یکی از این‌هاست:
 *   • GM     — سرور را اجرا می‌کند و مرجع حقیقت است
 *   • بازیکن — به سشن وصل می‌شود و برگه‌اش را دریافت می‌کند
 *   • هیچ    — برنامه دقیقاً مثل قبل و کاملاً آفلاین کار می‌کند
 *
 * **مهم:** تا وقتی GM سشن نساخته یا بازیکن جوین نشده،
 * هیچ سربار شبکه‌ای وجود ندارد و رفتار برنامه هیچ تغییری نمی‌کند.
 */
class LanManager(private val appContext: Context) {

    private val _role = MutableStateFlow(LanRole.NONE)
    val role: StateFlow<LanRole> = _role.asStateFlow()

    private val _state = MutableStateFlow(LanConnState.OFFLINE)
    val state: StateFlow<LanConnState> = _state.asStateFlow()

    private val _slots = MutableStateFlow<List<LanSlot>>(emptyList())
    val slots: StateFlow<List<LanSlot>> = _slots.asStateFlow()

    private val _pending = MutableStateFlow<List<PendingRequest>>(emptyList())
    val pending: StateFlow<List<PendingRequest>> = _pending.asStateFlow()

    private val _syncSuspended = MutableStateFlow(false)
    val syncSuspended: StateFlow<Boolean> = _syncSuspended.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /** پیام‌های خصوصی GM به این بازیکن. */
    private val _whispers = MutableStateFlow<List<String>>(emptyList())
    val whispers: StateFlow<List<String>> = _whispers.asStateFlow()

    /**
     * شناسه‌ی شخصیتی که این دستگاه (به‌عنوان بازیکن) کنترل می‌کند.
     *
     * **همیشه id محلیِ دیتابیس خودِ این گوشی است، نه id شبکه.** قبلاً id شبکه
     * (id برگه در دیتابیس GM) اینجا می‌نشست؛ وقتی بازیکن همان برگه را از قبل
     * داشت، آن id به ردیف محلیِ بی‌ربطی اشاره می‌کرد و pushهای بعدی
     * شخصیتِ تکراری می‌ساختند.
     */
    private val _myCharacterId = MutableStateFlow<Int?>(null)
    val myCharacterId: StateFlow<Int?> = _myCharacterId.asStateFlow()

    /**
     * نگاشتِ id شبکه (GM) → id محلی، تا pushهای بعدی بدون ساختن کپی تازه
     * همان ردیف محلی را به‌روز کنند.
     */
    private val netToLocal = mutableMapOf<Int, Int>()

    /** برگه‌ی محلی‌ای که موقع join انتخاب شد — لنگر هویتِ اتصال فعلی. */
    private var associatedLocalId: Int? = null

    /** برای ViewModel: نگاشت شبکه→محلیِ یادت مانده. */
    fun localIdForNetworkId(netId: Int): Int? = netToLocal[netId]

    /** برای ViewModel: برگه‌ای که این اتصال به آن گره خورده. */
    val associatedLocalCharacterId: Int? get() = associatedLocalId

    /**
     * حلقه‌ی poll باید تا وقتی برنامه باز است زنده بماند.
     * قبلاً scope از داخل دیالوگ می‌آمد و لحظه‌ای که دیالوگ بسته می‌شد
     * حلقه هم کنسل می‌شد — اتصال برقرار بود ولی هیچ داده‌ای نمی‌رسید.
     */
    private val netScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sweepJob: Job? = null

    var host: LanHost? = null
        private set
    var client: LanClient? = null
        private set

    /** ViewModel این‌ها را وصل می‌کند. */
    var characterProvider: (Int) -> Character? = { null }
    /**
     * برگه‌ای که از شبکه رسید را ذخیره می‌کند و **id محلیِ** ردیفی را که
     * به‌روز/ساخته شد برمی‌گرداند تا نگاشت شبکه→محلی درست بماند.
     * آرگومان اول id همان برگه در دیتابیس GM است.
     */
    var onCharacterFromNetwork: ((Int, Character) -> Int)? = null
    var onCharacterAdded: ((Character) -> Unit)? = null

    fun clearToast() { _toast.value = null }

    fun consumeWhisper(index: Int) {
        _whispers.value = _whispers.value.filterIndexed { i, _ -> i != index }
    }

    // ─────────────────────── سمت GM ───────────────────────

    fun startHosting(sessionId: Long, sessionName: String): Boolean {
        if (_role.value == LanRole.PLAYER) return false
        val h = LanHost(sessionId, sessionName) { id -> characterProvider(id) }
        // هندلرهای سرور روی نخ‌های pool اجرا می‌شوند؛ به‌روزرسانی UI باید Main باشد.
        h.onSlotsChanged = { netScope.launch(Dispatchers.Main) { _slots.value = h.slotsSnapshot() } }
        h.onPendingChanged = { netScope.launch(Dispatchers.Main) { _pending.value = h.pendingSnapshot() } }
        val ok = h.start()
        if (ok) {
            host = h
            _role.value = LanRole.GM
            _state.value = LanConnState.ONLINE
            _slots.value = h.slotsSnapshot()
            // هر ۱۰ ثانیه نقش‌های سکوت‌کرده را آزاد کن تا فهرست GM واقعی بماند.
            sweepJob?.cancel()
            sweepJob = netScope.launch {
                while (isActive) {
                    delay(10_000)
                    val cur = host ?: break
                    cur.sweepStaleConnections()
                    withContext(Dispatchers.Main) { _slots.value = cur.slotsSnapshot() }
                }
            }
        } else {
            _state.value = LanConnState.ERROR
            _toast.value = h.lastError.ifBlank { "سرور راه‌اندازی نشد" }
        }
        return ok
    }

    fun stopHosting() {
        sweepJob?.cancel()
        sweepJob = null
        host?.stop()
        host = null
        _role.value = LanRole.NONE
        _state.value = LanConnState.OFFLINE
        _slots.value = emptyList()
        _pending.value = emptyList()
        _syncSuspended.value = false
    }

    fun enableSlot(characterId: Int, label: String): LanTicket? {
        val h = host ?: return null
        val t = h.enableSlot(characterId, label)
        _slots.value = h.slotsSnapshot()
        return t
    }

    /** آزاد کردن دستی اتصال یک نقش (گوشی بازیکن مرده یا عوض شده). */
    fun resetSlotConnection(characterId: Int) {
        val h = host ?: return
        h.resetSlotConnection(characterId)
        _slots.value = h.slotsSnapshot()
    }

    fun disableSlot(characterId: Int) {
        host?.disableSlot(characterId)
        _slots.value = host?.slotsSnapshot().orEmpty()
    }

    fun newPlayerTicket(): LanTicket? {
        val h = host ?: return null
        val t = h.newPlayerTicket()
        _slots.value = h.slotsSnapshot()
        return t
    }

    fun ticketFor(characterId: Int): LanTicket? = host?.ticketFor(characterId)

    fun ticketForSlot(slotId: String): LanTicket? = host?.ticketForSlot(slotId)

    /** حذف slot «بازیکن جدید» که کسی با آن وصل نشده (ساخته‌ی اشتباهی). */
    fun removeSlotIfUnused(slotId: String) {
        val h = host ?: return
        if (h.removeSlotIfUnused(slotId)) _slots.value = h.slotsSnapshot()
    }

    fun approve(requestId: Long) {
        val h = host ?: return
        h.approve(requestId) { ch ->
            if (ch.id == 0 || characterProvider(ch.id) == null) onCharacterAdded?.invoke(ch)
            else onCharacterFromNetwork?.invoke(ch.id, ch)
        }
        _pending.value = h.pendingSnapshot()
    }

    fun reject(requestId: Long, reason: String = "") {
        val h = host ?: return
        h.reject(requestId, reason)
        _pending.value = h.pendingSnapshot()
    }

    fun approveAll() {
        val h = host ?: return
        h.approveAll { ch -> onCharacterFromNetwork?.invoke(ch.id, ch) }
        _pending.value = h.pendingSnapshot()
    }

    fun setSyncSuspended(v: Boolean) {
        host?.setSyncSuspended(v)
        _syncSuspended.value = v
    }

    fun whisper(characterId: Int, text: String) = host?.whisper(characterId, text)

    /**
     * از `CharacterViewModel.persist()` صدا زده می‌شود.
     * هر تغییری روی گوشی GM خودبه‌خود به بازیکنِ همان شخصیت می‌رسد — بدون هیچ تأییدی.
     */
    fun broadcastCharacters(characters: List<Character>) {
        val h = host ?: return
        if (_role.value != LanRole.GM) return
        h.pushCharacters(characters)
    }

    // ─────────────────────── سمت بازیکن ───────────────────────

    private fun ensureClient(): LanClient {
        client?.let { return it }
        val c = LanClient(appContext)
        // callbackها از نخ IO می‌آیند. اعمال برگه به ViewModel و StateFlowها
        // باید روی نخ اصلی باشد، وگرنه یا چیزی رندر نمی‌شود یا کرش می‌دهد.
        c.onCharacter = { ch ->
            netScope.launch(Dispatchers.Main) {
                val localId = onCharacterFromNetwork?.invoke(ch.id, ch)
                if (localId != null) {
                    netToLocal[ch.id] = localId
                    if (_role.value == LanRole.PLAYER) _myCharacterId.value = localId
                } else {
                    _myCharacterId.value = null
                }
            }
        }
        c.onMessage = { m -> netScope.launch(Dispatchers.Main) { _toast.value = m } }
        c.onWhisper = { w -> netScope.launch(Dispatchers.Main) { _whispers.value = _whispers.value + w } }
        c.onStateChanged = { netScope.launch(Dispatchers.Main) { _state.value = c.state } }
        c.onClosed = {
            _role.value = LanRole.NONE
            _state.value = LanConnState.OFFLINE
        }
        client = c
        return c
    }

    /**
     * @param localCharacterId id محلیِ برگه‌ای که بازیکن از فهرست خودش انتخاب
     * کرد (برای «بازیکن جدید» یا «برگه را ندارم» null). این لنگر هویت است:
     * pushهای GM به همین ردیف محلی می‌نشینند و کپی تکراری ساخته نمی‌شود.
     */
    suspend fun joinWithTicket(
        ticket: LanTicket,
        scope: CoroutineScope,
        myCharacter: Character?,
        playerName: String,
        localCharacterId: Int? = null
    ): JoinResponse {
        val c = ensureClient()
        // نشست تازه = نگاشت‌های نشست قبلی باطل.
        netToLocal.clear()
        associatedLocalId = localCharacterId
        if (localCharacterId != null) c.rememberLocalId(localCharacterId)
        val res = c.connectWithTicket(ticket, myCharacter, playerName)
        if (res.ok) {
            _role.value = LanRole.PLAYER
            // _myCharacterId را ست نمی‌کنیم: اگر GM برگی فرستاده باشد،
            // هندلرِ onCharacter آن را با id **محلی** مقداردهی می‌کند.
            c.startLoop(netScope, { _myCharacterId.value?.let { characterProvider(it) } }, playerName)
            _toast.value = if (res.pending) {
                "درخواست فرستاده شد؛ منتظر تأیید GM"
            } else {
                "به سشن «${res.sessionName}» وصل شدید"
            }
        } else {
            _toast.value = res.error.ifBlank { "اتصال برقرار نشد" }
        }
        return res
    }

    /** اتصال مجدد خودکار با بلیت ذخیره‌شده — بدون اسکن دوباره. */
    suspend fun tryAutoReconnect(scope: CoroutineScope, playerName: String): Boolean {
        val c = ensureClient()
        if (!c.hasSavedTicket()) return false
        // برگه‌ی گره‌خورده از دفعه‌ی قبل را از prefs بازیابی کن تا هم هویت
        // حفظ شود و هم اگر وسط غیبت تغییری کرده باشی، GM اختلاف را ببیند.
        val remembered = c.savedLocalId().takeIf { it > 0 }
        netToLocal.clear()
        associatedLocalId = remembered
        val sheet = (remembered ?: _myCharacterId.value)?.let { characterProvider(it) }
        val res = c.reconnect(sheet, playerName)
        if (res.ok) {
            _role.value = LanRole.PLAYER
            c.startLoop(netScope, { _myCharacterId.value?.let { characterProvider(it) } }, playerName)
        }
        return res.ok
    }

    fun hasSavedTicket(): Boolean = ensureClient().hasSavedTicket()
    fun savedLabel(): String = ensureClient().savedLabel()

    fun leaveSession(scope: CoroutineScope = netScope) {
        client?.leave(netScope)
        client?.forget()
        client = null
        _role.value = LanRole.NONE
        _state.value = LanConnState.OFFLINE
        _myCharacterId.value = null
        netToLocal.clear()
        associatedLocalId = null
    }

    /**
     * بازیکن می‌خواهد چیزی را تغییر دهد → درخواست به GM می‌رود.
     * برمی‌گرداند آیا درخواست ارسال شد یا نه.
     */
    suspend fun requestChange(kind: String, summary: String, proposed: Character): Boolean {
        val c = client ?: return false
        if (_role.value != LanRole.PLAYER) return false
        val ok = c.sendRequest(kind, summary, proposed)
        _toast.value = if (ok) "درخواست برای GM فرستاده شد" else "درخواست فرستاده نشد"
        return ok
    }

    /** آیا این دستگاه بازیکنِ متصل است؟ اگر بله، تغییرات باید درخواست شوند. */
    fun isPlayerConnected(): Boolean =
        _role.value == LanRole.PLAYER && _state.value != LanConnState.OFFLINE
}
