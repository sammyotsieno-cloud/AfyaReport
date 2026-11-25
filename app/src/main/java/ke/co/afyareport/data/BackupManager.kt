package ke.co.afyareport.data

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/** Whole-database backup and restore through files the user picks. Works on every Android version. */
object BackupManager {
    private const val PREFS = "afyareport_prefs"
    private const val KEY_LAST = "last_backup"

    fun lastBackup(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST, 0L)

    suspend fun backupTo(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.get(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            val file = context.getDatabasePath(AppDatabase.NAME)
            val written = context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
                true
            } ?: false
            if (written) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
            }
            written
        } catch (e: Exception) {
            false
        }
    }

    /** Returns null when the restore worked, otherwise a message for the user. */
    suspend fun restoreFrom(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "restore.tmp")
        try {
            val opened = context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
                true
            } ?: false
            if (!opened) return@withContext "That file could not be opened."

            val header = ByteArray(100)
            val read = tmp.inputStream().use { it.read(header) }
            if (read < 100 || String(header, 0, 15, Charsets.US_ASCII) != "SQLite format 3") {
                tmp.delete()
                return@withContext "That is not an AfyaReport backup file."
            }
            val fileVersion = ByteBuffer.wrap(header, 60, 4).int
            val appDb = AppDatabase.get(context)
            val current = appDb.openHelper.readableDatabase.version
            if (fileVersion < 1) {
                tmp.delete()
                return@withContext "That is not an AfyaReport backup file."
            }
            if (fileVersion > current) {
                tmp.delete()
                return@withContext "This backup was made by a newer version of the app. Update the app first."
            }
            val looksRight = try {
                SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READONLY).use { d ->
                    d.rawQuery(
                        "SELECT name FROM sqlite_master WHERE name IN ('visits', 'patients')", null
                    ).use { it.count == 2 }
                }
            } catch (e: Exception) {
                false
            }
            if (!looksRight) {
                tmp.delete()
                return@withContext "That is not an AfyaReport backup file."
            }

            appDb.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            val dbFile = context.getDatabasePath(AppDatabase.NAME)
            dbFile.copyTo(File(context.filesDir, "pre-restore.db"), overwrite = true)
            appDb.close()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            tmp.copyTo(dbFile, overwrite = true)
            tmp.delete()
            null
        } catch (e: Exception) {
            tmp.delete()
            "The restore failed (${e.message ?: "unknown error"}). Close the app fully and open it again before trying anything else."
        }
    }

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (intent != null) context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
