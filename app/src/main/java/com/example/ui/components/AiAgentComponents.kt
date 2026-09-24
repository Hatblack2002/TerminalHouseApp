package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiMessage
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

/**
 * Compact AI Agent Card shown on the bottom of Portrait Terminal Home screen
 */
@Composable
fun AiAgentHomeCard(
    inputValue: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF16161B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262632)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpand() }
            .testTag("ai_agent_home_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header: Robot icon, "Agente IA", Green dot "En línea"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Robot Icon in squircle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF22222C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Agente IA",
                        tint = AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Agente IA",
                    color = TextPrimaryDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Green dot + En línea
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StatusOnlineGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "En línea",
                    color = StatusOnlineGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Tu asistente para crear, instalar y automatizar apps.",
                color = TextSecondaryDark,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(start = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Input Row: dark rounded field + orange submit button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1F1F28))
                    .border(1.dp, Color(0xFF2E2E3C), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputValue,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_home_input_field"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimaryDark),
                    cursorBrush = SolidColor(AccentOrange),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                    decorationBox = { innerTextField ->
                        if (inputValue.isEmpty()) {
                            Text(
                                text = "¿Qué quieres hacer hoy?",
                                color = TextMutedDark,
                                fontSize = 13.5.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Orange circular submit button >
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentOrange)
                        .clickable { onSubmit() }
                        .testTag("ai_home_submit_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full AI Panel Screen (as depicted in "Panel IA (en modo portrait)")
 */
@Composable
fun AiAgentFullPanel(
    messages: List<AiMessage>,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onExecuteCommandInTerminal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_agent_full_panel")
    ) {
        // Header: Agente IA, Green dot "En línea"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF22222C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Agente IA",
                        color = TextPrimaryDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(StatusOnlineGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "En línea",
                        color = StatusOnlineGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat & Action Options Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // First Message Speech Bubble
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF181820),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262632)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Hola, soy tu agente IA. Puedo ayudarte a crear apps, instalar herramientas, ejecutar comandos, analizar archivos y mucho más.",
                        color = Color(0xFFD4D4E0),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Section: "¿Qué quieres hacer hoy?"
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "¿Qué quieres hacer hoy?",
                        color = AccentOrange,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 4 Action Buttons
            item {
                AiActionOptionCard(
                    icon = Icons.Default.Android,
                    iconBgColor = Color(0xFF10B981),
                    title = "Crear una app nativa de Android",
                    onClick = { onSendMessage("Crear una app nativa de Android") }
                )
            }

            item {
                AiActionOptionCard(
                    icon = Icons.Default.Widgets,
                    iconBgColor = AccentOrange,
                    title = "Instalar una herramienta",
                    onClick = { onSendMessage("Instalar una herramienta") }
                )
            }

            item {
                AiActionOptionCard(
                    icon = Icons.Default.Terminal,
                    iconBgColor = Color(0xFF3B82F6),
                    title = "Ejecutar un comando",
                    onClick = { onSendMessage("Ejecutar un comando") }
                )
            }

            item {
                AiActionOptionCard(
                    icon = Icons.Default.Description,
                    iconBgColor = Color(0xFFA855F7),
                    title = "Analizar un archivo",
                    onClick = { onSendMessage("Analizar un archivo") }
                )
            }

            // Additional user & agent messages
            items(messages.drop(1)) { msg ->
                AiMessageBubble(
                    message = msg,
                    onExecuteCommand = onExecuteCommandInTerminal
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom input row: >_ icon + "Escribe tu solicitud..." + orange > button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF191922))
                .border(1.dp, Color(0xFF2B2B38), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // >_ icon
            Text(
                text = ">_",
                color = AccentOrange,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(10.dp))

            BasicTextField(
                value = inputValue,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_full_input_field"),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimaryDark),
                cursorBrush = SolidColor(AccentOrange),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendMessage(null) }),
                decorationBox = { innerTextField ->
                    if (inputValue.isEmpty()) {
                        Text(
                            text = "Escribe tu solicitud...",
                            color = TextMutedDark,
                            fontSize = 13.5.sp
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Orange rounded button >
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentOrange)
                    .clickable { onSendMessage(null) }
                    .testTag("ai_full_send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Enviar solicitud",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AiActionOptionCard(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1A1A22),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282834)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(iconBgColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBgColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF6B6B7C),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiMessage,
    onExecuteCommand: (String) -> Unit
) {
    if (message.isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentOrange.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentOrange.copy(alpha = 0.5f)),
                modifier = Modifier.padding(start = 32.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF181820),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262632)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = Color(0xFFD4D4E0),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )

                if (message.suggestedCommand != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF111116),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = message.suggestedCommand,
                                color = Color(0xFF22C55E),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentOrange)
                                    .clickable { onExecuteCommand(message.suggestedCommand) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Ejecutar",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
