package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import com.example.service.SystemMonitor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDark

@Composable
fun SplashScreenView(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDark,
                        Color(0xFF180D07),
                        Color(0xFF241208),
                        BackgroundDark
                    )
                )
            )
            .testTag("splash_screen_view"),
        contentAlignment = Alignment.Center
    ) {
        // Glowing orange aura behind logo
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pulseScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentOrange.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // House Logo with glowing orange
            Icon(
                painter = painterResource(id = R.drawable.ic_terminalhouse_logo),
                contentDescription = "TerminalHouse Logo",
                tint = AccentOrange,
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Brand Title: Terminal in White, House in Orange
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Terminal",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "House",
                    color = AccentOrange,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = SystemMonitor.identitySummaryCached(),
                color = Color(0xFFA0A0B0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Circular Orange Spinner
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = AccentOrange,
                strokeWidth = 3.dp,
                trackColor = Color(0xFF282834)
            )
        }
    }
}
