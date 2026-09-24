package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LineType
import com.example.model.TerminalLine
import com.example.model.TerminalSession
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.TerminalDark
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalTextStyle

@Composable
fun TerminalView(
    sessions: List<TerminalSession>,
    activeSessionId: String,
    currentInput: String,
    isKeyboardVisible: Boolean,
    onInputChange: (String) -> Unit,
    onSubmitCommand: () -> Unit,
    onSwitchSession: (String) -> Unit,
    onAddNewSession: () -> Unit,
    onCloseSession: (String) -> Unit,
    onToggleKeyboard: () -> Unit,
    onInsertKey: (String) -> Unit,
    onQuickAction: (String) -> Unit,
    onOpenFileExplorer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSession = sessions.find { it.id == activeSessionId } ?: sessions.firstOrNull()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var showQuickMenu by remember { mutableStateOf(false) }

    // Auto-scroll to bottom on new output
    LaunchedEffect(activeSession?.lines?.size, currentInput) {
        if (activeSession != null && activeSession.lines.isNotEmpty()) {
            listState.animateScrollToItem(activeSession.lines.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("terminal_view_container")
    ) {
        // Tab Bar
        TerminalTabsRow(
            sessions = sessions,
            activeSessionId = activeSessionId,
            onSwitchSession = onSwitchSession,
            onAddNewSession = onAddNewSession,
            onCloseSession = onCloseSession
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Main Terminal Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TerminalDark)
                .border(1.dp, Color(0xFF282834), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Terminal Output Area
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("terminal_lines_list")
                ) {
                    if (activeSession != null) {
                        items(activeSession.lines) { line ->
                            TerminalLineItem(line = line)
                        }

                        // Current Input Line with Prompt and Blinking Cursor
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\u276F ",
                                    color = TerminalGreen,
                                    style = TerminalTextStyle.copy(fontWeight = FontWeight.Bold)
                                )

                                BasicTextField(
                                    value = currentInput,
                                    onValueChange = onInputChange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("terminal_input_field"),
                                    textStyle = TerminalTextStyle.copy(color = Color(0xFFE0E0E0)),
                                    cursorBrush = SolidColor(AccentOrange),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Send,
                                        autoCorrectEnabled = false
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = { onSubmitCommand() }
                                    )
                                )
                            }
                        }
                    }
                }

                // In-Terminal Bottom Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Keyboard toggle icon
                    IconButton(
                        onClick = onToggleKeyboard,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_keyboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Alternar teclado",
                            tint = if (isKeyboardVisible) AccentOrange else Color(0xFFA0A0B0),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Right group: Copy, Folder, Grid
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy button
                        IconButton(
                            onClick = {
                                val fullText = activeSession?.lines?.joinToString("\n") { it.text } ?: ""
                                clipboardManager.setText(AnnotatedString(fullText))
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_terminal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar salida",
                                tint = Color(0xFFA0A0B0),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Folder button
                        IconButton(
                            onClick = onOpenFileExplorer,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("open_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Archivos",
                                tint = Color(0xFFA0A0B0),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Grid quick actions button
                        Box {
                            IconButton(
                                onClick = { showQuickMenu = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("quick_actions_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Acciones rápidas",
                                    tint = Color(0xFFA0A0B0),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showQuickMenu,
                                onDismissRequest = { showQuickMenu = false },
                                modifier = Modifier.background(Color(0xFF1E1E26))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("cat /etc/os-release", color = Color(0xFFE0E0E0)) },
                                    onClick = {
                                        showQuickMenu = false
                                        onQuickAction("cat /etc/os-release")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("uname -a", color = Color(0xFFE0E0E0)) },
                                    onClick = {
                                        showQuickMenu = false
                                        onQuickAction("uname -a")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("free -h", color = Color(0xFFE0E0E0)) },
                                    onClick = {
                                        showQuickMenu = false
                                        onQuickAction("free -h")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ls -lah", color = Color(0xFFE0E0E0)) },
                                    onClick = {
                                        showQuickMenu = false
                                        onQuickAction("ls -lah")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("clear", color = AccentOrange) },
                                    onClick = {
                                        showQuickMenu = false
                                        onQuickAction("clear")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Extra Keys Toolbar (toggleable with keyboard button)
        AnimatedVisibility(visible = isKeyboardVisible) {
            ExtraKeysBar(onInsertKey = onInsertKey)
        }
    }
}

@Composable
private fun TerminalTabsRow(
    sessions: List<TerminalSession>,
    activeSessionId: String,
    onSwitchSession: (String) -> Unit,
    onAddNewSession: () -> Unit,
    onCloseSession: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sessions.forEach { session ->
            val isActive = session.id == activeSessionId

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isActive) AccentOrange else Color(0xFF1C1C24),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSwitchSession(session.id) }
                    .testTag("tab_${session.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_terminalhouse_logo),
                        contentDescription = null,
                        tint = if (isActive) Color.White else Color(0xFFA0A0B0),
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = session.title,
                        color = if (isActive) Color.White else Color(0xFFA0A0B0),
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar pestaña",
                        tint = if (isActive) Color.White.copy(alpha = 0.8f) else Color(0xFF707080),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onCloseSession(session.id) }
                    )
                }
            }
        }

        // Add new session button
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1C1C24),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onAddNewSession() }
                .testTag("add_tab_button")
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nueva pestaña",
                    tint = Color(0xFFA0A0B0),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TerminalLineItem(line: TerminalLine) {
    when (line.type) {
        LineType.INPUT -> {
            Text(
                text = line.text,
                color = Color(0xFFE0E0E0),
                style = TerminalTextStyle.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        LineType.ASCII_ART -> {
            Text(
                text = line.text,
                color = AccentOrange,
                style = TerminalTextStyle
            )
        }
        LineType.OUTPUT -> {
            Text(
                text = line.text,
                color = Color(0xFFC8C8D4),
                style = TerminalTextStyle,
                modifier = Modifier.padding(vertical = 0.5.dp)
            )
        }
        LineType.ERROR -> {
            Text(
                text = line.text,
                color = Color(0xFFEF4444),
                style = TerminalTextStyle,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        LineType.SYSTEM -> {
            Text(
                text = line.text,
                color = Color(0xFF3B82F6),
                style = TerminalTextStyle.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        LineType.PROMPT -> {
            Text(
                text = line.text,
                color = TerminalGreen,
                style = TerminalTextStyle
            )
        }
    }
}

@Composable
private fun ExtraKeysBar(onInsertKey: (String) -> Unit) {
    val scrollState = rememberScrollState()
    val keys = listOf("ESC", "TAB", "CTRL+C", "|", "/", "-", "~", "UP", "DOWN", "CLEAR")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { key ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF242430),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onInsertKey(key) }
            ) {
                Text(
                    text = key,
                    color = if (key.startsWith("CTRL") || key == "ESC") AccentOrange else Color(0xFFD0D0E0),
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
