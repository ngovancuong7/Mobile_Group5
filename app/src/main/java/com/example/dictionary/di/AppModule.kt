package com.example.dictionary.di

import android.content.Context
import com.example.dictionary.BuildConfig
import com.example.dictionary.data.api.ChatGptApiService
import com.example.dictionary.data.api.FreeDictionaryApiService
import com.example.dictionary.data.api.RapidApiTranslateService
import com.example.dictionary.data.db.ChatMessageDao
import com.example.dictionary.data.db.DicTextDao
import com.example.dictionary.data.db.DictionaryDatabase
import com.example.dictionary.data.db.FavoriteWordDao
import com.example.dictionary.data.db.WordDao
import com.example.dictionary.util.DatabaseHelper
import com.example.dictionary.util.NetworkUtils
import com.example.dictionary.util.PreferencesManager
import com.example.dictionary.util.TextToSpeechManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDictionaryDatabase(@ApplicationContext context: Context): DictionaryDatabase {
        return try {
            DictionaryDatabase.getDatabase(context)
        } catch (e: Exception) {
            // Nếu có lỗi khi tạo database, xóa file database cũ và tạo mới
            context.deleteDatabase("dictionary_database")
            DictionaryDatabase.getDatabase(context)
        }
    }

    @Provides
    @Singleton
    fun provideWordDao(database: DictionaryDatabase): WordDao {
        return database.wordDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteWordDao(database: DictionaryDatabase): FavoriteWordDao {
        return database.favoriteWordDao()
    }

    @Provides
    @Singleton
    fun provideDicTextDao(database: DictionaryDatabase): DicTextDao {
        return database.dicTextDao()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: DictionaryDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    @Singleton
    fun provideDatabaseHelper(@ApplicationContext context: Context): DatabaseHelper {
        return DatabaseHelper(context)
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRapidApiTranslateService(okHttpClient: OkHttpClient, gson: Gson): RapidApiTranslateService {
        return Retrofit.Builder()
            .baseUrl("https://google-translate113.p.rapidapi.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RapidApiTranslateService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatGptApiService(okHttpClient: OkHttpClient, gson: Gson): ChatGptApiService {
        return Retrofit.Builder()
            .baseUrl("https://free-chatgpt-api.p.rapidapi.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ChatGptApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideFreeDictionaryApiService(okHttpClient: OkHttpClient, gson: Gson): FreeDictionaryApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.dictionaryapi.dev/api/v2/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(FreeDictionaryApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTextToSpeechManager(@ApplicationContext context: Context): TextToSpeechManager {
        return TextToSpeechManager(context)
    }

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }
}
