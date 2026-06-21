package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserWallet
import com.example.data.model.MatchHistory
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

sealed interface BallTimingResult {
    object Idle : BallTimingResult
    object Missed : BallTimingResult // Clean bowled / Out
    data class Hit(val runs: Int, val description: String, val feedbackColor: Long) : BallTimingResult
}

data class Bowler(
    val name: String,
    val style: String,
    val speedLabel: String,
    val durationMs: Long,
    val multiplier: Double,
    val color: Long,
    val description: String,
    val isSpin: Boolean = false,
    val isSwing: Boolean = false
)

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    // UI state flows from local DB
    val userWallet: StateFlow<UserWallet> = repository.walletFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserWallet()
        )

    val matchHistory: StateFlow<List<MatchHistory>> = repository.matchHistoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val withdrawalHistory: StateFlow<List<WithdrawalRequest>> = repository.withdrawalFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Available Bowlers
    val bowlers = listOf(
        Bowler("Gully Uncle", "Slow Medium", "Slow (Easy)", 1600L, 1.0, 0xFF4CAF50, "Straight slow balls. Perfect for practicing your bat contact."),
        Bowler("Saeed Afridi", "Boom Boom Leg Spin", "Medium (Spin)", 1250L, 1.5, 0xFF009688, "Tricky sine-wave wind drift! Moves left and right during flight.", isSpin = true),
        Bowler("Wasim King", "Reverse Swing Specialist", "Fast (Swing)", 850L, 2.0, 0xFFFF9800, "Inward swinging delivery! Curves sharply at the crease.", isSwing = true),
        Bowler("Shaheen Falcon", "Left Arm Yorker", "Super Fast", 600L, 3.0, 0xFFE91E63, "Lightning speed! Requires ultra-fast visual reflex.")
    )

    // Current selected configuration
    var selectedBowler by mutableStateOf(bowlers[0])
        private set

    fun selectBowler(bowler: Bowler) {
        if (!isBallActive) {
            selectedBowler = bowler
            resetMatchState()
        }
    }

    // Match Gameplay State
    var ballCount by mutableStateOf(0) // out of 6 balls
        private set
    var matchScore by mutableStateOf(0)
        private set
    var wicketsLost by mutableStateOf(0) // max 1 wicket allowed
        private set
    var isMatchFinished by mutableStateOf(false)
        private set

    // Active Delivery Animation Parameters
    var isBallActive by mutableStateOf(false)
        private set
    var ballProgress by mutableStateOf(0f) // 0.0 to 1.0
        private set
    var ballCurveX by mutableStateOf(0f) // Offset X for movement path
        private set
    var currentTimingResult by mutableStateOf<BallTimingResult>(BallTimingResult.Idle)
        private set
    var hasSwungThisBall by mutableStateOf(false)
        private set

    private var deliveryJob: Job? = null

    // Reset whole match
    fun startNewMatch() {
        resetMatchState()
    }

    private fun resetMatchState() {
        ballCount = 0
        matchScore = 0
        wicketsLost = 0
        isMatchFinished = false
        isBallActive = false
        ballProgress = 0f
        ballCurveX = 0f
        currentTimingResult = BallTimingResult.Idle
        hasSwungThisBall = false
        deliveryJob?.cancel()
    }

    // Start a delivery
    fun bowlBall() {
        if (isMatchFinished || isBallActive || ballCount >= 6 || wicketsLost >= 1) return

        isBallActive = true
        ballProgress = 0f
        ballCurveX = 0f
        hasSwungThisBall = false
        currentTimingResult = BallTimingResult.Idle

        deliveryJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val duration = selectedBowler.durationMs
            val peakSwingTime = duration * 0.7f // Swing curve peak

            while (System.currentTimeMillis() - startTime < duration) {
                if (hasSwungThisBall) break // stop advancing loop on swing!

                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                ballProgress = progress

                // Apply special curves based on bowlers
                if (selectedBowler.isSpin) {
                    // Sine wave wobble
                    ballCurveX = sin(progress * 12f) * 45f // wobbling side to side
                } else if (selectedBowler.isSwing) {
                    // Swing in sharply at the crease
                    if (elapsed > peakSwingTime) {
                        val lateFactor = (elapsed - peakSwingTime).toFloat() / (duration - peakSwingTime)
                        ballCurveX = -lateFactor * 75f // late swing inward
                    }
                }

                delay(16) // ~60 FPS
            }

            // Ball ended. If user did not swing at all
            if (!hasSwungThisBall && isBallActive) {
                ballProgress = 1.0f
                resolveNoSwing()
            }
        }
    }

    // Player swung the bat!
    fun swingBat() {
        if (!isBallActive || hasSwungThisBall) return
        hasSwungThisBall = true
        deliveryJob?.cancel()

        val progress = ballProgress
        val timingResult: BallTimingResult

        // Match contact Y region zones:
        // Best zone is 0.81 to 0.88. Perfect contact.
        // Good zone is 0.74 to 0.80, and 0.89 to 0.94.
        // Early is 0.60 to 0.73.
        // Late is 0.95 to 0.99.
        // Out of boundary (missed) is below 0.60 or above 0.99.

        if (progress in 0.81f..0.88f) {
            // Perfect hit! Chance of a massive sixer
            val runsChance = if (Random.nextFloat() > 0.3) 6 else 4
            val desc = if (runsChance == 6) "CRACK! AN ABSOLUTE MONSTER SIX!! 🚀" else "SUPERB TIMING! SMASHED TO THE BOUNDARY FOR FOUR! 🏏"
            timingResult = BallTimingResult.Hit(runsChance, desc, 0xFF4CAF50)
            matchScore += runsChance
        } else if (progress in 0.74f..0.80f) {
            timingResult = BallTimingResult.Hit(2, "GOOD TIMING! Guided past cover for 2 runs.", 0xFF8BC34A)
            matchScore += 2
        } else if (progress in 0.89f..0.94f) {
            timingResult = BallTimingResult.Hit(3, "WELL MET! Nice glance through fine-leg for 3 runs.", 0xFF8BC34A)
            matchScore += 3
        } else if (progress in 0.60f..0.73f) {
            // Early swing
            val runs = if (Random.nextBoolean()) 1 else 0
            val desc = if (runs == 1) "Slightly early! Pushed to mid-off for a quick single." else "Too early swing! Complete air shot."
            timingResult = if (runs > 0) {
                matchScore += runs
                BallTimingResult.Hit(runs, desc, 0xFFFFBC00)
            } else {
                BallTimingResult.Hit(0, desc, 0xFFFF5722)
            }
        } else if (progress in 0.95f..0.99f) {
            // Late swing
            val runs = if (Random.nextBoolean()) 1 else 0
            val desc = if (runs == 1) "Late contact! Thick edge runs down to third man for 1." else "Late swing! Beaten by pace."
            timingResult = if (runs > 0) {
                matchScore += runs
                BallTimingResult.Hit(runs, desc, 0xFFFFBC00)
            } else {
                BallTimingResult.Hit(0, desc, 0xFFFF5722)
            }
        } else {
            // Complete wild miss or ball had already crossed batsman! Clean bowled.
            timingResult = BallTimingResult.Missed
            wicketsLost += 1
        }

        currentTimingResult = timingResult
        ballCount += 1

        viewModelScope.launch {
            delay(1800) // Let user read commentary and feedback
            isBallActive = false
            checkMatchOverStates()
        }
    }

    private fun resolveNoSwing() {
        hasSwungThisBall = true
        currentTimingResult = BallTimingResult.Missed
        wicketsLost += 1
        ballCount += 1

        viewModelScope.launch {
            delay(1800)
            isBallActive = false
            checkMatchOverStates()
        }
    }

    private suspend fun checkMatchOverStates() {
        if (wicketsLost >= 1 || ballCount >= 6) {
            isMatchFinished = true
            // Save completed match performance to Room
            repository.recordMatch(
                runs = matchScore,
                ballsFaced = ballCount,
                multiplier = selectedBowler.multiplier,
                bowlerName = selectedBowler.name
            )
        }
    }

    // Withdrawal submission logic
    var isWithdrawalSuccess by mutableStateOf<Boolean?>(null)
        private set
    var isSubmittingWithdrawal by mutableStateOf(false)
        private set
    var withdrawalMessage by mutableStateOf("")
        private set

    fun resetWithdrawalStatus() {
        isWithdrawalSuccess = null
        withdrawalMessage = ""
    }

    fun submitWithdrawal(provider: String, phoneNum: String, nameTitle: String, amount: Double) {
        if (phoneNum.length < 11 || !phoneNum.startsWith("03") && !phoneNum.startsWith("+92")) {
            isWithdrawalSuccess = false
            withdrawalMessage = "Invalid Mobile Format! Must be 11 digits starting with 03xx (e.g. 03001234567)"
            return
        }
        if (nameTitle.trim().length < 3) {
            isWithdrawalSuccess = false
            withdrawalMessage = "Account Title too short! Must be at least 3 characters."
            return
        }
        if (amount < 1000.0) {
            isWithdrawalSuccess = false
            withdrawalMessage = "Minimum Withdrawal is 1,000 PKR!"
            return
        }
        val currentWallet = userWallet.value
        if (currentWallet.balance < amount) {
            isWithdrawalSuccess = false
            withdrawalMessage = "Insufficient PKR Wallet balance! You need ${amount - currentWallet.balance} PKR more."
            return
        }

        isSubmittingWithdrawal = true
        isWithdrawalSuccess = null

        viewModelScope.launch {
            delay(1200) // Realistic processing delay
            val ok = repository.requestWithdrawal(provider, phoneNum, nameTitle, amount)
            isSubmittingWithdrawal = false
            if (ok) {
                isWithdrawalSuccess = true
                withdrawalMessage = "Withdrawal receipt generated! Your $amount PKR transfer will arrive in a few moments."
                // Trigger an auto-approval loop for playability/realism!
                startAutoApprovalTimer()
            } else {
                isWithdrawalSuccess = false
                withdrawalMessage = "Failed to process withdrawal. Please double-check your PKR balance."
            }
        }
    }

    // Automatically change Pending requests to Approved in 10s so user sees the reward flowing!
    private fun startAutoApprovalTimer() {
        viewModelScope.launch {
            delay(10000) // Wait 10 seconds
            val pendings = withdrawalHistory.value.filter { it.status == "Pending" }
            for (p in pendings) {
                repository.approveWithdrawal(p.id)
            }
        }
    }

    // Fast-track mock approval button for the user (so they don't have to wait 10s if they want immediate verification)
    fun fastTrackApprovals() {
        viewModelScope.launch {
            val pendings = withdrawalHistory.value.filter { it.status == "Pending" }
            for (p in pendings) {
                repository.approveWithdrawal(p.id)
            }
        }
    }
}

class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
