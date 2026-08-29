package com.sovereign.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CaptureEntity::class,
        LanguagePackEntity::class,
        SettingEntity::class,
        ModelConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SovereignDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun languagePackDao(): LanguagePackDao
    abstract fun settingDao(): SettingDao
    abstract fun modelConfigDao(): ModelConfigDao

    companion object {
        @Volatile private var INSTANCE: SovereignDatabase? = null
        private const val DATABASE_NAME = "sovereign_db"

        fun getInstance(context: Context): SovereignDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SovereignDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @androidx.room.TypeConverter
    fun fromList(list: List<String>): String = list.joinToString(",")

    @androidx.room.TypeConverter
    fun toList(string: String): List<String> = if (string.isBlank()) emptyList() else string.split(",")

    @androidx.room.TypeConverter
    fun fromLongList(list: List<Long>): String = list.joinToString(",")

    @androidx.room.TypeConverter
    fun toLongList(string: String): List<Long> = if (string.isBlank()) emptyList() else string.split(",").map { it.toLong() }
}