package com.salespro.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.salespro.data.AppDatabase
import com.salespro.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Full backup / restore of the shop's data as one ZIP file.
 *
 * ZIP layout:
 *   retailpilot-backup.json        manifest (format, database version, settings)
 *   database/sales_database        the Room/SQLite database
 *   files/product_images/<file>    product photos
 *
 * The user picks where the file goes (Google Drive, Downloads, e-mail…)
 * through the system file picker, so the backup can live outside the phone.
 */
object BackupManager {
    private const val DB_NAME = "sales_database"
    private const val MANIFEST = "retailpilot-backup.json"
    private const val DB_ENTRY = "database/$DB_NAME"
    private const val IMAGES_DIR = "product_images"
    private const val IMAGES_PREFIX = "files/product_images/"
    private const val FORMAT_VERSION = 1
    private const val SAFETY_COPY = "pre_restore_backup.db"

    class InvalidBackupException : Exception("Not a RetailPilot backup")
    class NewerBackupException : Exception("Backup is from a newer app version")

    fun suggestedFileName(): String =
        "RetailPilot-backup-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date()) + ".zip"

    /** Writes a full backup to [target] (a document Uri chosen by the user). */
    suspend fun exportBackup(context: Context, target: Uri) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        checkpoint(app)
        val dbFile = app.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) throw IOException("Database not found")

        val output = app.contentResolver.openOutputStream(target)
            ?: throw IOException("Cannot open the selected location")
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            val manifest = JSONObject().apply {
                put("format", FORMAT_VERSION)
                put("dbVersion", readUserVersion(dbFile))
                put("createdAt", System.currentTimeMillis())
                put("language", AppSettings.language.code)
                put("currency", AppSettings.currency.code)
            }
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(DB_ENTRY))
            dbFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()

            File(app.filesDir, IMAGES_DIR).listFiles()?.filter { it.isFile }?.forEach { image ->
                zip.putNextEntry(ZipEntry(IMAGES_PREFIX + image.name))
                image.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        AppSettings.setLastBackupAt(System.currentTimeMillis())
    }

    /**
     * Replaces all local data with the backup in [source].
     * Call [restartApp] afterwards: the database is closed during restore.
     */
    suspend fun restoreBackup(context: Context, source: Uri) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val staging = File(app.cacheDir, "restore_staging")
        staging.deleteRecursively()
        val stagedImages = File(staging, "images").apply { mkdirs() }
        var manifest: JSONObject? = null
        var stagedDb: File? = null

        try {
            // 1) Unpack into a staging folder and validate before touching real data
            val input = app.contentResolver.openInputStream(source) ?: throw InvalidBackupException()
            try {
                ZipInputStream(BufferedInputStream(input)).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val name = entry.name
                        when {
                            entry.isDirectory -> Unit
                            name == MANIFEST ->
                                manifest = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                            name == DB_ENTRY -> {
                                val file = File(staging, DB_NAME)
                                file.outputStream().use { zip.copyTo(it) }
                                stagedDb = file
                            }
                            name.startsWith(IMAGES_PREFIX) -> {
                                val fileName = name.removePrefix(IMAGES_PREFIX)
                                // Only plain file names (protects against "../" paths)
                                if (fileName.isNotBlank() && !fileName.contains('/') &&
                                    !fileName.contains('\\') && fileName != "." && fileName != ".."
                                ) {
                                    File(stagedImages, fileName).outputStream().use { zip.copyTo(it) }
                                }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            } catch (e: InvalidBackupException) {
                throw e
            } catch (e: Exception) {
                throw InvalidBackupException()
            }

            val backupManifest = manifest ?: throw InvalidBackupException()
            val backupDb = stagedDb ?: throw InvalidBackupException()
            val backupVersion = readUserVersion(backupDb)
            if (backupVersion <= 0) throw InvalidBackupException()

            val database = AppDatabase.getDatabase(app)
            val currentVersion = database.openHelper.readableDatabase.version
            if (backupVersion > currentVersion) throw NewerBackupException()

            // 2) Keep a safety copy of the current database on the device
            checkpoint(app)
            val dbFile = app.getDatabasePath(DB_NAME)
            if (dbFile.exists()) {
                dbFile.copyTo(File(app.filesDir, SAFETY_COPY), overwrite = true)
            }

            // 3) Close the database and swap the files
            AppDatabase.closeInstance()
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                File(dbFile.path + suffix).delete()
            }
            dbFile.parentFile?.mkdirs()
            backupDb.copyTo(dbFile, overwrite = true)

            val imagesDir = File(app.filesDir, IMAGES_DIR)
            imagesDir.deleteRecursively()
            imagesDir.mkdirs()
            stagedImages.listFiles()?.forEach { it.copyTo(File(imagesDir, it.name), overwrite = true) }

            AppSettings.restoreFromBackup(
                languageCode = backupManifest.optString("language").ifBlank { null },
                currencyCode = backupManifest.optString("currency").ifBlank { null }
            )
        } finally {
            staging.deleteRecursively()
        }
    }

    /** Restarts the app so everything reopens with the restored data. */
    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    /** Moves all WAL content into the main database file so a file copy is complete. */
    private fun checkpoint(app: Context) {
        AppDatabase.getDatabase(app).openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(FULL)")
            .use { it.moveToFirst() }
    }

    /** Reads SQLite's user_version (= Room schema version); 0 if the file is not SQLite. */
    private fun readUserVersion(file: File): Int {
        if (file.length() < 100) return 0
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(16)
            raf.readFully(header)
            if (String(header, Charsets.US_ASCII) != "SQLite format 3\u0000") return 0
            raf.seek(60)
            return raf.readInt()
        }
    }
}
