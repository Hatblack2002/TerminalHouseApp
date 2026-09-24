package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.SystemStats
import com.example.service.LinuxBootstrap
import com.example.service.PtyBridge
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemInfoDialog(
    stats: SystemStats?,
    onDismiss: () -> Unit
) {
    if (stats == null) return

    val context = LocalContext.current

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("system_info_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF14141A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2B38)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header with gear icon, title, and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sistema",
                            color = TextPrimaryDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(Android host + Entorno Ubuntu)",
                            color = TextSecondaryDark,
                            fontSize = 11.5.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 1: Android (host)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1C1C24),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282834)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = "Android host",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Android (host)",
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // CPU
                        StatRow(
                            label = "CPU",
                            value = "${stats.cpuUsagePercent}%",
                            progress = stats.cpuUsagePercent / 100f,
                            showProgress = false
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // RAM
                        StatRow(
                            label = "RAM",
                            value = "${stats.ramUsedPercent}%",
                            progress = stats.ramUsedPercent / 100f,
                            showProgress = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Almacenamiento
                        StatRow(
                            label = "Almacenamiento",
                            value = "${stats.storageUsedPercent}%",
                            progress = stats.storageUsedPercent / 100f,
                            showProgress = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 2: Entorno Ubuntu (guest)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1C1C24),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282834)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentOrange.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_terminalhouse_logo),
                                    contentDescription = "Entorno Ubuntu",
                                    tint = AccentOrange,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Entorno Ubuntu (guest)",
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        KeyValueRow(key = "SO rootfs", value = stats.osVersion)
                        KeyValueRow(key = "Arquitectura", value = stats.architecture)
                        KeyValueRow(key = "Rootfs", value = "${stats.rootfsSizeMb} MB • ${LinuxBootstrap.metrics(context).fileCount} archivos")
                        KeyValueRow(key = "Procesos", value = "${stats.processCount}")
                        KeyValueRow(key = "PID app", value = "${stats.pid}")
                        Spacer(modifier = Modifier.height(10.dp))
                        // Diagnósticos REALES del motor (Fase 9): sin valores inventados
                        PtyBridge.diagnostics(context, "term-1").forEach { (k, v) ->
                            KeyValueRow(key = k, value = if (v.length > 160) v.take(160) + "…" else v)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    progress: Float,
    showProgress: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextSecondaryDark,
                fontSize = 13.sp
            )
            Text(
                text = value,
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (showProgress) {
            Spacer(modifier = Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = AccentOrange,
                trackColor = Color(0xFF282834),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun KeyValueRow(
    key: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            color = TextSecondaryDark,
            fontSize = 12.5.sp
        )
        Text(
            text = value,
            color = TextPrimaryDark,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
