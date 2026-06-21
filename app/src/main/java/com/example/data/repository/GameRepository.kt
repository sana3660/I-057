package com.example.data.repository

import com.example.data.local.GameDao
import com.example.data.model.UserWallet
import com.example.data.model.MatchHistory
import com.example.data.model.WithdrawalRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.random.Random

class GameRepository(private val gameDao: GameDao) {

    // Fetch the wallet, ensuring a default entry exists if null
    val walletFlow: Flow<UserWallet> = gameDao.getWalletFlow().map { wallet ->
        if (wallet == null) {
            val defaultWallet = UserWallet()
            gameDao.insertWallet(defaultWallet)
            defaultWallet
        } else {
            wallet
        }
    }

    val matchHistoryFlow: Flow<List<MatchHistory>> = gameDao.getAllMatchesFlow()

    val withdrawalFlow: Flow<List<WithdrawalRequest>> = gameDao.getAllWithdrawalsFlow()

    suspend fun saveWallet(wallet: UserWallet) {
        gameDao.insertWallet(wallet)
    }

    // Records a completed match, updates runs, high score, and increments PKR balance
    suspend fun recordMatch(runs: Int, ballsFaced: Int, multiplier: Double, bowlerName: String) {
        val directWallet = gameDao.getWalletDirect() ?: UserWallet()
        val pkrEarned = runs * 50.0 * multiplier // Base: 50 PKR per run

        // Save new match history
        val newMatch = MatchHistory(
            bowlerName = bowlerName,
            runsScored = runs,
            multiplier = multiplier,
            pkrEarned = pkrEarned,
            ballsFaced = ballsFaced
        )
        gameDao.insertMatch(newMatch)

        // Update Wallet metrics
        val newHighScore = if (runs > directWallet.highScore) runs else directWallet.highScore
        val newWallet = directWallet.copy(
            balance = directWallet.balance + pkrEarned,
            highScore = newHighScore,
            lifetimeRuns = directWallet.lifetimeRuns + runs,
            matchesPlayed = directWallet.matchesPlayed + 1
        )
        gameDao.insertWallet(newWallet)
    }

    // Handles a user withdrawal, deducting immediately if balance is sufficient
    suspend fun requestWithdrawal(
        provider: String,
        accountNumber: String,
        accountTitle: String,
        amount: Double
    ): Boolean {
        val directWallet = gameDao.getWalletDirect() ?: return false
        if (directWallet.balance < amount) return false

        // Deduct balance
        val updatedWallet = directWallet.copy(balance = directWallet.balance - amount)
        gameDao.insertWallet(updatedWallet)

        // Generate custom Pak txn ID format (e.g., TXN-JAZZ-8239A)
        val shortUuid = UUID.randomUUID().toString().take(6).uppercase()
        val referenceId = "TXN-${provider.uppercase().take(4)}-$shortUuid"

        val withdrawal = WithdrawalRequest(
            referenceId = referenceId,
            provider = provider,
            accountNumber = accountNumber,
            accountTitle = accountTitle,
            amount = amount,
            status = "Pending"
        )
        gameDao.insertWithdrawal(withdrawal)
        return true
    }

    // Approves a withdrawal mock status
    suspend fun approveWithdrawal(id: Long) {
        gameDao.updateWithdrawalStatus(id, "Approved")
    }
}
