package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.data.model.UserWallet
import com.example.data.model.MatchHistory
import com.example.data.model.WithdrawalRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // Wallet operations
    @Query("SELECT * FROM user_wallet WHERE id = 1 LIMIT 1")
    fun getWalletFlow(): Flow<UserWallet?>

    @Query("SELECT * FROM user_wallet WHERE id = 1 LIMIT 1")
    suspend fun getWalletDirect(): UserWallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: UserWallet)

    // Match History operations
    @Query("SELECT * FROM match_history ORDER BY timestamp DESC")
    fun getAllMatchesFlow(): Flow<List<MatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchHistory)

    // Withdrawal Request operations
    @Query("SELECT * FROM withdrawal_request ORDER BY timestamp DESC")
    fun getAllWithdrawalsFlow(): Flow<List<WithdrawalRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(request: WithdrawalRequest)

    @Query("UPDATE withdrawal_request SET status = :status WHERE id = :id")
    suspend fun updateWithdrawalStatus(id: Long, status: String)
}

@Database(
    entities = [UserWallet::class, MatchHistory::class, WithdrawalRequest::class],
    version = 1,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
