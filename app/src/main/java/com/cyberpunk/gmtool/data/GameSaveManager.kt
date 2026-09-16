package com.cyberpunk.gmtool.data



import android.content.Context
import android.system.Os
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val GAME_SAVE_MAGIC = "CPRGMTOOLS_GAME_SAVE"
private const val GAME_SAVE_VERSION = 2

data class GameFileBlob(val path: String = "", val base64: String = "", val sha256: String = "")
data class GamePrefValue(
    val type: String = "string", val stringValue: String? = null, val stringSetValue: Set<String>? = null,
    val intValue: Int? = null, val longValue: Long? = null, val floatValue: Float? = null, val booleanValue: Boolean? = null
)
data class GamePreferenceSnapshot(val name: String = "", val values: Map<String, GamePrefValue> = emptyMap())
data class GameArchive(
    val magic: String = GAME_SAVE_MAGIC,
    val formatVersion: Int = GAME_SAVE_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val label: String = "",
    val appVersion: String = "unknown",
    val characterSchemaVersion: Int = 2,
    val campaignSchemaVersion: Int = 2,
    val sessionSchemaVersion: Int = 4,
    val combatSchemaVersion: Int = 2,
    val files: List<GameFileBlob> = emptyList(),
    val preferences: List<GamePreferenceSnapshot> = emptyList()
)
data class GameSaveSlot(val file: File, val label: String, val createdAt: Long, val automatic: Boolean)

/** Whole-game, checksum-verified, rollback-capable snapshots. */
class GameSaveManager(private val context: Context) {
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    private val saveDir = File(context.filesDir, "game_saves").apply { mkdirs() }
    private val restoreJournal = File(context.filesDir, "restore_in_progress.cprjournal")
    private val persistentFileNames = listOf("characters.json", "campaigns_v2.json", "sessions_v3.json", "battle_maps_v1.json")
    private val restorablePreferenceNames = setOf(
        "cpr_settings", "cpr_active_combat", "gm_net_architectures", "gm_night_markets", "gm_clocks", "battle_map_ui"
    )

    init {
        // If Android killed the process in the middle of a previous multi-file restore, roll back
        // before any screen/store can observe a mixed game state.
        recoverInterruptedRestore()
    }

    fun createManualSave(label: String = defaultManualLabel()): GameSaveSlot = PersistenceRuntime.locked {
        val archive = buildArchive(label)
        val safe = safeName(label).ifBlank { "save" }
        val file = File(saveDir, "manual_${archive.createdAt}_$safe.cprgame.json")
        atomicWrite(file, gson.toJson(archive))
        GameSaveSlot(file, archive.label, archive.createdAt, false)
    }

    fun createAutosave(reason: String = "ذخیره خودکار"): GameSaveSlot = PersistenceRuntime.locked {
        val archive = buildArchive(reason)
        val file = File(saveDir, "auto_${archive.createdAt}.cprgame.json")
        atomicWrite(file, gson.toJson(archive))
        pruneAutosaves(20)
        GameSaveSlot(file, archive.label, archive.createdAt, true)
    }

    fun maybeCreateAutosave(minIntervalMillis: Long = 60_000L): GameSaveSlot? = PersistenceRuntime.locked {
        val meta = context.getSharedPreferences("cpr_game_save_meta", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = meta.getLong("last_auto", 0L)
        if (now - last < minIntervalMillis) return@locked null
        val slot = createAutosave()
        meta.edit().putLong("last_auto", now).commit()
        slot
    }

    fun listSaves(): List<GameSaveSlot> = saveDir.listFiles().orEmpty()
        .filter { it.isFile && it.name.endsWith(".cprgame.json") }
        .mapNotNull { f -> runCatching {
            val a = gson.fromJson(f.readText(), GameArchive::class.java) ?: return@runCatching null
            if (a.magic != GAME_SAVE_MAGIC) null else GameSaveSlot(f, a.label, a.createdAt, f.name.startsWith("auto_"))
        }.getOrNull() }.sortedByDescending { it.createdAt }

    fun delete(slot: GameSaveSlot): Boolean = slot.file.delete()

    fun validateJson(json: String): Result<GameArchive> = runCatching {
        val archive = gson.fromJson(json, GameArchive::class.java) ?: error("فایل ذخیره خالی است")
        require(archive.magic == GAME_SAVE_MAGIC) { "این فایل ذخیره کامل CPRGMTOOLS نیست." }
        require(archive.formatVersion in 1..GAME_SAVE_VERSION) { "نسخه ذخیره پشتیبانی نمی‌شود." }
        require(archive.files.map { it.path }.distinct().size == archive.files.size) { "فایل ذخیره مسیر تکراری دارد." }
        archive.files.forEach { blob ->
            require(isAllowedGamePath(blob.path)) { "مسیر غیرمجاز در فایل ذخیره: ${blob.path}" }
            val bytes = Base64.decode(blob.base64, Base64.DEFAULT)
            require(sha256(bytes) == blob.sha256) { "Checksum فایل ${blob.path} معتبر نیست." }
        }
        if (archive.formatVersion >= 2) {
            archive.preferences.forEach { require(it.name in restorablePreferenceNames) { "Preference غیرمجاز: ${it.name}" } }
        }
        archive
    }

    fun importAsSlot(json: String, labelOverride: String? = null): Result<GameSaveSlot> = validateJson(json).map { archive ->
        val imported = archive.copy(label = labelOverride?.takeIf { it.isNotBlank() } ?: archive.label)
        val file = File(saveDir, "manual_${System.currentTimeMillis()}_imported.cprgame.json")
        atomicWrite(file, gson.toJson(imported))
        GameSaveSlot(file, imported.label, imported.createdAt, false)
    }

    fun restore(slot: GameSaveSlot): Result<Unit> = restoreJson(slot.file.readText())

    fun restoreJson(json: String): Result<Unit> = validateJson(json).mapCatching { archive ->
        PersistenceRuntime.locked {
            // Durable user-visible rollback point first.
            createManualSave("قبل از Load")

            // Crash journal: survives process death. On next app launch init{} restores this archive.
            val rollbackArchive = buildArchive("restore rollback journal")
            atomicWriteBytes(restoreJournal, gson.toJson(rollbackArchive).toByteArray(Charsets.UTF_8), keepBackup = false)

            try {
                applyArchiveUnchecked(archive)
                if (!restoreJournal.delete() && restoreJournal.exists()) error("نمی‌توان restore journal را پاک کرد")
                File(restoreJournal.parentFile, restoreJournal.name + ".bak").delete()
                PersistenceRuntime.publishSuccessfulRestore()
            } catch (t: Throwable) {
                runCatching { applyArchiveUnchecked(rollbackArchive) }
                restoreJournal.delete()
                File(restoreJournal.parentFile, restoreJournal.name + ".bak").delete()
                throw t
            }
        }
    }

    private fun applyArchiveUnchecked(archive: GameArchive) {
        val blobs = archive.files.associateBy { it.path }
        persistentFileNames.forEach { name ->
            val bytes = blobs[name]?.let { Base64.decode(it.base64, Base64.DEFAULT) } ?: defaultBytes(name)
            atomicWriteBytes(File(context.filesDir, name), bytes, keepBackup = true)
        }
        val mapDir = File(context.filesDir, "battle_maps")
        if (mapDir.exists()) mapDir.listFiles().orEmpty().filter { it.isFile }.forEach { it.delete() } else mapDir.mkdirs()
        blobs.values.filter { it.path.startsWith("battle_maps/") }.forEach { blob ->
            val name = blob.path.removePrefix("battle_maps/")
            require(!name.contains('/') && !name.contains('\\') && name.isNotBlank())
            atomicWriteBytes(File(mapDir, name), Base64.decode(blob.base64, Base64.DEFAULT), keepBackup = false)
        }
        clearWhitelistedPreferences()
        archive.preferences.filter { it.name in restorablePreferenceNames }.forEach(::restorePreferences)
    }

    private fun recoverInterruptedRestore() = PersistenceRuntime.locked {
        if (!restoreJournal.exists()) return@locked
        val recovered = runCatching {
            val archive = gson.fromJson(restoreJournal.readText(), GameArchive::class.java)
                ?: error("restore journal خالی است")
            require(archive.magic == GAME_SAVE_MAGIC)
            archive.files.forEach { blob ->
                require(isAllowedGamePath(blob.path))
                val bytes = Base64.decode(blob.base64, Base64.DEFAULT)
                require(sha256(bytes) == blob.sha256)
            }
            applyArchiveUnchecked(archive)
        }
        if (recovered.isSuccess) {
            restoreJournal.delete()
            File(restoreJournal.parentFile, restoreJournal.name + ".bak").delete()
        }
        // If the journal itself is damaged, keep it for manual diagnosis instead of silently deleting it.
    }

    private fun buildArchive(label: String): GameArchive = PersistenceRuntime.locked {
        CampaignStore(context).load()
        SessionStore(context).load()
        val coreFiles = persistentFileNames.map { name ->
            val file = File(context.filesDir, name)
            val bytes = if (file.exists()) file.readBytes() else defaultBytes(name)
            GameFileBlob(name, Base64.encodeToString(bytes, Base64.NO_WRAP), sha256(bytes))
        }
        val mapFiles = File(context.filesDir, "battle_maps").listFiles().orEmpty().filter { it.isFile }.map { file ->
            val bytes = file.readBytes()
            GameFileBlob("battle_maps/${file.name}", Base64.encodeToString(bytes, Base64.NO_WRAP), sha256(bytes))
        }
        val files = coreFiles + mapFiles
        GameArchive(
            label = label.trim().ifBlank { defaultManualLabel() },
            appVersion = appVersion(),
            files = files,
            preferences = snapshotPreferences(restorablePreferenceNames)
        )
    }

    private fun snapshotPreferences(names: Set<String>): List<GamePreferenceSnapshot> = names.sorted().map { name ->
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        GamePreferenceSnapshot(name, prefs.all.mapValues { (_, value) -> encodePref(value) })
    }

    private fun encodePref(value: Any?): GamePrefValue = when (value) {
        is String -> GamePrefValue(type = "string", stringValue = value)
        is Set<*> -> GamePrefValue(type = "stringSet", stringSetValue = value.filterIsInstance<String>().toSet())
        is Int -> GamePrefValue(type = "int", intValue = value)
        is Long -> GamePrefValue(type = "long", longValue = value)
        is Float -> GamePrefValue(type = "float", floatValue = value)
        is Boolean -> GamePrefValue(type = "boolean", booleanValue = value)
        else -> GamePrefValue(type = "string", stringValue = value?.toString())
    }

    private fun clearWhitelistedPreferences() {
        restorablePreferenceNames.forEach { name ->
            check(context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()) { "پاک‌سازی $name ناموفق بود." }
        }
    }

    private fun restorePreferences(snapshot: GamePreferenceSnapshot) {
        require(snapshot.name in restorablePreferenceNames)
        val prefs = context.getSharedPreferences(snapshot.name, Context.MODE_PRIVATE)
        val e = prefs.edit().clear()
        snapshot.values.forEach { (key, value) -> when (value.type) {
            "string" -> e.putString(key, value.stringValue)
            "stringSet" -> e.putStringSet(key, value.stringSetValue ?: emptySet())
            "int" -> e.putInt(key, value.intValue ?: 0)
            "long" -> e.putLong(key, value.longValue ?: 0L)
            "float" -> e.putFloat(key, value.floatValue ?: 0f)
            "boolean" -> e.putBoolean(key, value.booleanValue ?: false)
        } }
        check(e.commit()) { "بازگردانی تنظیمات ${snapshot.name} ناموفق بود." }
    }

    private fun isAllowedGamePath(path: String): Boolean {
        if (path in persistentFileNames) return true
        if (!path.startsWith("battle_maps/")) return false
        val name = path.removePrefix("battle_maps/")
        return name.isNotBlank() && !name.contains('/') && !name.contains('\\') && name != "." && name != ".."
    }

    private fun defaultBytes(name: String): ByteArray = when (name) {
        "characters.json", "campaigns_v2.json" -> "[]"
        "sessions_v3.json" -> "{\"count\":1,\"sessions\":[],\"activeSessionId\":null}"
        "battle_maps_v1.json" -> "[]"
        else -> ""
    }.toByteArray(Charsets.UTF_8)

    private fun pruneAutosaves(max: Int) { listSaves().filter { it.automatic }.drop(max).forEach { it.file.delete() } }
    private fun defaultManualLabel(): String = gtr("Save %1s", SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date()))
    private fun safeName(value: String) = value.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun appVersion(): String = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }.getOrDefault("unknown")

    private fun atomicWrite(file: File, text: String) = atomicWriteBytes(file, text.toByteArray(Charsets.UTF_8), keepBackup = true)

    private fun atomicWriteBytes(file: File, bytes: ByteArray, keepBackup: Boolean) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(tmp).use { out -> out.write(bytes); out.fd.sync() }
        if (keepBackup && file.exists()) {
            val bak = File(file.parentFile, "${file.name}.bak")
            val bakTmp = File(file.parentFile, "${bak.name}.tmp")
            file.copyTo(bakTmp, overwrite = true)
            FileOutputStream(bakTmp, true).use { it.fd.sync() }
            moveReplace(bakTmp, bak)
        }
        moveReplace(tmp, file)
    }

    private fun moveReplace(source: File, target: File) {
        // API-21-safe atomic rename; avoids java.nio.file.Files (API 26) on minSdk 24 devices.
        runCatching { Os.rename(source.absolutePath, target.absolutePath) }.onSuccess { return }
        source.copyTo(target, overwrite = true)
        source.delete()
    }
}
