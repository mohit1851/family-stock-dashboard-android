package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StockAsset::class, ChatMessage::class, PriceAlert::class, User::class, FamilyGroup::class, WatchlistItem::class],
    version = 3,
    exportSchema = false
)
abstract class FamilyDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun chatDao(): ChatDao
    abstract fun alertDao(): AlertDao
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: FamilyDatabase? = null

        fun getDatabase(context: Context): FamilyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FamilyDatabase::class.java,
                    "family_stock_dashboard_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSqlIgnoringFailures(
                    """
                    CREATE TABLE IF NOT EXISTS `family_groups` (
                        `groupId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `inviteCode` TEXT NOT NULL,
                        `ownerUsername` TEXT NOT NULL,
                        PRIMARY KEY(`groupId`)
                    )
                    """.trimIndent()
                )
                db.execSqlIgnoringFailures(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `username` TEXT NOT NULL,
                        `fullName` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `salt` TEXT NOT NULL,
                        `groupId` TEXT,
                        PRIMARY KEY(`username`)
                    )
                    """.trimIndent()
                )
                db.execSqlIgnoringFailures("ALTER TABLE `stock_assets` ADD COLUMN `groupId` TEXT NOT NULL DEFAULT 'SHARMA_GROUP'")
                db.execSqlIgnoringFailures("ALTER TABLE `chat_messages` ADD COLUMN `groupId` TEXT NOT NULL DEFAULT 'SHARMA_GROUP'")
                db.execSqlIgnoringFailures("ALTER TABLE `price_alerts` ADD COLUMN `groupId` TEXT NOT NULL DEFAULT 'SHARMA_GROUP'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSqlIgnoringFailures("ALTER TABLE `chat_messages` ADD COLUMN `targetNotificationPrice` REAL")
                db.execSqlIgnoringFailures(
                    """
                    CREATE TABLE IF NOT EXISTS `watchlist_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `symbol` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetPrice` REAL NOT NULL,
                        `currentPrice` REAL NOT NULL,
                        `dailyChangePercentage` REAL NOT NULL,
                        `isTriggered` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `groupId` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private fun SupportSQLiteDatabase.execSqlIgnoringFailures(sql: String) {
            try {
                execSQL(sql)
            } catch (_: Exception) {
                // Older prototype builds may already contain parts of the target schema.
            }
        }
    }
}
