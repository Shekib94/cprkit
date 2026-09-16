package com.cyberpunk.gmtool.viewmodel

import com.cyberpunk.gmtool.data.gtr


import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyberpunk.gmtool.data.Character
import com.cyberpunk.gmtool.data.CharacterRepository
import com.cyberpunk.gmtool.data.GameSaveManager
import com.cyberpunk.gmtool.data.GameSaveSlot
import com.cyberpunk.gmtool.data.InventoryItem
import com.cyberpunk.gmtool.data.LifepathData
import com.cyberpunk.gmtool.data.SettingsRepository
import com.cyberpunk.gmtool.data.SkillData
import com.cyberpunk.gmtool.data.StreetratData
import com.cyberpunk.gmtool.data.Stats
import com.cyberpunk.gmtool.data.safeInt
import com.cyberpunk.gmtool.data.safeLong
import com.cyberpunk.gmtool.data.safeString
import com.cyberpunk.gmtool.data.safeStringSet
import com.cyberpunk.gmtool.data.Weapon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong



data class CombatInitiativeEntry(
    val characterId: Int,
    val total: Int,
    val rollText: String,
    val tieBreak: Int = 0
)

data class CombatUiRuntime(
    val targetId: Int? = null,
    val distanceText: String = "10",
    val targetDodges: Boolean = true,
    val aimTarget: String = "None",
    val heldActionSummary: String? = null,
    val sandeActiveUntil: Long = 0L,
    val sandeCooldownUntil: Long = 0L,
    val attackActionUsed: Int = 0,
    val attackActionCap: Int? = null,
    val spotWeaknessUsed: Boolean = false,
    val damageDeflectionUsed: Set<Int> = emptySet(),
    val coverHp: Int = 0,
    val coverMaxHp: Int = 0,
    val coverLabel: String = "No Cover",
    val coverDamageExpr: String = "5d6",
    val selectedVehicleId: String? = null,
    val maneuverDv: Int = 13,
    val ramTargetKey: String? = null,
    val ramNosBoosted: Boolean = false,
    val selectedAttackWeaponId: Int? = null,
    val selectedAttackMode: String = "Single"
)

private data class CharacterSaveRequest(val revision: Long, val snapshot: List<Character>)

class CharacterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CharacterRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)
    private val gameSaveManager = GameSaveManager(application.applicationContext)
    internal val combatPrefs = application.applicationContext.getSharedPreferences("cpr_active_combat", android.content.Context.MODE_PRIVATE)
    val dataGeneration = com.cyberpunk.gmtool.data.PersistenceRuntime.reloadGeneration
    private val characterWriteMutex = Mutex()
    private val characterRevision = AtomicLong(0L)

    /**
     * سشن محلی (LAN).
     * تا وقتی GM سشنی نساخته یا بازیکنی جوین نشده، هیچ سربار شبکه‌ای ندارد
     * و رفتار برنامه دقیقاً مثل قبل و کاملاً آفلاین است.
     */
    val lan = com.cyberpunk.gmtool.data.net.LanManager(application.applicationContext).also { m ->
        m.characterProvider = { id -> _characters.value.firstOrNull { it.id == id } }
        // برگه‌ای که از شبکه رسید (GM → بازیکن، یا تأیید درخواست).
        // id محلیِ ردیفِ به‌روز‌شده را برمی‌گردانیم تا LanManager نگاشت
        // شبکه→محلی را درست نگه دارد.
        m.onCharacterFromNetwork = { netId, incoming -> applyNetworkCharacter(netId, incoming) }
        // بازیکن جدیدی که GM تأییدش کرد
        m.onCharacterAdded = { incoming ->
            val dupe = incoming.sheetUid.isNotBlank() &&
                _characters.value.any { it.sheetUid == incoming.sheetUid }
            if (dupe) {
                // تأییدِ مجدد (مثلاً بعد از reconnect) — همان ردیف را به‌روز کن
                applyNetworkCharacter(incoming.id, incoming)
            } else {
                addCharacter(incoming.copy(id = 0))
            }
        }
    }

    /**
     * برگه‌ی رسیده از شبکه را بدون پخش دوباره روی **ردیف درست** اعمال می‌کند.
     *
     * نسخه‌ی قبلی فقط به `incoming.id` نگاه می‌کرد؛ اما idها محلی‌اند: ردیف ۷
     * دیتابیس GM روی گوشی بازیکن ردیف ۲ است (یا اصلاً وجود ندارد). نتیجه یا
     * روی‌نویسِ یک ردیف بی‌ربط بود یا ساختن شخصیت تکراری — همان باگِ
     * «هر اسکن = یک کپی جدید».
     *
     * ترتیب حل هویت (اولین برنده است):
     *  1. sheetUid — هویت یکتای جهانیِ خودِ برگه.
     *  2. نگاشت شبکه→محلی که LanManager از pushهای قبلیِ این اتصال به یاد دارد.
     *  3. برگه‌ی محلی‌ای که بازیکن موقع join انتخاب کرده بود.
     *  4. تطبیق دقیق name+handle (برای سیوهای قدیمیِ بدون uid).
     *  5. هیچ‌کدام نبود → برگه واقعاً تازه است → اضافه می‌شود.
     *
     * id محلی **هرگز** با id شبکه عوض نمی‌شود؛ uid برنده پذیرفته می‌شود تا
     * هر دو گوشی از این به بعد بر سر هویت برگه توافق داشته باشند.
     *
     * @return id محلیِ ردیفی که به‌روز (یا ساخته) شد.
     */
    private fun applyNetworkCharacter(netId: Int, incoming: Character): Int {
        val uid = incoming.sheetUid
        val byUid = if (uid.isNotBlank()) {
            _characters.value.firstOrNull { it.sheetUid == uid }
        } else null
        val byNetMap = lan.localIdForNetworkId(netId)
            ?.let { id -> _characters.value.firstOrNull { it.id == id } }
        val byAssoc = lan.associatedLocalCharacterId
            ?.let { id -> _characters.value.firstOrNull { it.id == id } }
        val byName = if (incoming.name.isNotBlank() || incoming.handle.isNotBlank()) {
            _characters.value.firstOrNull {
                it.isAlly &&
                    it.name.trim().equals(incoming.name.trim(), ignoreCase = true) &&
                    it.handle.trim().equals(incoming.handle.trim(), ignoreCase = true)
            }
        } else null
        val target = byUid ?: byNetMap ?: byAssoc ?: byName

        // پرچم را بالا می‌بریم تا این اعمال، خودش دوباره پخش/درخواست نشود.
        applyingFromNetwork = true
        return try {
            if (target != null) {
                val merged = incoming.copy(
                    id = target.id,
                    sheetUid = uid.ifBlank { target.sheetUid }
                )
                persist(_characters.value.map { if (it.id == target.id) merged else it })
                target.id
            } else {
                addCharacter(incoming.copy(id = 0)).id
            }
        } finally {
            applyingFromNetwork = false
        }
    }

    /**
     * تغییر شخصیت وقتی این دستگاه «بازیکنِ متصل» است.
     * بازیکن هرگز مستقیم تغییر نمی‌دهد؛ درخواست به GM می‌رود و منتظر تأیید می‌ماند.
     * اگر سشنی فعال نباشد، تغییر مثل قبل مستقیم اعمال می‌شود.
     *
     * برمی‌گرداند: true یعنی درخواست فرستاده شد (تغییر هنوز اعمال نشده).
     */
    fun requestOrApply(
        id: Int,
        kind: String,
        summary: String,
        transform: (Character) -> Character
    ): Boolean {
        val current = _characters.value.firstOrNull { it.id == id } ?: return false
        val proposed = transform(current)
        return if (lan.isPlayerConnected() && lan.myCharacterId.value == id) {
            viewModelScope.launch { lan.requestChange(kind, summary, proposed) }
            true
        } else {
            persist(_characters.value.map { if (it.id == id) proposed else it })
            false
        }
    }


    // ==========================================
    // --- تنظیمات برنامه ---
    // ==========================================
    var autoDice by mutableStateOf(settingsRepository.autoDice)
        internal set
    var hapticsEnabled by mutableStateOf(settingsRepository.hapticsEnabled)
        internal set
    var saveError by mutableStateOf<String?>(null)
        internal set

    // ---------- وضعیت مبارزه‌ی فعال (مشترک بین شیت Player و NPC) ----------
    var combatParticipantIds by mutableStateOf<Set<Int>>(
        combatPrefs.safeStringSet("participants", emptySet(), application.applicationContext).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    )
        internal set
    var combatInitiative by mutableStateOf<List<CombatInitiativeEntry>>(
        runCatching {
            val arr = org.json.JSONArray(combatPrefs.safeString("initiative", "[]", application.applicationContext) ?: "[]")
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                CombatInitiativeEntry(o.getInt("id"), o.getInt("total"), o.getString("roll"), o.optInt("tie", 0))
            }
        }.getOrDefault(emptyList())
    )
        internal set
    var combatRound by mutableStateOf(combatPrefs.safeInt("round", 1, application.applicationContext).coerceAtLeast(1))
        internal set
    var combatTurnIndex by mutableStateOf(combatPrefs.safeInt("turn", 0, application.applicationContext).coerceAtLeast(0))
        internal set
    var combatLinkedCampaignId by mutableStateOf(combatPrefs.safeLong("linked_campaign", -1L, application.applicationContext).takeIf { it >= 0L })
        internal set
    var combatLinkedEncounterId by mutableStateOf(combatPrefs.safeLong("linked_encounter", -1L, application.applicationContext).takeIf { it >= 0L })
        internal set
    var combatLinkedSessionId by mutableStateOf(combatPrefs.safeLong("linked_session", -1L, application.applicationContext).takeIf { it >= 0L })
        internal set

    // ───────── مدیریت نشست مبارزه ─────────
    // توابع initiative/turn/combat-runtime به CharacterViewModelCombat.kt
    // منتقل شدند (توابع توسعه روی همین کلاس).


    fun updateAutoDice(value: Boolean) {
        autoDice = value
        settingsRepository.autoDice = value
    }

    fun updateHaptics(value: Boolean) {
        hapticsEnabled = value
        settingsRepository.hapticsEnabled = value
    }

    suspend fun createGameSave(label: String): Result<GameSaveSlot> = runCatching {
        characterWriteMutex.withLock { repository.saveCharacters(_characters.value) }
        persistCombatState()
        withContext(Dispatchers.IO) { gameSaveManager.createManualSave(label) }
    }

    fun listGameSaves(): List<GameSaveSlot> = gameSaveManager.listSaves()

    fun deleteGameSave(slot: GameSaveSlot): Boolean = gameSaveManager.delete(slot)

    suspend fun restoreGameSave(slot: GameSaveSlot): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { gameSaveManager.restore(slot).getOrThrow() }
        reloadAfterGameRestore()
    }

    suspend fun restoreGameSaveJson(json: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { gameSaveManager.restoreJson(json).getOrThrow() }
        reloadAfterGameRestore()
    }

    fun importGameSaveAsSlot(json: String): Result<GameSaveSlot> = gameSaveManager.importAsSlot(json)

    /** Idempotent reward write: the transaction marker is saved with the character payload itself. */
    private suspend fun applyCharacterTransactions(
        operations: List<Triple<String, Int, (Character) -> Character>>
    ): List<Character> = characterWriteMutex.withLock {
        var next = _characters.value
        operations.forEach { (txId, characterId, transform) ->
            next = next.map { c ->
                if (c.id != characterId || txId in c.appliedTransactionIds) c
                else transform(c).copy(appliedTransactionIds = (c.appliedTransactionIds + txId).distinct())
            }
        }
        next = next.map(::ensureInventoryInstanceIds).map(com.cyberpunk.gmtool.data.GameRules::syncDerived)
        val previous = _characters.value
        val rev = characterRevision.incrementAndGet()
        _characters.value = next
        repository.saveCharacters(next)
        // The revision invalidates any queued pre-transaction snapshot.
        saveQueue.trySend(CharacterSaveRequest(rev, next))
        // پاداش سشن/Encounter و غنیمت از این مسیر می‌آیند و persist() را دور می‌زنند،
        // پس همان قاعده‌ی پخش را اینجا هم اجرا می‌کنیم وگرنه گوشی بازیکن بی‌خبر می‌ماند.
        broadcastIfHosting(previous, next)
        next
    }

    suspend fun awardSessionReward(session: com.cyberpunk.gmtool.data.GameSession, characterId: Int): Result<com.cyberpunk.gmtool.data.GameSession> = runCatching {
        val reward = session.players.firstOrNull { it.characterId == characterId } ?: error("Player reward not found")
        require(reward.ip > 0 || reward.eb > 0 || reward.rep > 0) { "Reward is empty" }
        val nextBatch = reward.awardCount + 1
        val tx = "session_reward_${session.id}_${characterId}_$nextBatch"
        applyCharacterTransactions(listOf(Triple(tx, characterId) { c: Character ->
            c.copy(
                improvementPoints = (c.improvementPoints + reward.ip).coerceAtLeast(0),
                eurodollars = (c.eurodollars + reward.eb).coerceAtLeast(0),
                reputation = (c.reputation + reward.rep).coerceIn(0, 10)
            )
        }))
        session.copy(players = session.players.map {
            if (it.characterId != characterId) it else it.copy(
                ip = 0, eb = 0, rep = 0, awarded = false,
                awardCount = nextBatch,
                awardedIpTotal = it.awardedIpTotal + reward.ip,
                awardedEbTotal = it.awardedEbTotal + reward.eb,
                awardedRepTotal = it.awardedRepTotal + reward.rep
            )
        })
    }

    suspend fun awardAllSessionRewards(session: com.cyberpunk.gmtool.data.GameSession): Result<com.cyberpunk.gmtool.data.GameSession> = runCatching {
        val payable = session.players.filter { (it.ip > 0 || it.eb > 0 || it.rep > 0) && getCharacter(it.characterId) != null }
        val ops = payable.map { r ->
            val nextBatch = r.awardCount + 1
            Triple("session_reward_${session.id}_${r.characterId}_$nextBatch", r.characterId) { c: Character ->
                c.copy(
                    improvementPoints = (c.improvementPoints + r.ip).coerceAtLeast(0),
                    eurodollars = (c.eurodollars + r.eb).coerceAtLeast(0),
                    reputation = (c.reputation + r.rep).coerceIn(0, 10)
                )
            }
        }
        applyCharacterTransactions(ops)
        val paidIds = payable.map { it.characterId }.toSet()
        session.copy(players = session.players.map { r ->
            if (r.characterId !in paidIds) r else r.copy(
                ip = 0, eb = 0, rep = 0, awarded = false,
                awardCount = r.awardCount + 1,
                awardedIpTotal = r.awardedIpTotal + r.ip,
                awardedEbTotal = r.awardedEbTotal + r.eb,
                awardedRepTotal = r.awardedRepTotal + r.rep
            )
        })
    }

    suspend fun awardEncounterRewards(campaignId: Long, encounter: com.cyberpunk.gmtool.data.CampaignEncounter): Result<com.cyberpunk.gmtool.data.CampaignEncounter> = runCatching {
        val eligibleIds = encounter.fighterIds.filter { id -> getCharacter(id)?.isAlly == true }
        val ops = eligibleIds.map { id ->
            Triple("encounter_reward_${campaignId}_${encounter.id}_$id", id) { c: Character ->
                c.copy(
                    improvementPoints = (c.improvementPoints + encounter.rewardIp).coerceAtLeast(0),
                    eurodollars = (c.eurodollars + encounter.rewardEb).coerceAtLeast(0),
                    reputation = (c.reputation + encounter.rewardRep).coerceIn(0, 10)
                )
            }
        }
        applyCharacterTransactions(ops)
        encounter.copy(rewardsAwarded = true)
    }

    suspend fun claimEncounterLoot(
        campaignId: Long,
        encounter: com.cyberpunk.gmtool.data.CampaignEncounter,
        lootId: String,
        characterId: Int
    ): Result<com.cyberpunk.gmtool.data.CampaignEncounter> = runCatching {
        val loot = encounter.loot.firstOrNull { it.id == lootId } ?: error("Loot not found")
        require(loot.claimedBy == null || loot.claimedBy == characterId) { "Loot قبلاً گرفته شده است." }
        val tx = "encounter_loot_${campaignId}_${encounter.id}_${loot.id}_$characterId"
        applyCharacterTransactions(listOf(Triple(tx, characterId) { c: Character ->
            c.copy(inventory = c.inventory + List(loot.quantity.coerceAtLeast(1)) {
                InventoryItem(loot.name, loot.category, instanceId = "inv_${java.util.UUID.randomUUID()}")
            })
        }))
        encounter.copy(loot = encounter.loot.map { if (it.id == loot.id) it.copy(claimedBy = characterId) else it })
    }

    suspend fun finishLinkedEncounter(outcome: String = "Combat resolved", consequence: String = ""): Result<Unit> = runCatching {
        val campaignId = combatLinkedCampaignId ?: error("این Combat به Campaign Encounter وصل نیست.")
        val encounterId = combatLinkedEncounterId ?: error("Encounter link پیدا نشد.")
        val sessionId = combatLinkedSessionId
        val epoch = com.cyberpunk.gmtool.data.PersistenceRuntime.currentEpoch()
        withContext(Dispatchers.IO) {
            com.cyberpunk.gmtool.data.PersistenceRuntime.locked {
                com.cyberpunk.gmtool.data.CampaignStore(getApplication<Application>().applicationContext)
                    .resolveEncounter(campaignId, encounterId, outcome, consequence, epoch)
                val stamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
                val survivors = combatParticipantIds.mapNotNull { getCharacter(it) }.count { !it.isDead }
                com.cyberpunk.gmtool.data.SessionStore(getApplication<Application>().applicationContext).appendLog(
                    sessionId,
                    com.cyberpunk.gmtool.data.SessionLogEntry(stamp, gtr("Combat resolved • Round %1s • %2s survivor(s) • %3s", combatRound, survivors, outcome)),
                    epoch
                )
            }
        }
        clearCombatEncounter()
        com.cyberpunk.gmtool.data.PersistenceRuntime.publishExternalDataChange()
    }

    private suspend fun reloadAfterGameRestore() {
        val restored = repository.loadCharacters().map(::ensureInventoryInstanceIds).map(com.cyberpunk.gmtool.data.GameRules::syncDerived)
        val previous = _characters.value
        val rev = characterRevision.incrementAndGet()
        _characters.value = restored
        saveQueue.trySend(CharacterSaveRequest(rev, restored))
        // ری‌استور وسط سشن هم باید به بازیکن‌ها برسد، وگرنه برگه‌هایشان با GM فرق می‌کند.
        broadcastIfHosting(previous, restored)
        reloadCombatStateFromPrefs()
        val validIds = restored.map { it.id }.toSet()
        combatParticipantIds = combatParticipantIds.filter { it in validIds }.toSet()
        combatInitiative = combatInitiative.filter { it.characterId in validIds }
        combatTurnIndex = combatTurnIndex.coerceIn(0, (combatInitiative.size - 1).coerceAtLeast(0))
        autoDice = settingsRepository.autoDice
        hapticsEnabled = settingsRepository.hapticsEnabled
        persistCombatState()
    }

    /** پاک کردن کامل همه‌ی کاراکترها */
    fun wipeAllCharacters() {
        persist(emptyList())
    }

    internal val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()
    private val saveQueue = Channel<CharacterSaveRequest>(Channel.CONFLATED)

    internal fun ensureInventoryInstanceIds(character: Character): Character {
        var changed = false
        val inv = character.inventory.map { item ->
            var next = item
            if (next.instanceId.isBlank()) {
                changed = true
                next = next.copy(instanceId = "inv_${java.util.UUID.randomUUID()}")
            }
            // Legacy saves had no install ledger. An already-equipped piece has necessarily been installed before.
            if (next.category.equals("Cyberware", true) && next.equipped && next.cyberwareInstallCount == 0) {
                changed = true
                next = next.copy(
                    cyberwareInstallCount = 1,
                    humanityLossPaid = com.cyberpunk.gmtool.data.CyberwareCatalog.humanityLossFor(next.name)
                )
            }
            // گزارش تست ۵.۱: سیوهای قدیمی Cyberdeck را (به‌خلافِ نبودِ شاخه‌ی Gear در
            // EquipmentParser) با دسته‌ی Utilities ذخیره کرده‌اند. NET Architecture فقط
            // Gear را می‌بیند، پس همان‌جا که سیو load می‌شود دسته اصلاح می‌شود.
            if (next.name.contains("Cyberdeck", true) && next.category.equals("Utilities", true)) {
                changed = true
                next = next.copy(category = "Gear")
            }
            next
        }
        return if (changed) character.copy(inventory = inv) else character
    }

    init {
        viewModelScope.launch {
            _characters.value = repository.loadCharacters().map(::ensureInventoryInstanceIds).map(com.cyberpunk.gmtool.data.GameRules::syncDerived)
            val validIds = _characters.value.map { it.id }.toSet()
            combatParticipantIds = combatParticipantIds.filter { it in validIds }.toSet()
            combatInitiative = combatInitiative.filter { it.characterId in validIds }
            combatTurnIndex = combatTurnIndex.coerceIn(0, (combatInitiative.size - 1).coerceAtLeast(0))
            persistCombatState()
            repository.lastLoadWarning?.let { saveError = it }
        }
        // Single revision-aware writer. A snapshot created before a restore/transaction can never win later.
        viewModelScope.launch {
            for (request in saveQueue) {
                try {
                    characterWriteMutex.withLock {
                        if (request.revision == characterRevision.get()) repository.saveCharacters(request.snapshot)
                    }
                    saveError = null
                } catch (t: Throwable) {
                    saveError = t.message ?: "ذخیره‌سازی ناموفق بود"
                }
            }
        }
        // Recovery snapshots are intentionally frequent because active Combat runtime is now persisted too.
        viewModelScope.launch {
            delay(15_000L)
            while (isActive) {
                runCatching {
                    characterWriteMutex.withLock { repository.saveCharacters(_characters.value) }
                    withContext(Dispatchers.IO) { gameSaveManager.maybeCreateAutosave(60_000L) }
                }
                delay(60_000L)
            }
        }
    }

    // ---------- ذخیره خودکار بعد از هر تغییر ----------
    /**
     * وقتی true باشد، [persist] هیچ درخواستی به GM نمی‌فرستد و هیچ‌چیز پخش نمی‌کند.
     * برای وقتی است که خودِ شبکه دارد برگه را اعمال می‌کند؛ بدون این، برگه‌ی
     * رسیده دوباره پخش می‌شد و حلقه‌ی بی‌پایان می‌ساخت.
     */
    private var applyingFromNetwork = false

    internal fun persist(updated: List<Character>) {
        val normalized = updated.map(::ensureInventoryInstanceIds).map(com.cyberpunk.gmtool.data.GameRules::syncDerived)
        val previous = _characters.value

        // ── دروازه‌ی تأیید GM ──
        // این تنها قیف تغییر شخصیت است، پس همین‌جا جلوی همه‌ی تغییرات بازیکن
        // گرفته می‌شود: خرید، پول، استت، ویرایش دستی — بدون استثنا.
        // قبلاً فقط دو نقطه (پول و افزودن آیتم) بررسی می‌شدند و بقیه رد می‌شدند.
        if (!applyingFromNetwork && lan.isPlayerConnected()) {
            val myId = lan.myCharacterId.value
            val prevById = previous.associateBy { it.id }
            val changed = normalized.filter { prevById[it.id] != it }
            val mine = changed.filter { it.id == myId }
            if (mine.isNotEmpty()) {
                // تغییر روی برگه‌ی خودِ بازیکن: فقط درخواست بفرست، اعمال نکن.
                mine.forEach { proposed ->
                    val before = prevById[proposed.id]
                    viewModelScope.launch {
                        lan.requestChange(
                            com.cyberpunk.gmtool.data.net.ChangeRequest.STAT,
                            describeChange(before, proposed),
                            proposed
                        )
                    }
                }
                // تغییرات مربوط به شخصیت‌های دیگر (اگر بود) عادی ذخیره می‌شوند.
                val others = normalized.mapIndexed { i, c ->
                    if (c.id == myId) previous.firstOrNull { it.id == myId } ?: c else c
                }
                val rev = characterRevision.incrementAndGet()
                _characters.value = others
                saveQueue.trySend(CharacterSaveRequest(rev, others))
                return
            }
        }

        val rev = characterRevision.incrementAndGet()
        _characters.value = normalized
        saveQueue.trySend(CharacterSaveRequest(rev, normalized))

        // سشن محلی: فقط شخصیت‌هایی که واقعاً تغییر کرده‌اند به گوشی بازیکن می‌روند.
        // اگر سشنی فعال نباشد، این خط هیچ کاری نمی‌کند.
        broadcastIfHosting(previous, normalized)
    }

    /**
     * تنها جای پخش تغییرات به بازیکن‌ها.
     * هر مسیری که برگه را عوض می‌کند باید این را صدا بزند — چه از persist() بیاید
     * چه از تراکنش‌های پاداش. اگر جای دیگری مستقیم _characters.value را بنویسد
     * و این را صدا نزند، همان باگِ «به گوشی بازیکن نمی‌رسد» برمی‌گردد.
     */
    private fun broadcastIfHosting(previous: List<Character>, next: List<Character>) {
        if (applyingFromNetwork) return
        if (lan.role.value != com.cyberpunk.gmtool.data.net.LanRole.GM) return
        val prevById = previous.associateBy { it.id }
        val changed = next.filter { prevById[it.id] != it }
        if (changed.isNotEmpty()) lan.broadcastCharacters(changed)
    }

    /** خلاصه‌ی خوانا از تفاوت دو برگه، برای صف تأیید GM. */
    private fun describeChange(before: Character?, after: Character): String {
        if (before == null) return "برگه‌ی ${after.name}"
        val parts = mutableListOf<String>()
        if (before.eurodollars != after.eurodollars) {
            val d = after.eurodollars - before.eurodollars
            parts += if (d < 0) "خرج ${-d} اِدی" else "دریافت $d اِدی"
        }
        if (before.inventory.size != after.inventory.size) {
            val d = after.inventory.size - before.inventory.size
            parts += if (d > 0) "افزودن $d آیتم" else "حذف ${-d} آیتم"
        }
        if (before.weapons.size != after.weapons.size) {
            val d = after.weapons.size - before.weapons.size
            parts += if (d > 0) "افزودن $d سلاح" else "حذف ${-d} سلاح"
        }
        if (before.hp != after.hp) parts += "HP ${before.hp} ← ${after.hp}"
        if (before.improvementPoints != after.improvementPoints) {
            parts += "IP ${before.improvementPoints} ← ${after.improvementPoints}"
        }
        if (before.stats != after.stats) parts += "تغییر STAT"
        if (before.skills != after.skills) parts += "تغییر مهارت"
        if (before.currentHumanity != after.currentHumanity) {
            parts += "انسانیت ${before.currentHumanity} ← ${after.currentHumanity}"
        }
        if (parts.isEmpty()) parts += "ویرایش برگه"
        return parts.joinToString(" • ")
    }

    internal fun updateCharacter(id: Int, transform: (Character) -> Character) {
        persist(_characters.value.map { if (it.id == id) transform(it) else it })
    }

    fun getCharacter(id: Int): Character? = _characters.value.firstOrNull { it.id == id }

    // ---------- ویرایش سریع NPC (شیت ساده‌شده) ----------
    fun updateNpc(id: Int, transform: (Character) -> Character) = updateCharacter(id, transform)

    // ---------- ساخت کاراکتر جدید ----------

    /**
     * آیا name+handle از قبل توسط شخصیتی در فهرست استفاده شده؟
     * اعتبارسنجی زمان ساخت (گزارش تست ۴.۲): معیار یکتایی فقط HANDLE است.
     * نام می‌تواند تکراری باشد (دو «Johnny» در فهرست اشکالی ندارد)، اما
     * Handle کلید هویت شبیه‌سازی‌شده‌ی برگه در LAN است و تکراری بودنش
     * جفت‌سازی شبکه را خراب می‌کند و کلیدواژه‌ی باگ کپی‌شدن است.
     */
    fun handleTaken(handle: String, excludeId: Int? = null): Boolean {
        val h = handle.trim()
        if (h.isBlank()) return false
        return _characters.value.any {
            it.isAlly && it.id != excludeId &&
                it.handle.trim().equals(h, ignoreCase = true)
        }
    }

    fun addCharacter(character: Character): Character {
        val newId = (_characters.value.maxOfOrNull { it.id } ?: 0) + 1
        val saved = character.copy(id = newId)
        persist(_characters.value + saved)
        return saved
    }

    fun deleteCharacter(id: Int) {
        persist(_characters.value.filterNot { it.id == id })
    }


    // ---------- NPCها (isAlly = false) ----------
    val npcs: List<Character> get() = _characters.value.filter { !it.isAlly }
    val players: List<Character> get() = _characters.value.filter { it.isAlly }

    fun addNpc(npc: Character): Character = addCharacter(npc.copy(isAlly = false))

    /** افزودن دسته‌ای (مثلاً Bulk) با id یکتا */
    fun addMany(npcs: List<Character>): List<Character> {
        if (npcs.isEmpty()) return emptyList()
        var nextId = (_characters.value.maxOfOrNull { it.id } ?: 0) + 1
        val saved = npcs.map { it.copy(id = nextId++, isAlly = false) }
        persist(_characters.value + saved)
        return saved
    }

    /** ایمپورت لیست کاراکتر/NPC از فایل JSON؛ id جدید می‌گیرد تا تداخل نشود. */
    fun importCharacters(imported: List<Character>): Int {
        if (imported.isEmpty()) return 0
        var nextId = (_characters.value.maxOfOrNull { it.id } ?: 0) + 1
        val reId = imported.map { raw ->
            val cleaned = com.cyberpunk.gmtool.data.GameRules.syncDerived(raw).copy(
                id = nextId++,
                stats = raw.stats.copy(
                    int = raw.stats.int.coerceIn(1, 15), ref = raw.stats.ref.coerceIn(1, 15),
                    dex = raw.stats.dex.coerceIn(1, 15), tech = raw.stats.tech.coerceIn(1, 15),
                    cool = raw.stats.cool.coerceIn(1, 15), will = raw.stats.will.coerceIn(1, 15),
                    luck = raw.stats.luck.coerceIn(1, 15), move = raw.stats.move.coerceIn(0, 15),
                    body = raw.stats.body.coerceIn(1, 15), emp = raw.stats.emp.coerceIn(0, 10)
                ),
                skills = raw.skills.distinctBy { it.name.lowercase() }.map { it.copy(level = it.level.coerceIn(0, 10)) },
                weapons = raw.weapons.map { w ->
                    val newWeaponId = (java.util.UUID.randomUUID().hashCode() and Int.MAX_VALUE).coerceAtLeast(1)
                    w.copy(id = newWeaponId, currentAmmo = w.currentAmmo.coerceIn(0, w.magazineSize.coerceAtLeast(0)))
                }
            )
            com.cyberpunk.gmtool.data.GameRules.syncDerived(cleaned)
        }
        persist(_characters.value + reId)
        return reId.size
    }

    // ---------- لایف‌پث ----------
    fun updateLifepath(id: Int, lifepath: LifepathData) =
        updateCharacter(id) { it.copy(lifepath = lifepath) }

    // ---------- مهارت‌ها ----------
    fun setSkillLevel(id: Int, skillName: String, level: Int) =
        updateCharacter(id) { c ->
            val skills = c.skills.map { if (it.name == skillName) it.copy(level = level.coerceIn(0, 10)) else it }
            c.copy(skills = skills)
        }

    /** تنظیم سطح مهارت بر اساس نام؛ اگر مهارت در لیست نبود (سطح صفر)، آن را می‌سازد. برای صفحه‌ی ادیت استت. */
    fun setSkillLevelByName(id: Int, skillName: String, level: Int) =
        updateCharacter(id) { c ->
            val target = level.coerceIn(0, 10)
            val skills = c.skills.toMutableList()
            val idx = skills.indexOfFirst { it.name == skillName }
            if (idx >= 0) skills[idx] = skills[idx].copy(level = target)
            else if (target > 0) skills.add(
                SkillData(name = skillName, stat = com.cyberpunk.gmtool.data.SkillCatalog.statFor(skillName), level = target)
            )
            c.copy(skills = skills)
        }

    // ---------- استات‌های پایه ----------
    /** تغییر یک استات پایه با کلید INT/REF/DEX/TECH/COOL/WILL/MOVE/BODY/EMP. سقف/کف ۲ تا ۱۰. */
    fun adjustStat(id: Int, statKey: String, delta: Int) =
        updateCharacter(id) { c ->
            val s = c.stats
            val clamp: (Int) -> Int = { v -> (v + delta).coerceIn(2, 10) }
            val newStats = when (statKey) {
                "INT" -> s.copy(int = clamp(s.int))
                "REF" -> s.copy(ref = clamp(s.ref))
                "DEX" -> s.copy(dex = clamp(s.dex))
                "TECH" -> s.copy(tech = clamp(s.tech))
                "COOL" -> s.copy(cool = clamp(s.cool))
                "WILL" -> s.copy(will = clamp(s.will))
                "LUCK" -> s.copy(luck = clamp(s.luck))
                "MOVE" -> s.copy(move = clamp(s.move))
                "BODY" -> s.copy(body = clamp(s.body))
                "EMP" -> s.copy(emp = clamp(s.emp))
                else -> s
            }
            // تغییر EMP سقف Humanity را هم به‌روز می‌کند
            val cyberMaxReduction = c.inventory.filter { it.category.equals("Cyberware", true) && it.equipped }.sumOf { com.cyberpunk.gmtool.data.CyberwareCatalog.maxReductionFor(it.name) }
            val newMaxHum = (newStats.emp * 10 - cyberMaxReduction).coerceAtLeast(0)
            val newMaxHp = com.cyberpunk.gmtool.data.GameRules.maxHp(newStats.body, newStats.will)
            c.copy(stats = newStats,
                baseEmp = if (statKey == "EMP") newStats.emp else c.baseEmp,
                baseBody = if (statKey == "BODY") newStats.body else c.baseBody,
                maxHumanity = newMaxHum, currentHumanity = c.currentHumanity.coerceAtMost(newMaxHum),
                maxHp = newMaxHp, hp = c.hp.coerceAtMost(newMaxHp), maxLuck = newStats.luck, currentLuck = c.currentLuck.coerceAtMost(newStats.luck))
        }

    /** تنظیم مستقیم یک استات پایه (برای دیالوگ ادیت). */
    fun setStat(id: Int, statKey: String, value: Int) =
        updateCharacter(id) { c ->
            val v = value.coerceIn(2, 10)
            val s = c.stats
            val newStats = when (statKey) {
                "INT" -> s.copy(int = v)
                "REF" -> s.copy(ref = v)
                "DEX" -> s.copy(dex = v)
                "TECH" -> s.copy(tech = v)
                "COOL" -> s.copy(cool = v)
                "WILL" -> s.copy(will = v)
                "LUCK" -> s.copy(luck = v)
                "MOVE" -> s.copy(move = v)
                "BODY" -> s.copy(body = v)
                "EMP" -> s.copy(emp = v)
                else -> s
            }
            val cyberMaxReduction = c.inventory.filter { it.category.equals("Cyberware", true) && it.equipped }.sumOf { com.cyberpunk.gmtool.data.CyberwareCatalog.maxReductionFor(it.name) }
            val newMaxHum = (newStats.emp * 10 - cyberMaxReduction).coerceAtLeast(0)
            val newMaxHp = com.cyberpunk.gmtool.data.GameRules.maxHp(newStats.body, newStats.will)
            c.copy(stats = newStats,
                baseEmp = if (statKey == "EMP") newStats.emp else c.baseEmp,
                baseBody = if (statKey == "BODY") newStats.body else c.baseBody,
                maxHumanity = newMaxHum, currentHumanity = c.currentHumanity.coerceAtMost(newMaxHum),
                maxHp = newMaxHp, hp = c.hp.coerceAtMost(newMaxHp), maxLuck = newStats.luck, currentLuck = c.currentLuck.coerceAtMost(newStats.luck))
        }

    // ارتقای مهارت با امتیاز IP. هزینه: رتبه‌ی هدف × ۱۰ (قانون پایه‌ی IP).
    // مهارت‌های صفر (که در لیست نیستند) هم قابل ارتقایند و در صورت نبود، ساخته می‌شوند.
    fun upgradeSkillWithIP(id: Int, skillName: String): Boolean {
        val c = getCharacter(id) ?: return false
        val skill = c.skills.firstOrNull { it.name == skillName }
        val currentLevel = skill?.level ?: 0
        val cost = com.cyberpunk.gmtool.data.SkillCatalog.improvementCost(skillName, currentLevel + 1)
        if (c.improvementPoints < cost || currentLevel >= 10) return false
        updateCharacter(id) { ch ->
            val skills = ch.skills.toMutableList()
            val idx = skills.indexOfFirst { it.name == skillName }
            if (idx >= 0) skills[idx] = skills[idx].copy(level = skills[idx].level + 1)
            else skills.add(
                com.cyberpunk.gmtool.data.SkillData(
                    name = skillName,
                    stat = com.cyberpunk.gmtool.data.SkillCatalog.statFor(skillName),
                    level = 1
                )
            )
            ch.copy(improvementPoints = ch.improvementPoints - cost, skills = skills)
        }
        return true
    }

    fun skillUpgradeCost(character: Character, skillName: String): Int? {
        val level = character.skills.firstOrNull { it.name == skillName }?.level ?: 0
        if (level >= 10) return null
        return com.cyberpunk.gmtool.data.SkillCatalog.improvementCost(skillName, level + 1)
    }

    // ---------- امتیازات قابلیت نقش ----------
    fun setRoleAbilityPoint(id: Int, key: String, value: Int) =
        updateCharacter(id) { c ->
            val mutable = c.roleAbilityPoints.toMutableMap()
            mutable[key] = value.coerceAtLeast(0)
            val normalized = normalizeRoleAllocations(c.role, mutable, c.roleRank)
            val pharmaCap = if (c.role.equals("Medtech", true)) (normalized["pharma"] ?: 0).coerceIn(0, 5) else c.medtechPharmaceuticals.size
            c.copy(
                roleAbilityPoints = normalized,
                medtechPharmaceuticals = if (c.role.equals("Medtech", true)) c.medtechPharmaceuticals.distinct().take(pharmaCap) else c.medtechPharmaceuticals
            )
        }

    fun adjustRoleAbilityPoint(id: Int, key: String, delta: Int) =
        updateCharacter(id) { c ->
            val current = c.roleAbilityPoints[key] ?: 0
            val mutable = c.roleAbilityPoints.toMutableMap()
            mutable[key] = (current + delta).coerceAtLeast(0)
            c.copy(roleAbilityPoints = normalizeRoleAllocations(c.role, mutable, c.roleRank))
        }

    fun roleAbilityPoints(id: Int, key: String): Int =
        getCharacter(id)?.roleAbilityPoints?.get(key) ?: 0

    fun toggleMedtechPharmaceutical(id: Int, name: String) = updateCharacter(id) { c ->
        if (!c.role.equals("Medtech", true)) return@updateCharacter c
        val pharmaPoints = (c.roleAbilityPoints["pharma"] ?: 0).coerceIn(0, 5)
        val allowed = com.cyberpunk.gmtool.data.RoleAssistantData.corePharmaceuticals.map { it.name }
        if (name !in allowed) return@updateCharacter c
        val current = c.medtechPharmaceuticals.filter { it in allowed }.distinct().toMutableList()
        if (name in current) current.remove(name)
        else if (current.size < pharmaPoints) current.add(name)
        c.copy(medtechPharmaceuticals = current)
    }

    // ---------- بیماران Medtech ----------
    fun addMedtechPatient(id: Int, name: String = "", linkedCharacterId: Int? = null) = updateCharacter(id) { c ->
        if (!c.role.equals("Medtech", true)) return@updateCharacter c
        // اگر بیمار یک کاراکتر واقعی باشد، وضعیت و جراحت‌هایش از همان برگه کپی می‌شود
        // تا تابلوی بیماران از لحظه‌ی اول با واقعیت بخواند.
        val linked = linkedCharacterId?.let { cid -> _characters.value.firstOrNull { it.id == cid } }
        val state = if (linked != null && !com.cyberpunk.gmtool.data.GameRules.isMortallyWounded(linked.hp) && !linked.isDead) "Stabilized" else "Critical"
        val injuryText = linked?.criticalInjuries?.joinToString(" • ") { key ->
            com.cyberpunk.gmtool.data.CriticalInjuries.byKey(key)?.faName ?: key
        } ?: ""
        c.copy(medtechPatients = c.medtechPatients + com.cyberpunk.gmtool.data.MedtechPatient(
            id = System.currentTimeMillis(),
            name = name,
            state = state,
            injury = injuryText,
            linkedCharacterId = linkedCharacterId
        ))
    }

    fun updateMedtechPatient(id: Int, patientId: Long, transform: (com.cyberpunk.gmtool.data.MedtechPatient) -> com.cyberpunk.gmtool.data.MedtechPatient) =
        updateCharacter(id) { c ->
            c.copy(medtechPatients = c.medtechPatients.map { if (it.id == patientId) transform(it) else it })
        }

    fun deleteMedtechPatient(id: Int, patientId: Long) = updateCharacter(id) { c ->
        c.copy(medtechPatients = c.medtechPatients.filterNot { it.id == patientId })
    }

    fun addMediaCase(id: Int) = updateCharacter(id) { c ->
        if (!c.role.equals("Media", true)) return@updateCharacter c
        val next = (c.mediaCases.maxOfOrNull { it.id } ?: 0L) + 1L
        c.copy(mediaCases = c.mediaCases + com.cyberpunk.gmtool.data.MediaCase(id = next, title = "پرونده جدید"))
    }

    fun updateMediaCase(id: Int, caseId: Long, transform: (com.cyberpunk.gmtool.data.MediaCase) -> com.cyberpunk.gmtool.data.MediaCase) =
        updateCharacter(id) { c -> c.copy(mediaCases = c.mediaCases.map { if (it.id == caseId) transform(it) else it }) }

    fun deleteMediaCase(id: Int, caseId: Long) = updateCharacter(id) { c ->
        c.copy(mediaCases = c.mediaCases.filterNot { it.id == caseId })
    }

    /** به‌روزرسانی تابلوی رزم سولو. */
    fun updateSoloCombatState(id: Int, transform: (com.cyberpunk.gmtool.data.SoloCombatState) -> com.cyberpunk.gmtool.data.SoloCombatState) =
        updateCharacter(id) { c ->
            if (!c.role.equals("Solo", true)) return@updateCharacter c
            val current = c.soloCombatState ?: com.cyberpunk.gmtool.data.SoloCombatState()
            c.copy(soloCombatState = transform(current))
        }

    /** پایان صحنه: شمارنده‌های موقتی سولو صفر می‌شوند، نشانه‌گذاری‌ها پاک. */
    fun resetSoloScene(id: Int) = updateCharacter(id) { c ->
        if (!c.role.equals("Solo", true)) return@updateCharacter c
        c.copy(soloCombatState = com.cyberpunk.gmtool.data.SoloCombatState())
    }

    fun updateNomadFamilyProfile(id: Int, transform: (com.cyberpunk.gmtool.data.NomadFamilyProfile) -> com.cyberpunk.gmtool.data.NomadFamilyProfile) =
        updateCharacter(id) { c ->
            if (!c.role.equals("Nomad", true)) return@updateCharacter c
            val current = c.nomadFamilyProfile ?: com.cyberpunk.gmtool.data.NomadFamilyProfile()
            c.copy(nomadFamilyProfile = transform(current))
        }

    // ---------- منابع عددی ----------
    // دروازه‌ی تأیید در persist() است، پس اینجا فقط تغییر عادی انجام می‌شود.
    fun adjustEurodollars(id: Int, delta: Int) =
        updateCharacter(id) { it.copy(eurodollars = (it.eurodollars + delta).coerceAtLeast(0)) }

    fun setLifestyleAndHousing(id: Int, lifestyle: String, housing: String, monthlyRent: Int) =
        updateCharacter(id) { it.copy(lifestyle = lifestyle, housing = housing, monthlyRent = monthlyRent.coerceAtLeast(0)) }

    fun payExpense(id: Int, amount: Int): Boolean {
        val c = getCharacter(id) ?: return false
        val cost = amount.coerceAtLeast(0)
        if (c.eurodollars < cost) return false
        adjustEurodollars(id, -cost)
        return true
    }

    fun sellInventoryItem(id: Int, index: Int, salePrice: Int): Boolean {
        val c = getCharacter(id) ?: return false
        if (index !in c.inventory.indices || salePrice < 0) return false
        if (c.inventory[index].category.equals("Cyberware", true) && c.inventory[index].equipped) return false
        updateCharacter(id) { ch ->
            val removed = ch.inventory[index]
            var next = ch.copy(
                eurodollars = ch.eurodollars + salePrice,
                inventory = ch.inventory.filterIndexed { i, _ -> i != index }
            )
            if (removed.category.equals("Cyberware", true)) {
                next = next.copy(maxHumanity = com.cyberpunk.gmtool.data.GameRules.maxHumanity(next.baseEmp, next.inventory))
            }
            com.cyberpunk.gmtool.data.GameRules.syncDerived(next)
        }
        return true
    }

    fun adjustHp(id: Int, delta: Int) =
        updateCharacter(id) { c ->
            val hp = (c.hp + delta).coerceIn(0, c.maxHp)
            c.copy(hp = hp, isDead = if (hp > 0) false else c.isDead)
        }

    fun setHp(id: Int, value: Int) =
        updateCharacter(id) { c ->
            val hp = value.coerceIn(0, c.maxHp)
            c.copy(hp = hp, isDead = if (hp > 0) false else c.isDead)
        }

    fun adjustMaxHp(id: Int, delta: Int) =
        updateCharacter(id) { c ->
            val newMax = (c.maxHp + delta).coerceAtLeast(1)
            c.copy(maxHp = newMax, hp = c.hp.coerceIn(0, newMax))
        }

    fun adjustHeadArmor(id: Int, delta: Int) =
        updateCharacter(id) { it.copy(headArmorSp = (it.headArmorSp + delta).coerceAtLeast(0)) }

    fun adjustBodyArmor(id: Int, delta: Int) =
        updateCharacter(id) { it.copy(bodyArmorSp = (it.bodyArmorSp + delta).coerceAtLeast(0)) }

    fun adjustCurrentHumanity(id: Int, delta: Int) =
        updateCharacter(id) { c ->
            val hum = (c.currentHumanity + delta).coerceIn(0, c.maxHumanity)
            c.copy(currentHumanity = hum, stats = c.stats.copy(emp = minOf(c.baseEmp.coerceAtLeast(c.stats.emp), com.cyberpunk.gmtool.data.GameRules.currentEmpathy(hum))))
        }

    fun adjustMaxHumanity(id: Int, delta: Int) =
        updateCharacter(id) { c ->
            val newMax = (c.maxHumanity + delta).coerceAtLeast(1)
            c.copy(maxHumanity = newMax, currentHumanity = c.currentHumanity.coerceIn(0, newMax))
        }

    fun adjustCurrentLuck(id: Int, delta: Int) =
        updateCharacter(id) { c -> c.copy(currentLuck = (c.currentLuck + delta).coerceIn(0, c.maxLuck)) }

    fun adjustMaxLuck(id: Int, delta: Int) =
        updateCharacter(id) { c ->
            val newMax = (c.stats.luck + delta).coerceIn(0, 15)
            c.copy(stats = c.stats.copy(luck = newMax), maxLuck = newMax, currentLuck = c.currentLuck.coerceIn(0, newMax))
        }

    /** ریست شانس در شروع جلسه (برمی‌گردد به سقف) */
    fun refillLuck(id: Int) =
        updateCharacter(id) { it.copy(currentLuck = it.maxLuck) }

    fun adjustReputation(id: Int, delta: Int) =
        updateCharacter(id) { it.copy(reputation = (it.reputation + delta).coerceIn(0, 10)) }

    fun adjustImprovementPoints(id: Int, delta: Int) =
        updateCharacter(id) { it.copy(improvementPoints = (it.improvementPoints + delta).coerceAtLeast(0)) }

    fun adjustRoleRank(id: Int, delta: Int) =
        updateCharacter(id) { c ->
            val newRank = (c.roleRank + delta).coerceIn(1, 10)
            c.copy(roleRank = newRank, roleAbilityPoints = normalizeRoleAllocations(c.role, c.roleAbilityPoints, newRank))
        }

    /** ارتقای رنک نقش با IP. هزینه‌ی رنک جدید = رنک جدید × ۶۰ (صفحه‌ی Improvement). خروجی: موفقیت؟ */
    fun upgradeRoleRankWithIP(id: Int): Boolean {
        val c = getCharacter(id) ?: return false
        val next = c.roleRank + 1
        if (next > 10) return false
        val cost = next * 60
        if (c.improvementPoints < cost) return false
        updateCharacter(id) { it.copy(
            improvementPoints = it.improvementPoints - cost,
            roleRank = next.coerceIn(1, 10)
        ) }
        return true
    }

    // ───────── موجودی/سلاح/فروشگاه/نصب تجهیزات ─────────
    // به CharacterViewModelGear.kt منتقل شد (توابع توسعه روی همین کلاس).


    // ───────── Street Drugs / تراپی ─────────
    // به CharacterViewModelCare.kt منتقل شد (توابع توسعه روی همین کلاس).

    // ───────── Cyberware / Equip toggle ─────────
    // به CharacterViewModelGear.kt منتقل شد (توابع توسعه روی همین کلاس).


    // ---------- ویرایش نام و لقب ----------
    fun setName(id: Int, name: String) = updateCharacter(id) { it.copy(name = name.ifBlank { it.name }) }
    fun setHandle(id: Int, handle: String) = updateCharacter(id) { it.copy(handle = handle) }

    // ---------- لایف‌استایل و مسکن ----------
    fun setLifestyle(id: Int, key: String) = updateCharacter(id) { c ->
        c.copy(lifestyle = key)
    }
    fun setHousing(id: Int, key: String) = updateCharacter(id) { c ->
        c.copy(housing = key, monthlyRent = com.cyberpunk.gmtool.data.LifeData.rentFor(key))
    }

    // ---------- گوشی (Agent): نوت / آبجکتیو / مخاطبین ----------
    internal fun newId(): Long = java.util.UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE

    fun addNote(id: Int, title: String, body: String) = updateCharacter(id) { c ->
        c.copy(phoneNotes = c.phoneNotes + com.cyberpunk.gmtool.data.PhoneNote(newId(), title, body))
    }
    fun deleteNote(id: Int, noteId: Long) = updateCharacter(id) { c ->
        c.copy(phoneNotes = c.phoneNotes.filterNot { it.id == noteId })
    }

    fun addObjective(id: Int, title: String, type: String, body: String) = updateCharacter(id) { c ->
        c.copy(objectives = c.objectives + com.cyberpunk.gmtool.data.Objective(newId(), title, type, body))
    }
    fun toggleObjective(id: Int, objId: Long) = updateCharacter(id) { c ->
        c.copy(objectives = c.objectives.map { if (it.id == objId) it.copy(done = !it.done) else it })
    }
    fun deleteObjective(id: Int, objId: Long) = updateCharacter(id) { c ->
        c.copy(objectives = c.objectives.filterNot { it.id == objId })
    }

    fun addContact(id: Int, name: String, detail: String) = updateCharacter(id) { c ->
        c.copy(contacts = c.contacts + com.cyberpunk.gmtool.data.Contact(newId(), name, detail))
    }
    fun deleteContact(id: Int, contactId: Long) = updateCharacter(id) { c ->
        c.copy(contacts = c.contacts.filterNot { it.id == contactId })
    }

    /** A grenade is both throwable ammunition and launcher ammunition in Core. Load one grenade weapon into a launcher. */
    fun loadGrenadeWeaponIntoLauncher(id: Int, launcherId: Int, grenadeWeaponId: Int) = updateCharacter(id) { c ->
        val launcher = c.weapons.firstOrNull { it.id == launcherId } ?: return@updateCharacter c
        val grenade = c.weapons.firstOrNull { it.id == grenadeWeaponId } ?: return@updateCharacter c
        if (!launcher.ammoType.contains("Grenade", true) || launcher.currentAmmo >= launcher.magazineSize || !grenade.name.contains("Grenade", true)) return@updateCharacter c
        c.copy(
            weapons = c.weapons.filterNot { it.id == grenadeWeaponId }.map { if (it.id == launcherId) it.copy(currentAmmo = it.currentAmmo + 1, loadedAmmoName = grenade.name) else it }
        )
    }

    // ───────── رهگیری مبارزه (سپر/حرکت/آسیب/Death Save) ─────────
    // به CharacterViewModelCombat.kt منتقل شد (توابع توسعه روی همین کلاس).


    // ───────── اعتیاد / جراحت بحرانی / Quick Fix ─────────
    // به CharacterViewModelCare.kt منتقل شد (توابع توسعه روی همین کلاس).


    // ───────── دامنه‌ی وسیله‌های نقلیه ─────────
    // همه‌ی متدهای vehicle* / driving / mounted weapon / family vehicle
    // به فایل CharacterViewModelVehicles.kt منتقل شدند (توابع توسعه روی
    // همین کلاس). امضای عمومی و رفتارشان تغییر نکرده است.

    private fun normalizeRoleAllocations(role: String, points: Map<String, Int>, rank: Int): Map<String, Int> {
        val keys = when {
            role.equals("Solo", true) -> listOf("damageDeflection", "initiativeReaction", "precisionAttack", "spotWeakness", "threatDetection", "fumble")
            role.equals("Tech", true) -> listOf("field", "upgrade", "fab", "invent")
            role.equals("Medtech", true) -> listOf("surgery", "pharma", "cryo")
            else -> return points
        }
        var remaining = if (role.equals("Tech", true)) rank * 2 else rank
        val out = mutableMapOf<String, Int>()
        for (key in keys) {
            val raw = (points[key] ?: 0).coerceAtLeast(0)
            if (role.equals("Solo", true)) {
                when (key) {
                    "fumble" -> {
                        // گزارش تست ۳.۵: Fumble Recovery هزینه‌ی ثابت ۴ امتیاز دارد؛ مقدار
                        // ذخیره‌شده هم باید همان ۴ امتیاز واقعی باشد (نه فلگ ۱) تا شرط
                        // سمت رزم «>= 4» درست کار کند. سیوهای قدیمی با فلگ ۱ در
                        // GameRules.syncDerived به ۴ مهاجرت می‌شوند.
                        if (raw > 0 && remaining >= 4) { out[key] = 4; remaining -= 4 }
                        continue
                    }
                    "damageDeflection" -> {
                        val spent = minOf(raw - (raw % 2), remaining - (remaining % 2)).coerceAtLeast(0)
                        if (spent > 0) out[key] = spent
                        remaining -= spent
                        continue
                    }
                    "precisionAttack" -> {
                        val spent = minOf(raw - (raw % 3), remaining - (remaining % 3)).coerceAtLeast(0)
                        if (spent > 0) out[key] = spent
                        remaining -= spent
                        continue
                    }
                }
            }
            val cap = if (role.equals("Tech", true)) rank else Int.MAX_VALUE
            val affordable = minOf(raw, remaining, cap)
            if (affordable > 0) out[key] = affordable
            remaining -= affordable
        }
        return out
    }

}
