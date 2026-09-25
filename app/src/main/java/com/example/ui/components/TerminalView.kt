package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LineType
import com.example.model.TerminalLine
import com.example.model.TerminalSession
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BordesSutiles
import com.example.ui.theme.Divisores
import com.example.ui.theme.FondoGlobal
import com.example.ui.theme.FondoHeader
import com.example.ui.theme.FondoMenuContextual
import com.example.ui.theme.FondoTarjetas
import com.example.ui.theme.FondoTerminal
import com.example.ui.theme.FondoToolbar
import com.example.ui.theme.PromptPath
import com.example.ui.theme.PromptRoot
import com.example.ui.theme.PromptUsuario
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.TerminalDark
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalBlue
import com.example.ui.theme.TerminalTextStyle
import com.example.ui.theme.TextoAtenuado
import com.example.ui.theme.TextoPrimario
import com.example.ui.theme.TextoSecundario
import com.example.viewmodel.CopyAction
import androidx.compose.ui.text.TextStyle

/**
 * v0.3.0 SECCIÓN 5 — área del terminal.
 *
 * Portrait: ocupa TODO el espacio disponible (sin márgenes laterales), fondo
 * #1E1E1E, texto #E0E0E0, cursor naranja parpadeante, sin bordes redondeados ni
 * decoraciones. Prompt visual root@terminalhouse:~# con colores reales.
 * La toolbar de 5 iconos (⌨️ 📋 📁 🤖 ⊞) vive DEBAJO del área del terminal (56 dp).
 *
 * Copiar (SECCIÓN 7): el botón 📋 abre el menú contextual granular; toque largo en
 * una línea selecciona (rango extensible tocando otra línea) con menú flotante;
 * toque largo sin selección pega el portapapeles al PTY tal cual (\n preservado).
 *
 * Modo interactivo (SECCIÓN 5, entrada tecla a tecla): cuando hay comando en curso
 * o el modo está activo, cada tecla va al PTY al instante y el panel "PTY en vivo"
 * muestra las últimas líneas reales del emulador vivo (reemplazo completo por
 * sondeo: imposible duplicar bytes en la UI).
 */
@OptIn(ExperimentalFoundationApi::class)
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
    modifier: Modifier = Modifier,
    terminalFontSizeSp: Float = 14f,
    liveTail: List<String> = emptyList(),
    showLivePanel: Boolean = false,
    isInteractiveMode: Boolean = false,
    isCommandRunning: Boolean = false,
    onToggleInteractive: () -> Unit = {},
    onHardwareControlKey: (Char) -> Boolean = { false },
    onPasteToPty: () -> Unit = {},
    provideCopyText: (CopyAction) -> String = { "" },
    copyLineCount: () -> Int = { 0 },
    onSendToAi: (String) -> Unit = {},
    onOpenAiSheet: () -> Unit = {},
    onOpenAiSheetExpanded: () -> Unit = {}
) {
    val activeSession = sessions.find { it.id == activeSessionId } ?: sessions.firstOrNull()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // ---- Menú contextual de copiado (SECCIÓN 7) ----
    var showCopyMenu by remember { mutableStateOf(false) }
    var showQuickMenu by remember { mutableStateOf(false) }
    var searchDialogOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchIndex by remember { mutableStateOf(0) }

    // Coincidencias de búsqueda en tiempo real (Mockup Screen 7)
    val searchMatches = remember(activeSession?.lines, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.lowercase()
            activeSession?.lines?.mapIndexedNotNull { idx, l ->
                if (l.text.lowercase().contains(q)) idx else null
            } ?: emptyList()
        }
    }

    // ---- Selección manual por líneas con menú flotante (SECCIÓN 7 / Mockup Screen 5) ----
    var selStart by remember { mutableStateOf<Int?>(null) }
    var selEnd by remember { mutableStateOf<Int?>(null) }
    val inSelectionMode = selStart != null || selEnd != null
    fun selectionRange(): IntRange? {
        val a = selStart ?: return null
        val b = selEnd ?: a
        return if (a <= b) a..b else b..a
    }
    fun selectedText(): String {
        val r = selectionRange() ?: return ""
        val lines = activeSession?.lines ?: return ""
        return lines.filterIndexed { i, _ -> i in r }.joinToString("\n") { it.text }
    }

    fun doCopy(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        val n = if (text.isEmpty()) 0 else text.count { it == '\n' } + 1
        Toast.makeText(context, "Copiado: $n líneas", Toast.LENGTH_SHORT).show()
    }

    fun doShare(text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir salida del terminal"))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo compartir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Guardar a archivo (SAF)
    var pendingSaveText by remember { mutableStateOf<String?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val text = pendingSaveText
        if (uri != null && text != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Guardado a archivo", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        pendingSaveText = null
    }

    // ---- Auto-scroll con excepción (SECCIÓN 5) ----
    var stickToBottom by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow {
            Pair(listState.firstVisibleItemIndex, listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index)
        }.collect { (_, lastVisible) ->
            val count = listState.layoutInfo.totalItemsCount
            stickToBottom = lastVisible == null || lastVisible >= count - 2
        }
    }
    LaunchedEffect(activeSession?.lines?.size, currentInput, searchIndex, searchQuery) {
        val lines = activeSession?.lines ?: return@LaunchedEffect
        if (searchMatches.isNotEmpty()) {
            val target = searchMatches.getOrElse(searchIndex % searchMatches.size) { searchMatches.first() }
            listState.scrollToItem(target)
            return@LaunchedEffect
        }
        if (stickToBottom && lines.isNotEmpty()) {
            listState.scrollToItem(lines.size) // item de entrada (prompt + cursor)
        }
    }

    val terminalStyle = TerminalTextStyle.copy(
        fontSize = terminalFontSizeSp.sp,
        lineHeight = (terminalFontSizeSp * 1.25f).sp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { e ->
                // Teclado físico (DeX/OTG): Ctrl+C/D/Z/L/A/E/U/W = bytes crudos al PTY
                if (e.type == KeyEventType.KeyDown && e.isCtrlPressed) {
                    val c = when (e.key) {
                        Key.C -> 'C'; Key.D -> 'D'; Key.Z -> 'Z'; Key.L -> 'L'
                        Key.A -> 'A'; Key.E -> 'E'; Key.U -> 'U'; Key.W -> 'W'
                        else -> null
                    }
                    c != null && onHardwareControlKey(c)
                } else {
                    false
                }
            }
            .testTag("terminal_view_container")
    ) {
        // ---- 2. FILA DE PESTAÑAS (Mockup Screens 1, 2, 4, 6) ----
        TerminalTabsRow(
            sessions = sessions,
            activeSessionId = activeSessionId,
            onSwitchSession = onSwitchSession,
            onAddNewSession = onAddNewSession,
            onCloseSession = onCloseSession
        )

        // ---- 3. ÁREA DEL TERMINAL: todo el alto disponible, sin decoraciones ----
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(FondoTerminal)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { if (!inSelectionMode) onPasteToPty() }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
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
                        val sRange = selectionRange()
                        val currentActiveMatchIndex = if (searchMatches.isNotEmpty()) {
                            searchMatches.getOrNull(searchIndex % searchMatches.size)
                        } else null

                        itemsIndexed(activeSession.lines) { index, line ->
                            val selected = sRange?.contains(index) == true
                            val isStart = sRange?.first == index
                            val isEnd = sRange?.last == index
                            val isCurrentMatch = currentActiveMatchIndex == index

                            TerminalLineItem(
                                line = line,
                                style = terminalStyle,
                                selected = selected,
                                isSelectionStart = isStart,
                                isSelectionEnd = isEnd,
                                searchQuery = if (searchQuery.isBlank()) null else searchQuery,
                                isCurrentSearchMatch = isCurrentMatch,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (inSelectionMode) {
                                                selEnd = index
                                            }
                                        },
                                        onLongClick = {
                                            selStart = index
                                            selEnd = index
                                        },
                                        onDoubleClick = {
                                            selStart = index
                                            selEnd = index
                                        }
                                    )
                            )
                        }

                        // Línea de entrada con prompt visual real + cursor naranja
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "root@terminalhouse",
                                    color = PromptUsuario,
                                    style = terminalStyle.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(":", color = PromptRoot, style = terminalStyle)
                                Text(
                                    text = activeSession.workingDir.ifBlank { "~" },
                                    color = PromptPath,
                                    style = terminalStyle
                                )
                                Text("# ", color = PromptRoot, style = terminalStyle)

                                BasicTextField(
                                    value = currentInput,
                                    onValueChange = onInputChange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("terminal_input_field"),
                                    textStyle = terminalStyle.copy(color = TextoPrimario),
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

                // ---- Vista en vivo del PTY (modo interactivo / comando en curso) ----
                if (showLivePanel) {
                    LivePtyPanel(
                        liveTail = liveTail,
                        isInteractive = isInteractiveMode,
                        isCommandRunning = isCommandRunning
                    )
                }
            }

            // Menú flotante sobre la selección (Mockup Screen 5)
            if (inSelectionMode) {
                Surface(
                    color = FondoMenuContextual,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copiar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { doCopy(selectedText()) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = TextoPrimario, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(2.dp))
                            Text("Copiar", color = TextoPrimario, fontSize = 11.sp)
                        }

                        // Compartir
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { doShare(selectedText()) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir", tint = TextoPrimario, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(2.dp))
                            Text("Compartir", color = TextoPrimario, fontSize = 11.sp)
                        }

                        // Enviar a Agente IA
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSendToAi(selectedText()) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = "Enviar a Agente IA", tint = AccentOrange, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(2.dp))
                            Text("Enviar a Agente IA", color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        // Cerrar
                        IconButton(onClick = { selStart = null; selEnd = null }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar selección", tint = TextoSecundario, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Barra de búsqueda encima del terminal (Mockup Screen 7)
            if (searchDialogOpen || searchQuery.isNotBlank()) {
                Surface(
                    color = FondoMenuContextual,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                searchIndex = 0
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = TextoPrimario, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(AccentOrange),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text("Buscar en el terminal...", color = TextoAtenuado, fontSize = 13.sp)
                                }
                                inner()
                            }
                        )

                        if (searchMatches.isNotEmpty()) {
                            Text(
                                text = "${(searchIndex % searchMatches.size) + 1}/${searchMatches.size}",
                                color = TextoSecundario,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        } else if (searchQuery.isNotBlank()) {
                            Text(
                                text = "0/0",
                                color = TextoAtenuado,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (searchMatches.isNotEmpty()) {
                                    searchIndex = if (searchIndex <= 0) searchMatches.size - 1 else searchIndex - 1
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Anterior", tint = TextoPrimario, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = {
                                if (searchMatches.isNotEmpty()) {
                                    searchIndex = (searchIndex + 1) % searchMatches.size
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Siguiente", tint = TextoPrimario, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = {
                                searchQuery = ""
                                searchDialogOpen = false
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda", tint = TextoSecundario, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Teclas extra (toggleable) — modo interactivo + teclas especiales
        AnimatedVisibility(visible = isKeyboardVisible) {
            ExtraKeysBar(
                onInsertKey = onInsertKey,
                isInteractiveMode = isInteractiveMode,
                onToggleInteractive = onToggleInteractive
            )
        }

        // ---- 4. TOOLBAR INFERIOR (56 dp): ⌨️ Teclado · 📋 Copiar · 📁 Archivos · 🤖 Agente IA · ⊞ Más ----
        TerminalBottomToolbar(
            isKeyboardVisible = isKeyboardVisible,
            onToggleKeyboard = onToggleKeyboard,
            onShowCopyMenu = { showCopyMenu = true },
            onOpenFileExplorer = onOpenFileExplorer,
            onOpenAiSheet = onOpenAiSheet,
            onOpenAiSheetExpanded = onOpenAiSheetExpanded,
            showQuickMenu = showQuickMenu,
            onToggleQuickMenu = { showQuickMenu = it },
            onQuickAction = onQuickAction,
            showCopyMenu = showCopyMenu,
            onDismissCopyMenu = { showCopyMenu = false },
            onCopyAction = { action ->
                doCopy(provideCopyText(action))
            },
            onCountLines = copyLineCount,
            onSearch = { searchDialogOpen = true },
            onSave = {
                pendingSaveText = provideCopyText(CopyAction.ALL)
                saveLauncher.launch("terminalhouse-terminal.txt")
            },
            onShare = { doShare(provideCopyText(CopyAction.ALL)) },
            onSendToAi = { onSendToAi(provideCopyText(CopyAction.LAST_CMD)) }
        )
    }

    // Diálogo de búsqueda (SECCIÓN 7)
    if (searchDialogOpen) {
        var queryDraft by remember(searchDialogOpen) { mutableStateOf(searchQuery) }
        AlertDialog(
            onDismissRequest = { searchDialogOpen = false },
            title = { Text("Buscar en el terminal", color = Color(0xFFE0E0E0)) },
            text = {
                OutlinedTextField(
                    value = queryDraft,
                    onValueChange = { queryDraft = it },
                    singleLine = true,
                    placeholder = { Text("Texto a buscar", color = Color(0xFF6E6E82)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    searchQuery = queryDraft
                    searchIndex = 0
                    searchDialogOpen = false
                }) { Text("Buscar", color = AccentOrange) }
            },
            dismissButton = {
                TextButton(onClick = { searchDialogOpen = false }) { Text("Cancelar", color = Color(0xFFA0A0B0)) }
            },
            containerColor = Color(0xFF1E1E26)
        )
    }
}

/**
 * Vista en vivo del PTY: las últimas líneas reales del transcript del emulador.
 * Se REEMPLAZA completa en cada sondeo (300 ms) — nunca se concatena ni se hace
 * diff de snapshots, así es estructuralmente imposible duplicar bloques.
 */
@Composable
private fun LivePtyPanel(
    liveTail: List<String>,
    isInteractive: Boolean,
    isCommandRunning: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(Color(0xFF111118))
            .border(1.dp, AccentOrange.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(StatusOnlineGreen)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "PTY en vivo",
                color = AccentOrange,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isCommandRunning) "comando en curso" else if (isInteractive) "interactivo" else "",
                color = StatusOnlineGreen,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(4.dp))
        liveTail.takeLast(6).forEach { line ->
            Text(
                text = line,
                color = Color(0xFFC8C8D4),
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Las teclas van directo al proceso • Ctrl+C interrumpe • Enter envía \\n",
            color = Color(0xFF6E6E82),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Fila de pestañas (SECCIÓN 4 / Mockup Screens 1, 2, 4, 6):
 * Altura 52dp, fondo #0F0F12. Pestaña activa naranja #E95420 con logo, nombre y ✕.
 * Botón + con forma redondeada 8dp y fondo #1A1A1E.
 */
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
            .height(52.dp)
            .background(FondoHeader)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sessions.forEach { session ->
            val isActive = session.id == activeSessionId

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isActive) AccentOrange else FondoTarjetas,
                border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
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
                        tint = if (isActive) Color.White else TextoSecundario,
                        modifier = Modifier.size(15.dp)
                    )

                    Text(
                        text = session.title,
                        color = if (isActive) Color.White else TextoSecundario,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                    )

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar pestaña",
                        tint = if (isActive) Color.White.copy(alpha = 0.85f) else TextoAtenuado,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { onCloseSession(session.id) }
                    )
                }
            }
        }

        // Botón + : reinicio limpio de la sesión (sin acumular pestañas)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = FondoTarjetas,
            border = androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles),
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onAddNewSession() }
                .testTag("add_tab_button")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nueva sesión",
                    tint = TextoSecundario,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Render de cada línea con selección visual + manejadores (Mockup Screen 5)
 * y resaltado de coincidencias de búsqueda (Mockup Screen 7).
 */
@Composable
private fun TerminalLineItem(
    line: TerminalLine,
    style: TextStyle,
    selected: Boolean,
    isSelectionStart: Boolean = false,
    isSelectionEnd: Boolean = false,
    searchQuery: String?,
    isCurrentSearchMatch: Boolean = false,
    modifier: Modifier = Modifier
) {
    val baseColor = when (line.type) {
        LineType.INPUT -> TextoPrimario
        LineType.ASCII_ART -> AccentOrange
        LineType.OUTPUT -> Color(0xFFC8C8D4)
        LineType.ERROR -> Color(0xFFEF4444)
        LineType.SYSTEM -> Color(0xFF3B82F6)
        LineType.PROMPT -> PromptUsuario
    }
    val fontWeight = when (line.type) {
        LineType.INPUT -> FontWeight.Medium
        LineType.SYSTEM -> FontWeight.Medium
        LineType.PROMPT -> FontWeight.Normal
        LineType.ASCII_ART -> FontWeight.Normal
        LineType.OUTPUT -> FontWeight.Normal
        LineType.ERROR -> FontWeight.Normal
    }

    val annotated: AnnotatedString = if (!searchQuery.isNullOrBlank()) {
        buildAnnotatedString {
            append(line.text)
            var from = 0
            val q = searchQuery.lowercase()
            val hay = line.text.lowercase()
            while (true) {
                val idx = hay.indexOf(q, from)
                if (idx < 0) break
                val bg = if (isCurrentSearchMatch) AccentOrange else AccentOrange.copy(alpha = 0.45f)
                addStyle(SpanStyle(background = bg, color = Color.White, fontWeight = FontWeight.Bold), idx, idx + q.length)
                from = idx + q.length
            }
        }
    } else {
        AnnotatedString(line.text)
    }

    Box(
        modifier = modifier
            .background(if (selected) AccentOrange.copy(alpha = 0.35f) else Color.Transparent)
    ) {
        Text(
            text = annotated,
            color = if (selected) Color.White else baseColor,
            style = style.copy(fontWeight = fontWeight),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
        )

        // Manejador de selección inicial (Mockup Screen 5: círculo naranja con borde blanco)
        if (selected && isSelectionStart) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(AccentOrange)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        // Manejador de selección final (Mockup Screen 5: círculo naranja con borde blanco)
        if (selected && isSelectionEnd) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(AccentOrange)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Teclas extra (SECCIÓN 5 — mapeo completo) + chip INTERACTIVO: activa el envío
 * tecla a tecla al PTY (obligatorio para [Y/n], read, vim, htop, python3 -i).
 */
@Composable
private fun ExtraKeysBar(
    onInsertKey: (String) -> Unit,
    isInteractiveMode: Boolean,
    onToggleInteractive: () -> Unit
) {
    val scrollState = rememberScrollState()
    val keys = listOf(
        "ESC", "TAB", "CTRL+C", "CTRL+Z", "CTRL+L", "CTRL+A", "CTRL+E", "CTRL+U", "CTRL+W",
        "|", "/", "-", "~", "UP", "DOWN", "LEFT", "RIGHT", "HOME", "END", "BS", "CLEAR"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chip de modo interactivo (entrada tecla a tecla)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isInteractiveMode) AccentOrange else Color(0xFF242430),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onToggleInteractive() }
                .testTag("interactive_mode_chip")
        ) {
            Text(
                text = if (isInteractiveMode) "● INTERACTIVO" else "○ INTERACTIVO",
                color = if (isInteractiveMode) Color.White else AccentOrange,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

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

/**
 * Toolbar inferior (SECCIÓN 6 / Mockup Screens 1, 4, 5, 6):
 * Altura 56dp, fondo #0F0F12, divisor superior #1F1F24.
 * 5 botones con icono y etiqueta:
 * ⌨️ Teclado · 📋 Copiar (menú contextual granular) · 📁 Archivos · 🤖 Agente IA (toque corto 45%, toque largo 100%) · ⊞ Más.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TerminalBottomToolbar(
    isKeyboardVisible: Boolean,
    onToggleKeyboard: () -> Unit,
    onShowCopyMenu: () -> Unit,
    onOpenFileExplorer: () -> Unit,
    onOpenAiSheet: () -> Unit,
    onOpenAiSheetExpanded: () -> Unit,
    showQuickMenu: Boolean,
    onToggleQuickMenu: (Boolean) -> Unit,
    onQuickAction: (String) -> Unit,
    showCopyMenu: Boolean,
    onDismissCopyMenu: () -> Unit,
    onCopyAction: (CopyAction) -> Unit,
    onCountLines: () -> Int,
    onSearch: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onSendToAi: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(FondoToolbar)
            .border(width = 1.dp, color = Divisores, shape = androidx.compose.ui.graphics.RectangleShape)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. ⌨️ Teclado
        ToolbarItem(
            icon = Icons.Default.Keyboard,
            label = "Teclado",
            isActive = isKeyboardVisible,
            onClick = onToggleKeyboard,
            testTag = "toggle_keyboard_button"
        )

        // 2. 📋 Copiar — abre el menú contextual granular (Mockup Screen 4)
        Box {
            ToolbarItem(
                icon = Icons.Default.ContentCopy,
                label = "Copiar",
                isActive = showCopyMenu,
                onClick = onShowCopyMenu,
                testTag = "copy_terminal_button"
            )

            DropdownMenu(
                expanded = showCopyMenu,
                onDismissRequest = onDismissCopyMenu,
                modifier = Modifier
                    .background(FondoMenuContextual)
                    .border(1.dp, BordesSutiles, RoundedCornerShape(12.dp))
            ) {
                // Cabecera: "¿Qué quieres copiar?" (Mockup Screen 4)
                Box(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
                ) {
                    Text(
                        text = "¿Qué quieres copiar?",
                        color = TextoSecundario,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                DropdownMenuItem(
                    text = { Text("📋  Todo el terminal", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onDismissCopyMenu(); onCopyAction(CopyAction.ALL) }
                )
                DropdownMenuItem(
                    text = { Text("📄  Últimas 50 líneas", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onDismissCopyMenu(); onCopyAction(CopyAction.LAST_50) }
                )
                DropdownMenuItem(
                    text = { Text("📃  Últimas 100 líneas", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onDismissCopyMenu(); onCopyAction(CopyAction.LAST_100) }
                )
                DropdownMenuItem(
                    text = { Text("⌨️   Último comando", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onDismissCopyMenu(); onCopyAction(CopyAction.LAST_CMD) }
                )
                DropdownMenuItem(
                    text = { Text("📤  Solo salida", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onDismissCopyMenu(); onCopyAction(CopyAction.LAST_OUTPUT) }
                )
                DropdownMenuItem(
                    text = { Text("▪   Última línea", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onDismissCopyMenu(); onCopyAction(CopyAction.LAST_LINE) }
                )
                HorizontalDivider(color = BordesSutiles, thickness = 1.dp)
                DropdownMenuItem(
                    text = { Text("🔍  Buscar en el terminal", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onDismissCopyMenu(); onSearch() }
                )
                DropdownMenuItem(
                    text = { Text("💾  Guardar en archivo", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onDismissCopyMenu(); onSave() }
                )
                DropdownMenuItem(
                    text = { Text("📤  Compartir", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onDismissCopyMenu(); onShare() }
                )
                DropdownMenuItem(
                    text = { Text("🤖  Enviar a Agente IA", color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    onClick = { onDismissCopyMenu(); onSendToAi() }
                )
            }
        }

        // 3. 📁 Archivos
        ToolbarItem(
            icon = Icons.Default.Folder,
            label = "Archivos",
            isActive = false,
            onClick = onOpenFileExplorer,
            testTag = "open_folder_button"
        )

        // 4. 🤖 Agente IA — toque corto: sheet al 45%; toque largo: sheet expandido
        ToolbarItem(
            icon = Icons.Default.SmartToy,
            label = "Agente IA",
            isActive = false,
            hasBadge = true,
            onClick = onOpenAiSheet,
            onLongClick = onOpenAiSheetExpanded,
            testTag = "open_ai_sheet_button"
        )

        // 5. ⊞ Más — acciones rápidas
        Box {
            ToolbarItem(
                icon = Icons.Default.GridView,
                label = "Más",
                isActive = showQuickMenu,
                onClick = { onToggleQuickMenu(true) },
                testTag = "quick_actions_button"
            )

            DropdownMenu(
                expanded = showQuickMenu,
                onDismissRequest = { onToggleQuickMenu(false) },
                modifier = Modifier
                    .background(FondoMenuContextual)
                    .border(1.dp, BordesSutiles, RoundedCornerShape(10.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("cat /etc/os-release", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onToggleQuickMenu(false); onQuickAction("cat /etc/os-release") }
                )
                DropdownMenuItem(
                    text = { Text("neofetch", color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    onClick = { onToggleQuickMenu(false); onQuickAction("neofetch") }
                )
                DropdownMenuItem(
                    text = { Text("uname -a", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onToggleQuickMenu(false); onQuickAction("uname -a") }
                )
                DropdownMenuItem(
                    text = { Text("free -h", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onToggleQuickMenu(false); onQuickAction("free -h") }
                )
                DropdownMenuItem(
                    text = { Text("ls -lah", color = TextoPrimario, fontSize = 13.sp) },
                    onClick = { onToggleQuickMenu(false); onQuickAction("ls -lah") }
                )
                HorizontalDivider(color = BordesSutiles, thickness = 1.dp)
                DropdownMenuItem(
                    text = { Text("clear", color = AccentOrange, fontSize = 13.sp) },
                    onClick = { onToggleQuickMenu(false); onQuickAction("clear") }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    hasBadge: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isActive) Modifier
                    .background(FondoMenuContextual)
                    .border(1.dp, AccentOrange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) AccentOrange else TextoSecundario,
                modifier = Modifier.size(20.dp)
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(AccentOrange)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isActive) AccentOrange else TextoSecundario,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}
