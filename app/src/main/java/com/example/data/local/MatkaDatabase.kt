package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MarketEntity::class, ResultRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MatkaDatabase : RoomDatabase() {
    abstract fun matkaDao(): MatkaDao

    companion object {
        @Volatile
        private var INSTANCE: MatkaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE markets ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE markets ADD COLUMN openTimeMinutes INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE markets ADD COLUMN closeTimeMinutes INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE markets ADD COLUMN offDaysCsv TEXT NOT NULL DEFAULT 'DEFAULT'")
            }
        }

        fun getInstance(context: Context): MatkaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MatkaDatabase::class.java,
                    "dpboss_smart_analytics.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
