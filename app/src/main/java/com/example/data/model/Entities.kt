package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_wallet")
data class UserWallet(
    @PrimaryKey val id: Int = 1, // Single-row configuration
    val balance: Double = 500.0, // Starting PKR balance
    val highScore: Int = 0,
    val lifetimeRuns: Int = 0,
    val matchesPlayed: Int = 0
)

@Entity(tableName = "match_history")
data class MatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bowlerName: String,
    val runsScored: Int,
    val multiplier: Double,
    val pkrEarned: Double,
    val ballsFaced: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "withdrawal_request")
data class WithdrawalRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val referenceId: String,
    val provider: String, // "JazzCash" or "EasyPaisa"
    val accountNumber: String,
    val accountTitle: String,
    val amount: Double,
    val status: String, // "Pending" or "Approved"
    val timestamp: Long = System.currentTimeMillis()
)
