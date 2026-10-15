package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.CounterLog
import com.example.data.CounterRepository
import com.example.ui.CounterViewModel
import com.example.ui.CounterViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val db = AppDatabase.getDatabase(LocalContext.current)
                CounterApp(database = db)
            }
        }
    }
}

@Composable
fun CounterApp(database: AppDatabase) {
    val repository = remember(database) { CounterRepository(database.counterDao()) }
    val factory = remember(repository) { CounterViewModelFactory(repository) }
    val viewModel: CounterViewModel = viewModel(factory = factory)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1016))
    ) { innerPadding ->
        CounterScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CounterScreen(
    viewModel: CounterViewModel,
    modifier: Modifier = Modifier
) {
    val count by viewModel.counterValue.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    // Keep track of previous count to determine sliding direction
    var previousCount by remember { mutableStateOf(count) }
    val isIncrement = count > previousCount

    LaunchedEffect(count) {
        previousCount = count
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E0F15),
                        Color(0xFF141624)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Title
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.counter_title),
                color = Color(0xFF00E5FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Rotating/Scaling Counter Dial
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(230.dp)
                    .background(
                        color = Color(0xFF1C1E2D).copy(alpha = 0.8f),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally)
            ) {
                // Outer subtle ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .background(Color.Transparent, shape = CircleShape)
                )

                // Precise visual dial reading
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(
                        targetState = count,
                        transitionSpec = {
                            if (targetState == 0 && initialState != 0) {
                                // Default reset transition (elegant cross-fade & scale)
                                (fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.8f))
                                    .togetherWith(fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 0.8f))
                            } else if (isIncrement) {
                                (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)) { it } + fadeIn())
                                    .togetherWith(slideOutVertically { -it } + fadeOut())
                            } else {
                                (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)) { -it } + fadeIn())
                                    .togetherWith(slideOutVertically { it } + fadeOut())
                            }.using(SizeTransform(clip = false))
                        },
                        label = "CounterRollingAnim"
                    ) { targetCount ->
                        Text(
                            text = targetCount.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = Color.White
                            ),
                            modifier = Modifier.testTag("counter_value_text")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Tactical Control Action Button Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minus Button
                val minusInteraction = remember { MutableInteractionSource() }
                val isMinusPressed by minusInteraction.collectIsPressedAsState()
                val minusScale by animateFloatAsState(
                    targetValue = if (isMinusPressed) 0.88f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                    label = "MinusScale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(minusScale)
                        .background(
                            color = Color(0xFF23263B),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = minusInteraction,
                            indication = LocalIndication.current
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.decrement()
                        }
                        .testTag("minus_button")
                ) {
                    // Custom drawn horizontal line representing the minus symbol
                    Box(
                        modifier = Modifier
                            .size(16.dp, 3.dp)
                            .background(Color(0xFFFF5252), shape = RoundedCornerShape(2.dp))
                    )
                }

                // Reset Button
                val resetInteraction = remember { MutableInteractionSource() }
                val isResetPressed by resetInteraction.collectIsPressedAsState()
                val resetScale by animateFloatAsState(
                    targetValue = if (isResetPressed) 0.88f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                    label = "ResetScale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .scale(resetScale)
                        .background(
                            color = Color(0xFF23263B).copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = resetInteraction,
                            indication = LocalIndication.current
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.reset()
                        }
                        .testTag("reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.reset_label),
                        tint = Color(0xFFFFD740),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Plus Button
                val plusInteraction = remember { MutableInteractionSource() }
                val isPlusPressed by plusInteraction.collectIsPressedAsState()
                val plusScale by animateFloatAsState(
                    targetValue = if (isPlusPressed) 0.88f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                    label = "PlusScale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .scale(plusScale)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF00B0FF))
                            ),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = plusInteraction,
                            indication = LocalIndication.current
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.increment()
                        }
                        .testTag("add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.increment_label),
                        tint = Color(0xFF0E0F15),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Reactive Activity Log Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = Color(0xFF161928),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.recent_activity),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (logs.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.empty_logs_text),
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            LogItem(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogItem(log: CounterLog) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeString = remember(log.timestamp) { formatter.format(Date(log.timestamp)) }

    val iconColor = when (log.changeType) {
        "INCREMENT" -> Color(0xFF00E5FF)
        "DECREMENT" -> Color(0xFFFF5252)
        else -> Color(0xFFFFD740)
    }

    val iconChar = when (log.changeType) {
        "INCREMENT" -> "▲"
        "DECREMENT" -> "▼"
        else -> "↺"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E2134), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape)
            ) {
                Text(
                    text = iconChar,
                    color = iconColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (log.changeType) {
                    "INCREMENT" -> "Added 1"
                    "DECREMENT" -> "Subtracted 1"
                    else -> "Reset counter"
                },
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Count: ${log.countAfterChange}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = timeString,
                color = Color(0xFF6B7280),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
