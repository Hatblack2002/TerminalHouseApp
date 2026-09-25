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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiMessage
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BordesSutiles
import com.example.ui.theme.FondoMenuContextual
import com.example.ui.theme.FondoSheetIa
import com.example.ui.theme.FondoTarjetas
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextoAtenuado
import com.example.ui.theme.TextoPrimario
import com.example.ui.theme.TextoSecundario
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
 * Full AI Panel Screen (as depicted in "Panel IA" in Mockup Screens 2, 3, 6)
 */
@Composable
fun AiAgentFullPanel(
    messages: List<AiMessage>,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onExecuteCommandInTerminal: (String) -> Unit,
    modifier: Modifier = Modifier,
    contextText: String? = null,
    onClearContext: () -> Unit = {},
    onClose: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FondoSheetIa)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("ai_agent_full_panel")
    ) {
        // Header: 🤖 Agente IA · En línea · OpenAI · gpt-4o-mini · ⚙️
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Robot Icon squircle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FondoTarjetas)
                        .border(1.dp, BordesSutiles, RoundedCornerShape(10.dp)),
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

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Agente IA",
                            color = TextoPrimario,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StatusOnlineGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "En línea",
                            color = StatusOnlineGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "OpenAI · gpt-4o-mini",
                        color = TextoSecundario,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { /* Settings action */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Configuración del agente",
                        tint = TextoSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar panel IA",
                            tint = TextoSecundario,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Chat conversation area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // First Message Greeting
            item {
                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                    color = FondoMenuContextual,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "¡Hola! Estoy conectado a tu entorno Ubuntu. ¿En qué puedo ayudarte hoy?",
                            color = TextoPrimario,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "9:41",
                            color = TextoSecundario,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            // Quick suggestions when messages are few
            if (messages.size <= 1) {
                item {
                    Text(
                        text = "¿Qué quieres hacer hoy?",
                        color = TextoSecundario,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FondoTarjetas,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSendMessage("Muéstrame la versión de Ubuntu.") }
                        ) {
                            Text(
                                text = "Versión de Ubuntu",
                                color = TextoPrimario,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FondoTarjetas,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSendMessage("¿Cómo instalo htop?") }
                        ) {
                            Text(
                                text = "Instalar htop",
                                color = TextoPrimario,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Conversation history (User & Agent messages)
            items(messages.drop(1)) { msg ->
                AiMessageBubble(
                    message = msg,
                    onExecuteCommand = onExecuteCommandInTerminal
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Context chip from terminal selection (if any)
        if (contextText != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AccentOrange.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentOrange.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contexto: ${contextText.length} caracteres (solo la selección)",
                        color = AccentOrange,
                        fontSize = 11.5.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onClearContext, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Quitar contexto",
                            tint = AccentOrange,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Bottom input row: "¿Qué quieres hacer?" + circular orange send button ↑
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(FondoMenuContextual)
                .border(1.dp, BordesSutiles, RoundedCornerShape(24.dp))
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputValue,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_full_input_field"),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextoPrimario),
                cursorBrush = SolidColor(AccentOrange),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendMessage(null) }),
                decorationBox = { innerTextField ->
                    if (inputValue.isEmpty()) {
                        Text(
                            text = "¿Qué quieres hacer?",
                            color = TextoAtenuado,
                            fontSize = 13.5.sp
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Orange circular button with upward arrow ↑
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentOrange)
                    .clickable { onSendMessage(null) }
                    .testTag("ai_full_send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = "Enviar solicitud",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * v0.3.0 SECCIÓN 8 — contenido del bottom sheet del Agente IA (Mockup Screens 2 & 3).
 * Incluye drag handle centrado de 40x4 dp.
 */
@Composable
fun AiAgentSheetContent(
    messages: List<AiMessage>,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onExecuteCommandInTerminal: (String) -> Unit,
    contextText: String? = null,
    onClearContext: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FondoSheetIa)
    ) {
        // Drag handle del sheet (Mockup Screens 2 y 3: barra gris centrada 40x4 dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF5A5A5A))
            )
        }

        // Contenido completo del panel IA
        AiAgentFullPanel(
            messages = messages,
            inputValue = inputValue,
            onInputChange = onInputChange,
            onSendMessage = onSendMessage,
            onExecuteCommandInTerminal = onExecuteCommandInTerminal,
            contextText = contextText,
            onClearContext = onClearContext,
            onClose = onClose
        )
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
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                color = AccentOrange,
                modifier = Modifier
                    .padding(start = 40.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        lineHeight = 18.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "9:42",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "✓✓",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                color = FondoMenuContextual,
                border = androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles),
                modifier = Modifier
                    .padding(end = 40.dp)
                    .fillMaxWidth(0.92f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = TextoPrimario,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )

                    if (message.suggestedCommand != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF111116),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles),
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
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
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

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "9:41",
                        color = TextoSecundario,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
