package com.example.dictionary.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dictionary.data.model.ChatMessage
import com.example.dictionary.data.model.Word

// Tăng version lên 4 để bắt buộc migration mới
@Database(entities = [Word::class, ChatMessage::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    // Thử tạo database với migration
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "dictionary_database"
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                        .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    // Nếu migration thất bại, tạo database mới với fallback
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "dictionary_database_new" // Tạo database mới với tên khác
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                }
            }
        }

        // Migration từ version 1 lên 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Tạo bảng tạm thời để lưu dữ liệu
                database.execSQL("CREATE TABLE IF NOT EXISTS words_temp (word TEXT NOT NULL, translation TEXT NOT NULL, phonetic TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL DEFAULT 0, isFavorite INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(word))")

                // Copy dữ liệu từ bảng cũ sang bảng tạm (nếu bảng cũ tồn tại)
                try {
                    database.execSQL("INSERT OR IGNORE INTO words_temp SELECT word, translation, '', COALESCE(timestamp, 0), COALESCE(isFavorite, 0) FROM words")
                } catch (e: Exception) {
                    // Bảng cũ có thể không tồn tại hoặc có schema khác
                }

                // Xóa bảng cũ và đổi tên bảng tạm
                database.execSQL("DROP TABLE IF EXISTS words")
                database.execSQL("ALTER TABLE words_temp RENAME TO words")
            }
        }

        // Migration từ version 2 lên 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Tạo bảng chat_messages nếu chưa tồn tại
                database.execSQL("CREATE TABLE IF NOT EXISTS chat_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, message TEXT NOT NULL, isUser INTEGER NOT NULL, timestamp INTEGER NOT NULL DEFAULT 0)")
            }
        }

        // Migration từ version 3 lên 4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Đảm bảo tất cả các bảng có cấu trúc đúng

                // Tạo bảng words mới nếu cần
                database.execSQL("CREATE TABLE IF NOT EXISTS words_new (word TEXT NOT NULL, translation TEXT NOT NULL, phonetic TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL DEFAULT 0, isFavorite INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(word))")

                // Copy dữ liệu từ bảng cũ sang bảng mới (nếu bảng cũ tồn tại)
                try {
                    database.execSQL("INSERT OR IGNORE INTO words_new SELECT word, translation, COALESCE(phonetic, ''), COALESCE(timestamp, 0), COALESCE(isFavorite, 0) FROM words")
                } catch (e: Exception) {
                    // Bảng cũ có thể không tồn tại hoặc có schema khác
                }

                // Xóa bảng cũ và đổi tên bảng mới
                database.execSQL("DROP TABLE IF EXISTS words")
                database.execSQL("ALTER TABLE words_new RENAME TO words")

                // Tạo bảng chat_messages mới nếu cần
                database.execSQL("CREATE TABLE IF NOT EXISTS chat_messages_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, message TEXT NOT NULL, isUser INTEGER NOT NULL, timestamp INTEGER NOT NULL DEFAULT 0)")

                // Copy dữ liệu từ bảng cũ sang bảng mới (nếu bảng cũ tồn tại)
                try {
                    database.execSQL("INSERT OR IGNORE INTO chat_messages_new SELECT id, message, isUser, COALESCE(timestamp, 0) FROM chat_messages")
                } catch (e: Exception) {
                    // Bảng cũ có thể không tồn tại hoặc có schema khác
                }

                // Xóa bảng cũ và đổi tên bảng mới
                database.execSQL("DROP TABLE IF EXISTS chat_messages")
                database.execSQL("ALTER TABLE chat_messages_new RENAME TO chat_messages")
            }
        }
    }
}
