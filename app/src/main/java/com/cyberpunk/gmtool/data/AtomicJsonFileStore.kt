package com.cyberpunk.gmtool.data

import android.system.Os
import java.io.File
import java.io.FileOutputStream

/**
 * Crash-safe text store shared by long-lived data.
 * Writes through .tmp, keeps rotating last-known-good backups (.bak, .bak2, .bak3),
 * and participates in the process-wide lock.
 *
 * گزارش تست ۶.۴: قبلاً فقط یک .bak نگه داشته می‌شد؛ یعنی دو نوشتن پیاپیِ بد
 * آخرین وضعیت سالم را برای همیشه از بین می‌برد. حالا سه نسل چرخش می‌کنند:
 * با هر نوشتن، .bak2 → .bak3 و .bak → .bak2 و فایل اصلی → .bak. مسیر بازیابی
 * به ترتیب اصلی → .bak → .bak2 → .bak3 را امتحان می‌کند. قرارداد .bak به عنوان
 * «آخرین پشتیبان» برای همه‌ی فراخوان‌های موجود (readBackupText و
 * repairMainFromKnownGoodBackup) بدون تغییر باقی می‌ماند.
 */
class AtomicJsonFileStore(private val file: File) {
    private val backup = File(file.parentFile, "${file.name}.bak")
    private val backup2 = File(file.parentFile, "${file.name}.bak2")
    private val backup3 = File(file.parentFile, "${file.name}.bak3")

    data class ReadResult(val text: String?, val recoveredFromBackup: Boolean = false, val warning: String? = null)

    fun read(): ReadResult = PersistenceRuntime.locked {
        val primary = runCatching { if (file.exists()) file.readText() else null }.getOrNull()
        if (primary != null) return@locked ReadResult(primary)
        for (bak in listOf(backup, backup2, backup3)) {
            val fallback = runCatching { if (bak.exists()) bak.readText() else null }.getOrNull()
            if (fallback != null) return@locked ReadResult(
                fallback, true,
                "نسخه‌ی اصلی ذخیره قابل خواندن نبود؛ نسخه‌ی پشتیبان بازیابی شد."
            )
        }
        ReadResult(null)
    }

    fun write(text: String, expectedEpoch: Long? = null): Boolean = PersistenceRuntime.locked {
        if (expectedEpoch != null && !PersistenceRuntime.isCurrent(expectedEpoch)) return@locked false
        writeBytesCrashSafe(text.toByteArray(Charsets.UTF_8))
        true
    }

    /** Called only after the caller has successfully parsed the backup. */
    fun repairMainFromKnownGoodBackup() = PersistenceRuntime.locked {
        if (!backup.exists()) return@locked
        val tmp = File(file.parentFile, "${file.name}.recover.tmp")
        backup.copyTo(tmp, overwrite = true)
        syncFile(tmp)
        replaceWithoutTouchingBackup(tmp, file)
    }

    private fun writeBytesCrashSafe(bytes: ByteArray) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(tmp).use { out -> out.write(bytes); out.fd.sync() }

        // Preserve the current main as backup only after a complete temp file exists.
        if (file.exists()) {
            rotateBackups()
            val bakTmp = File(file.parentFile, "${backup.name}.tmp")
            file.copyTo(bakTmp, overwrite = true)
            syncFile(bakTmp)
            replaceWithoutTouchingBackup(bakTmp, backup)
        }
        replaceWithoutTouchingBackup(tmp, file)
    }

    /** یک نسل به همه اضافه می‌کند: bak3 دور ریخته، bak2→bak3، bak→bak2. */
    private fun rotateBackups() {
        if (backup3.exists()) backup3.delete()
        if (backup2.exists()) replaceWithoutTouchingBackup(backup2, backup3)
        if (backup.exists()) replaceWithoutTouchingBackup(backup, backup2)
    }

    private fun replaceWithoutTouchingBackup(source: File, target: File) {
        // android.system.Os.rename is available from API 21 and is atomic on the app's filesystem.
        runCatching { Os.rename(source.absolutePath, target.absolutePath) }.onSuccess { return }
        source.copyTo(target, overwrite = true)
        source.delete()
    }

    private fun syncFile(f: File) { runCatching { FileOutputStream(f, true).use { it.fd.sync() } } }

    fun mainFile(): File = file
    fun backupFile(): File = backup
    fun readBackupText(): String? = PersistenceRuntime.locked { runCatching { if (backup.exists()) backup.readText() else null }.getOrNull() }
}
