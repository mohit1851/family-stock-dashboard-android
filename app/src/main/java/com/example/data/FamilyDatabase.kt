package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
