package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_assets ORDER BY symbol ASC")
    fun getAllStocks(): Flow<List<StockAsset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockAsset)

    @Update
    suspend fun updateStock(stock: StockAsset)

    @Query("DELETE FROM stock_assets WHERE id = :id")
    suspend fun deleteStock(id: Int)

    @Query("DELETE FROM stock_assets")
    suspend fun deleteAllStocks()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<PriceAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlert)

    @Update
    suspend fun updateAlert(alert: PriceAlert)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun deleteAlert(id: Int)

    @Query("DELETE FROM price_alerts")
    suspend fun deleteAllAlerts()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM family_groups WHERE groupId = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): FamilyGroup?

    @Query("SELECT * FROM family_groups WHERE inviteCode = :inviteCode LIMIT 1")
    suspend fun getGroupByInviteCode(inviteCode: String): FamilyGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: FamilyGroup)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist_items ORDER BY timestamp DESC")
    fun getAllWatchlistItems(): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistItem)

    @Update
    suspend fun updateWatchlistItem(item: WatchlistItem)

    @Query("DELETE FROM watchlist_items WHERE id = :id")
    suspend fun deleteWatchlistItem(id: Int)

    @Query("DELETE FROM watchlist_items")
    suspend fun deleteAllWatchlistItems()
}


