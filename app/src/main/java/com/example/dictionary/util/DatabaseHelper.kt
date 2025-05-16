package com.example.dictionary.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "DatabaseHelper"

    /**
     * Xóa database hiện tại và tạo database mới
     */
    fun resetDatabase() {
        try {
            // Xóa file database
            val dbFile = context.getDatabasePath("dictionary_database")
            if (dbFile.exists()) {
                val deleted = dbFile.delete()
                Log.d(TAG, "Database deleted: $deleted")
            }

            // Xóa các file liên quan
            val dbShm = File(dbFile.path + "-shm")
            if (dbShm.exists()) {
                dbShm.delete()
            }

            val dbWal = File(dbFile.path + "-wal")
            if (dbWal.exists()) {
                dbWal.delete()
            }

            Log.d(TAG, "Database reset completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting database", e)
        }
    }

    /**
     * Sao lưu database hiện tại
     */
    fun backupDatabase(): Boolean {
        try {
            val dbFile = context.getDatabasePath("dictionary_database")
            if (!dbFile.exists()) {
                return false
            }

            val backupDir = File(context.filesDir, "backup")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val backupFile = File(backupDir, "dictionary_database_backup")
            dbFile.copyTo(backupFile, overwrite = true)

            // Sao lưu các file liên quan
            val dbShm = File(dbFile.path + "-shm")
            if (dbShm.exists()) {
                val backupShm = File(backupDir, "dictionary_database_backup-shm")
                dbShm.copyTo(backupShm, overwrite = true)
            }

            val dbWal = File(dbFile.path + "-wal")
            if (dbWal.exists()) {
                val backupWal = File(backupDir, "dictionary_database_backup-wal")
                dbWal.copyTo(backupWal, overwrite = true)
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up database", e)
            return false
        }
    }

    /**
     * Khôi phục database từ bản sao lưu
     */
    fun restoreDatabase(): Boolean {
        try {
            val backupDir = File(context.filesDir, "backup")
            val backupFile = File(backupDir, "dictionary_database_backup")
            if (!backupFile.exists()) {
                return false
            }

            val dbFile = context.getDatabasePath("dictionary_database")

            // Xóa database hiện tại
            if (dbFile.exists()) {
                dbFile.delete()

                val dbShm = File(dbFile.path + "-shm")
                if (dbShm.exists()) {
                    dbShm.delete()
                }

                val dbWal = File(dbFile.path + "-wal")
                if (dbWal.exists()) {
                    dbWal.delete()
                }
            }

            // Khôi phục từ bản sao lưu
            backupFile.copyTo(dbFile, overwrite = true)

            // Khôi phục các file liên quan
            val backupShm = File(backupDir, "dictionary_database_backup-shm")
            if (backupShm.exists()) {
                val dbShm = File(dbFile.path + "-shm")
                backupShm.copyTo(dbShm, overwrite = true)
            }

            val backupWal = File(backupDir, "dictionary_database_backup-wal")
            if (backupWal.exists()) {
                val dbWal = File(dbFile.path + "-wal")
                backupWal.copyTo(dbWal, overwrite = true)
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring database", e)
            return false
        }
    }
}
