package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.local.GameDatabase
import com.example.data.repository.GameRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BallTimingResult
import com.example.ui.viewmodel.Bowler
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.GameViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val database: GameDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            GameDatabase::class.java,
            "pkr_batting_game_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = database.gameDao()
        val repository = GameRepository(dao)
        val factory = GameViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    BattingGameAppScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun BattingGameAppScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wallet by viewModel.userWallet.collectAsState()
    val matches by viewModel.matchHistory.collectAsState()
    val withdrawals by viewModel.withdrawalHistory.collectAsState()

    var showWithdrawSheet by remember { mutableStateOf(false) }

    // Custom Sports styling palettes:
    // Pakistani Emerald theme
    val emeraldDark = Color(0xFF042013)
    val emeraldMedium = Color(0xFF083C25)
    val emeraldLight = Color(0xFF0F5B3A)
    val goldAccent = Color(0xFFFFA000)
    val goldLight = Color(0xFFFFD54F)

    // Main screen container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(emeraldDark, emeraldMedium)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // APP HEADER TITLE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SportsCricket,
                                contentDescription = "Cricket Icon",
                                tint = goldAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PKR BATTING GAME",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Text(
                            text = "Play cricket to earn simulated PKR cashout",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }

                    // Green Flag-colored Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0F5132))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PAK LEVEL",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // WALLET COUNTER BOX
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = emeraldLight),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2A7C57))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "AVAILABLE BALANCE",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₨ ${String.format("%,.2f", wallet.balance)} PKR",
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Dynamic rotating coin simulator
                            val infiniteTransition = rememberInfiniteTransition(label = "coin")
                            val rotationAngle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "coin_rotation"
                            )

                            Button(
                                onClick = { showWithdrawSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = goldAccent),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("withdraw_trigger_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Wallet",
                                    tint = emeraldDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "WITHDRAW",
                                    color = emeraldDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color(0xFF206D47)
                        )

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "HIGH SCORE",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${wallet.highScore} Runs",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            VerticalDivider()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "LIFETIME RUNS",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${wallet.lifetimeRuns}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            VerticalDivider()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "MATCHES",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${wallet.matchesPlayed} Innings",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // IMAGE HERO BANNER
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = emeraldMedium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Dynamic Image loading with static resource
                        Image(
                            painter = painterResource(id = R.drawable.cricket_hero_1782071794290),
                            contentDescription = "Pakistan stadium game cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )

                        // Vignette dark gradient overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xE0042013)),
                                        startY = 100f
                                    )
                                )
                        )

                        // Highlight Texts
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(goldAccent)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CHALLENGE MATCH",
                                    color = emeraldDark,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pace, Swing & Spin Roster Available!",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // BOWLER SELECTOR COCKPIT
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHOOSE YOUR OPPONENT BOWLER",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        if (viewModel.isBallActive) {
                            Text(
                                text = "MATCH ACTIVE",
                                color = Color.Red,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(viewModel.bowlers) { bowler ->
                            val isSelected = viewModel.selectedBowler.name == bowler.name
                            val cardBg = if (isSelected) emeraldLight else Color(0x33FFFFFF)
                            val borderColor = if (isSelected) goldAccent else Color.Transparent

                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable(enabled = !viewModel.isBallActive) {
                                        viewModel.selectBowler(bowler)
                                    }
                                    .border(2.dp, borderColor, RoundedCornerShape(10.dp)),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(bowler.color))
                                        )
                                        Text(
                                            text = bowler.style,
                                            color = Color.LightGray,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = bowler.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = bowler.speedLabel,
                                        color = goldLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${bowler.multiplier}x Multiplier",
                                        color = Color(0xFFA5D6A7),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.selectedBowler.description,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
            }

            // CRICKET ARENA - BATTING PITCH
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF06331E)),
                    border = BorderStroke(1.5.dp, Color(0xFF1E5E3D))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Current match score scoreboard
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF031E12))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TARGET OVERS MATCH",
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "SCORE: ",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${viewModel.matchScore}",
                                        color = goldAccent,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = " Runs",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Deliveries Tracker
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "BALLS",
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${viewModel.ballCount} / 6",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Wickets tracker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "WICKETS",
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${viewModel.wicketsLost} / 1",
                                    color = if (viewModel.wicketsLost == 1) Color.Red else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual cricket pitch canvas!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF1E5E3D), Color(0xFF0F3A22))
                                    )
                                )
                        ) {
                            val activeProgress = viewModel.ballProgress
                            val curveX = viewModel.ballCurveX

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Draw pitch line boundaries (perspective narrow lanes)
                                drawLine(
                                    color = Color(0x66FFFFFF),
                                    start = Offset(w * 0.35f, 0f),
                                    end = Offset(w * 0.15f, h),
                                    strokeWidth = 3f
                                )
                                drawLine(
                                    color = Color(0x66FFFFFF),
                                    start = Offset(w * 0.65f, 0f),
                                    end = Offset(w * 0.85f, h),
                                    strokeWidth = 3f
                                )

                                // Draw wickets crease at bottom (Y around h * 0.82)
                                drawLine(
                                    color = Color(0xCCFFFFFF),
                                    start = Offset(w * 0.15f, h * 0.82f),
                                    end = Offset(w * 0.85f, h * 0.82f),
                                    strokeWidth = 4f
                                )

                                // Draw sweet timing hit zone on canvas (Y = 0.81h to 0.88h)
                                drawRect(
                                    color = Color(0x3F00E676),
                                    topLeft = Offset(w * 0.17f, h * 0.81f),
                                    size = androidx.compose.ui.geometry.Size(w * 0.66f, h * 0.07f)
                                )

                                // Draw wickets at top (Bowlers end)
                                val bowlerCenter = w * 0.5f
                                drawRect(
                                    color = Color(0xFF795548),
                                    topLeft = Offset(bowlerCenter - 15f, 8f),
                                    size = androidx.compose.ui.geometry.Size(30f, 6f)
                                )
                                drawLine(Color.LightGray, Offset(bowlerCenter - 12f, 8f), Offset(bowlerCenter - 12f, 25f), 4f)
                                drawLine(Color.LightGray, Offset(bowlerCenter, 8f), Offset(bowlerCenter, 25f), 4f)
                                drawLine(Color.LightGray, Offset(bowlerCenter + 12f, 8f), Offset(bowlerCenter + 12f, 25f), 4f)

                                // Draw wickets at bottom (crease)
                                drawLine(Color.White, Offset(w * 0.5f - 18f, h * 0.82f), Offset(w * 0.5f - 18f, h * 0.82f + 25f), 4f)
                                drawLine(Color.White, Offset(w * 0.5f, h * 0.82f), Offset(w * 0.5f, h * 0.82f + 25f), 4f)
                                drawLine(Color.White, Offset(w * 0.5f + 18f, h * 0.82f), Offset(w * 0.5f + 18f, h * 0.82f + 25f), 4f)
                            }

                            // Dynamic Ball Drawing with 3D shadow zoom effect
                            if (viewModel.isBallActive) {
                                val ballRadius = (10f + activeProgress * 32f).dp
                                val ballY = (220f * activeProgress).dp
                                val ballXOffset = curveX.dp

                                // Shadow of the ball on ground
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(
                                            x = ballXOffset,
                                            y = ballY + (activeProgress * 6f).dp
                                        )
                                        .size(ballRadius * 0.9f)
                                        .clip(CircleShape)
                                        .background(Color(0x55000000))
                                )

                                // Real Cricket Red Leather ball
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(x = ballXOffset, y = ballY)
                                        .size(ballRadius)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(Color(0xFFFF5252), Color(0xFFB71C1C))
                                            )
                                        )
                                        .border(1.dp, Color.White, CircleShape)
                                )
                            }

                            // Swipe/Timing Feedback Commentary Indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 24.dp)
                            ) {
                                when (val result = viewModel.currentTimingResult) {
                                    is BallTimingResult.Hit -> {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xF20F3124)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(result.feedbackColor))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = result.description,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                                if (result.runs > 0) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Runs: +${result.runs} (Earned +₨ ${result.runs * 50 * viewModel.selectedBowler.multiplier} PKR)",
                                                        color = goldLight,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    is BallTimingResult.Missed -> {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xF2C62828)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "OUT! CLEAN BOWLED! 🔴",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        if (!viewModel.isBallActive) {
                                            if (viewModel.isMatchFinished) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xE60A1E15))
                                                        .padding(12.dp)
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = "🏆 INNINGS COMPLETED!",
                                                            color = goldLight,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "${viewModel.matchScore} Runs Scored against ${viewModel.selectedBowler.name}!",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Text(
                                                            text = "Earned +₨ ${viewModel.matchScore * 50 * viewModel.selectedBowler.multiplier} PKR",
                                                            color = Color.Green,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Button(
                                                            onClick = { viewModel.startNewMatch() },
                                                            colors = ButtonDefaults.buttonColors(containerColor = goldAccent),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text("PLAY NEXT OVER", color = emeraldDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = "Select Bowler then bowl to hit!",
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xCC000000))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls Area
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { viewModel.bowlBall() },
                                enabled = !viewModel.isBallActive && !viewModel.isMatchFinished && viewModel.wicketsLost < 1 && viewModel.ballCount < 6,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = emeraldDark,
                                    disabledContainerColor = Color(0x22FFFFFF)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("bowl_delivery_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Bowl")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("BOWL BALL", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.swingBat() },
                                enabled = viewModel.isBallActive && !viewModel.hasSwungThisBall,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = goldAccent,
                                    contentColor = emeraldDark,
                                    disabledContainerColor = Color(0x33FFA000)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("swing_bat_button")
                            ) {
                                Icon(Icons.Default.SportsCricket, contentDescription = "Swing", tint = emeraldDark)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SWING BAT!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // RECENT PLAYED INNINGS HISTORY
            item {
                Column {
                    Text(
                        text = "LAST MATCH HISTORIES (PAK ROSTER)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (matches.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.History, contentDescription = "History", tint = Color.LightGray, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No history recorded yet.\nPlay your first cricket over against Gully Uncle to build records!",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        matches.take(5).forEach { m ->
                            val readableTime = remember(m.timestamp) {
                                SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(m.timestamp))
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C3822)),
                                border = BorderStroke(0.5.dp, Color(0xFF1E5E3D)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = m.bowlerName,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$readableTime • ${m.ballsFaced} balls faced",
                                            color = Color.LightGray,
                                            fontSize = 9.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF042013))
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${m.runsScored} Runs",
                                                color = goldLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "+₨ ${m.pkrEarned.toInt()} PKR",
                                            color = Color.Green,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // WITHDRAWAL SHEET DIALOG SLIDER (EASYPAISA & JAZZCASH SPECIFIC)
        if (showWithdrawSheet) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.resetWithdrawalStatus()
                    showWithdrawSheet = false
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PKR CASH OUT PLATFORM",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            viewModel.resetWithdrawalStatus()
                            showWithdrawSheet = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                },
                text = {
                    var selectProvider by remember { mutableStateOf("JazzCash") } // Or "EasyPaisa"
                    var phoneValue by remember { mutableStateOf("") }
                    var accountNameValue by remember { mutableStateOf("") }
                    var amountValue by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Current balance representation
                        Card(
                            colors = CardDefaults.cardColors(containerColor = emeraldLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Available PKR Limit:", color = Color.White, fontSize = 11.sp)
                                Text("₨ ${String.format("%,.0f", wallet.balance)} PKR", color = goldLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // JAZZCASH & EASYPAISA SELECTOR TABS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // JazzCash Tab (Pakistan brand color Maroon-Orange/Green-ish)
                            val isJazz = selectProvider == "JazzCash"
                            val jazzBg = if (isJazz) Color(0xFFFF9800) else Color(0x33FFFFFF)
                            val jazzText = if (isJazz) Color.Black else Color.White
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(jazzBg)
                                    .clickable { selectProvider = "JazzCash" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MonetizationOn, "JazzCash", tint = jazzText, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("JazzCash", color = jazzText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // EasyPaisa Tab (Sky-Green/Blue brand color)
                            val isEasy = selectProvider == "EasyPaisa"
                            val easyBg = if (isEasy) Color(0xFF00E676) else Color(0x33FFFFFF)
                            val easyText = if (isEasy) Color.Black else Color.White
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(easyBg)
                                    .clickable { selectProvider = "EasyPaisa" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalance, "EasyPaisa", tint = easyText, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EasyPaisa", color = easyText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // MOBILE WALLET ACCOUNT FIELDS
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Phone input field
                            OutlinedTextField(
                                value = phoneValue,
                                onValueChange = { if (it.length <= 15) phoneValue = it },
                                label = { Text("Wallet Phone Number", color = Color.White) },
                                placeholder = { Text("e.g. 03001234567") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = goldAccent,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = goldAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedPlaceholderColor = Color.LightGray,
                                    unfocusedPlaceholderColor = Color.LightGray
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wallet_phone_input")
                            )

                            // Account Title Info
                            OutlinedTextField(
                                value = accountNameValue,
                                onValueChange = { accountNameValue = it },
                                label = { Text("Account Holder Title", color = Color.White) },
                                placeholder = { Text("e.g. Asif Ali") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = goldAccent,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = goldAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedPlaceholderColor = Color.LightGray,
                                    unfocusedPlaceholderColor = Color.LightGray
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wallet_title_input")
                            )

                            // Amount Field
                            OutlinedTextField(
                                value = amountValue,
                                onValueChange = { amountValue = it },
                                label = { Text("Amount to Withdraw (PKR)", color = Color.White) },
                                placeholder = { Text("Min 1,000 PKR") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = goldAccent,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = goldAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedPlaceholderColor = Color.LightGray,
                                    unfocusedPlaceholderColor = Color.LightGray
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wallet_amount_input")
                            )
                        }

                        // Submit with processing state
                        if (viewModel.isSubmittingWithdrawal) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = goldAccent)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val amt = amountValue.toDoubleOrNull() ?: 0.0
                                    viewModel.submitWithdrawal(
                                        provider = selectProvider,
                                        phoneNum = phoneValue,
                                        nameTitle = accountNameValue,
                                        amount = amt
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = goldAccent),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_withdraw_button")
                            ) {
                                Text("REQUEST PKR TRANSFER", color = emeraldDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Error or success messages
                        viewModel.isWithdrawalSuccess?.let { success ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (success) Color(0x3300E676) else Color(0x33FF5252))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = viewModel.withdrawalMessage,
                                    color = if (success) Color(0xFFB9F6CA) else Color(0xFFFF8A80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // PAST TRANSACTION RECIPIENTS SECTION
                        Divider(color = Color(0x40FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TRANSACTION HISTORY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            if (withdrawals.any { it.status == "Pending" }) {
                                Button(
                                    onClick = { viewModel.fastTrackApprovals() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Speed-up Settlement ⚡", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Box(modifier = Modifier.height(90.dp)) {
                            if (withdrawals.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No previous transactions recorded.", color = Color.LightGray, fontSize = 9.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(withdrawals) { w ->
                                        val statusColor = if (w.status == "Approved") Color.Green else goldLight
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF031E12))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(w.referenceId, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                Text("${w.provider} | ${w.accountNumber}", color = Color.LightGray, fontSize = 7.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("₨ ${w.amount.toInt()} PKR", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                Text(w.status, color = statusColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetWithdrawalStatus()
                        showWithdrawSheet = false
                    }) {
                        Text("DONE", color = goldAccent)
                    }
                },
                containerColor = emeraldDark,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color(0xFF206D47))
    )
}
