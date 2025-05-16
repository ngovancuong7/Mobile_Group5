package com.example.dictionary.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dictionary.data.model.ChatMessage
import com.example.dictionary.data.model.DicText
import com.example.dictionary.data.model.FavoriteWord
import com.example.dictionary.data.model.Word

@Database(
    entities = [
        Word::class,
        FavoriteWord::class,
        DicText::class,
        ChatMessage::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DictionaryDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun favoriteWordDao(): FavoriteWordDao
    abstract fun dicTextDao(): DicTextDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: DictionaryDatabase? = null

        fun getDatabase(context: Context): DictionaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DictionaryDatabase::class.java,
                    "dictionary_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
