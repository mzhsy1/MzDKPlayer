package org.mz.mzdkplayer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration // 👈 记得导入
import androidx.sqlite.db.SupportSQLiteDatabase // 👈 记得导入

@Database(entities = [MediaCacheEntity::class], version = 2, exportSchema = false) // 👈 版本改为 2
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 👇 【定义 V1 到 V2 的迁移】 👇
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 新增的列都是 String 类型，在 Room 中对应 TEXT NOT NULL，
                // 必须提供 DEFAULT 值，否则无法将现有数据升级。
                db.execSQL("ALTER TABLE media_cache ADD COLUMN dataSourceType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE media_cache ADD COLUMN fileName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE media_cache ADD COLUMN connectionName TEXT NOT NULL DEFAULT ''")
            }
        }
        // 👆 【定义 V1 到 V2 的迁移】 👆

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mzdk_player_database"
                )
                    // 👇 【使用 addMigrations() 而非 fallbackToDestructiveMigration()】 👇
                    .addMigrations(MIGRATION_1_2)
                    // 👆 【使用 addMigrations()】 👆
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}