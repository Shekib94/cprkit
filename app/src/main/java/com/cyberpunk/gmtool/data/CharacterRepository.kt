package com.cyberpunk.gmtool.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Crash-safe, serialized JSON persistence with a last-known-good backup. */
class CharacterRepository(context: Context) {
    private val gson = Gson()
    private val file = File(context.filesDir, "characters.json")
    private val atomic = AtomicJsonFileStore(file)
    private val mutex = Mutex()
    private val listType = object : TypeToken<List<Character>>() {}.type
    @Volatile var lastLoadWarning: String? = null
        private set

    suspend fun loadCharacters(): List<Character> = withContext(Dispatchers.IO) {
        PersistenceRuntime.locked {
            lastLoadWarning = null
            fun parse(json: String?): Result<List<Character>?> = runCatching {
                if (json == null) return@runCatching null
                if (json.isBlank()) emptyList() else (gson.fromJson<List<Character>>(json, listType) ?: emptyList())
                    .map { raw ->
                        // Gson نمونه را بدون صدا زدن سازنده می‌سازد؛ هر فیلد غایب در JSON
                        // برای نوع‌های مرجع null می‌شود، حتی اگر در Kotlin غیر-null اعلام
                        // شده باشد. پس همه‌ی فیلدهای مرجع اینجا صریحاً ترمیم می‌شوند؛
                        // وگرنه یک سیو قدیمی، صفحه‌ی مبارزه/آمار را با NPE می‌بندد.
                        raw.copy(
                            addictions = raw.addictions ?: emptyList(),
                            // Gson فیلدهای غایب در JSON قدیمی را null می‌گذارد حتی اگر نوع
                            // غیرnullable باشد؛ uid خالی یعنی «بعداً در syncDerived ساخته می‌شود».
                            sheetUid = raw.sheetUid ?: "",
                            addictionRelapseUntil = raw.addictionRelapseUntil ?: emptyMap(),
                            addictionRelapseRisk = raw.addictionRelapseRisk ?: emptyList(),
                            activeDrugEffects = raw.activeDrugEffects ?: emptyMap(),
                            drugHumanityHeld = raw.drugHumanityHeld ?: emptyMap(),
                            combatEffects = raw.combatEffects ?: emptyMap(),
                            appliedTransactionIds = raw.appliedTransactionIds ?: emptyList(),
                            temporaryStatPenalties = raw.temporaryStatPenalties ?: emptyMap(),
                            criticalInjuries = raw.criticalInjuries ?: emptyList(),
                            nomadMotorpool = raw.nomadMotorpool ?: emptyList(),
                            nomadVehicleUpgrades = raw.nomadVehicleUpgrades ?: emptyList(),
                            ownedVehicles = raw.ownedVehicles ?: emptyList(),
                            stats = raw.stats ?: Stats(),
                            skills = raw.skills ?: emptyList(),
                            inventory = raw.inventory ?: emptyList(),
                            weapons = raw.weapons ?: emptyList(),
                            lifepath = raw.lifepath ?: LifepathData(),
                            roleAbilityPoints = raw.roleAbilityPoints ?: emptyMap(),
                            medtechPharmaceuticals = raw.medtechPharmaceuticals ?: emptyList(),
                            medtechPatients = raw.medtechPatients ?: emptyList(),
                            mediaCases = raw.mediaCases ?: emptyList(),
                            phoneNotes = raw.phoneNotes ?: emptyList(),
                            objectives = raw.objectives ?: emptyList(),
                            contacts = raw.contacts ?: emptyList()
                        )
                    }.map(GameRules::syncDerived)
            }

            val rr = atomic.read()
            val primary = parse(rr.text)
            if (primary.isSuccess) return@locked primary.getOrNull() ?: emptyList()
            val backup = parse(atomic.readBackupText())
            if (backup.isSuccess && backup.getOrNull() != null) {
                lastLoadWarning = "فایل اصلی ذخیره خراب بود؛ نسخه‌ی پشتیبان سالم بازیابی و فایل اصلی ترمیم شد."
                atomic.repairMainFromKnownGoodBackup()
                return@locked backup.getOrNull() ?: emptyList()
            }
            if (file.exists() || atomic.backupFile().exists()) {
                lastLoadWarning = "فایل ذخیره قابل خواندن نیست. فایل خراب حذف نشده؛ برای بازیابی/بررسی نگه داشته شده است."
            }
            emptyList()
        }
    }

    suspend fun saveCharacters(characters: List<Character>, expectedEpoch: Long? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock { atomic.write(gson.toJson(characters), expectedEpoch) }
    }
}
