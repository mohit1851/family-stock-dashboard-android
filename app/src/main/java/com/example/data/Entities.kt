package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String, // lowercase unique user handle
    val fullName: String,
    val passwordHash: String, // Securely hashed passcode
    val salt: String,
    val groupId: String? = null // Associated group ID, if any
)

@Entity(tableName = "family_groups")
data class FamilyGroup(
    @PrimaryKey val groupId: String,
    val name: String,
    val inviteCode: String, // Code used to join the group
    val ownerUsername: String
)

@Entity(tableName = "stock_assets")
data class StockAsset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val name: String,
    val shares: Double,
    val avgPrice: Double,
    val currentPrice: Double,
    val dailyChangePercentage: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val groupId: String = "SHARMA_GROUP"
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "Dad", "Me", "System"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val recommendedStockSymbol: String? = null,
    val targetNotificationPrice: Double? = null,
    val groupId: String = "SHARMA_GROUP"
)

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val targetPrice: Double,
    val triggerType: String, // "ABOVE", "BELOW"
    val isTriggered: Boolean = false,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val groupId: String = "SHARMA_GROUP"
)

@Entity(tableName = "watchlist_items")
data class WatchlistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val name: String,
    val targetPrice: Double,
    val currentPrice: Double,
    val dailyChangePercentage: Double = 0.0,
    val isTriggered: Boolean = false,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val groupId: String = "SHARMA_GROUP"
)


