package com.example

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AboutDialog
import com.example.ui.components.AiAgentSheetContent
import com.example.ui.components.DrawerContent
import com.example.ui.components.FilesScreen
import com.example.ui.components.PackagesScreen
import com.example.ui.components.ProjectsScreen
import com.example.ui.components.SettingsScreen
import com.example.ui.components.SplashScreenView
import com.example.ui.components.SystemInfoDialog
import com.example.ui.components.TabletDexLayout
import com.example.ui.components.TerminalView
import com.example.ui.components.ToolsScreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BordesSutiles
import com.example.ui.theme.ColorExito
import com.example.ui.theme.FondoHeader
import com.example.ui.theme.FondoMenuContextual
import com.example.ui.theme.FondoToolbar
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TerminalHouseTheme
import com.example.ui.theme.TextoPrimario
import com.example.ui.theme.TextoSecundario
import com.example.viewmodel.AppScreen
import com.example.viewmodel.TerminalUiState
import com.example.viewmodel.TerminalViewModel
import com.terminalhouse.ui.settings.AiAgentSettingsScreen
import kotlinx.coroutines.launch

import android.os.Bundle

class MainActivity : ComponentActivity() {

    private val viewModel: TerminalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            TerminalHouseTheme(darkTheme = uiState.isDarkMode) {
                Crossfade(
                    targetState = uiState.isSplashActive,
                    label = "splash_transition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreenView()
                    } else {
                        TerminalHouseApp(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TerminalHouseApp(
    uiState: TerminalUiState,
    viewModel: TerminalViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showOverflowMenu by remember { mutableStateOf(false) }
    // v0.3.0 SECCIÓN 3 — subtítulo dinámico real (recompone cuando cambia el bootstrap)
    val bootstrapStatus by viewModel.bootstrapStatus.collectAsStateWithLifecycle()

    // Sync drawer state with ViewModel
    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen && drawerState.isClosed) {
            drawerState.open()
        } else if (!uiState.isDrawerOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen != uiState.isDrawerOpen) {
            viewModel.toggleDrawer(drawerState.isOpen)
        }
    }

    BackHandler(enabled = drawerState.isOpen || uiState.currentScreen != AppScreen.TERMINAL) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.setScreen(AppScreen.TERMINAL)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // SECCIÓN 2 — landscape (orientación real) O pantallas anchas (plegables/Dex)
        val configuration = LocalConfiguration.current
        val isTabletOrLandscape = maxWidth >= 600.dp ||
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isTabletOrLandscape) {
            // Horizontal / Tablet / DeX Mode — terminal 70% + panel IA 30% (SECCIÓN 2)
            TabletDexLayout(
                currentScreen = uiState.currentScreen,
                onSelectScreen = { viewModel.setScreen(it) },
                sessions = uiState.sessions,
                activeSessionId = uiState.activeSessionId,
                currentInput = uiState.currentInput,
                isKeyboardVisible = uiState.isKeyboardVisible,
                onInputChange = { viewModel.onInputChange(it) },
                onSubmitCommand = { viewModel.submitCommand() },
                onSwitchSession = { viewModel.switchSession(it) },
                onAddNewSession = { viewModel.addNewSession() },
                onCloseSession = { viewModel.closeSession(it) },
                onToggleKeyboard = { viewModel.toggleKeyboard() },
                onInsertKey = { viewModel.insertSpecialKey(it) },
                onQuickAction = { viewModel.executeQuickCommand(it) },
                onOpenSystemDialog = { viewModel.toggleSystemDialog(true) },
                onOpenAbout = { viewModel.toggleAboutDialog(true) },
                subtitle = viewModel.dynamicSubtitle(bootstrapStatus),
                aiMessages = uiState.aiMessages,
                aiInputText = uiState.aiInputText,
                onAiInputChange = { viewModel.onAiInputChange(it) },
                onAiSendMessage = { viewModel.sendAiMessage(it) },
                onAiExecuteCommandInTerminal = { cmd ->
                    viewModel.setScreen(AppScreen.TERMINAL)
                    viewModel.executeQuickCommand(cmd)
                },
                aiContext = uiState.aiContext,
                onClearAiContext = { viewModel.clearAiContext() }
            )
        } else {
            // Portrait Phone Mode
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = Color(0xFF14141A)
                    ) {
                        DrawerContent(
                            currentScreen = uiState.currentScreen,
                            onSelectScreen = {
                                viewModel.setScreen(it)
                                scope.launch { drawerState.close() }
                            },
                            onCloseDrawer = { scope.launch { drawerState.close() } },
                            onOpenAbout = { viewModel.toggleAboutDialog(true) }
                        )
                    }
                }
            ) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        // v0.3.0 SECCIÓN 9 — el terminal se redimensiona con el teclado;
                        // nunca se tapa el cursor ni el input del sheet
                        .imePadding(),
                    containerColor = if (uiState.isDarkMode) BackgroundDark else BackgroundLight,
                    topBar = {
                        // Official Top App Bar — subtítulo dinámico real (SECCIÓN 3)
                        PortraitTopBar(
                            subtitle = viewModel.dynamicSubtitle(bootstrapStatus),
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenSystemDialog = { viewModel.toggleSystemDialog(true) },
                            showOverflowMenu = showOverflowMenu,
                            onToggleOverflowMenu = { showOverflowMenu = it },
                            onClearScreen = { viewModel.executeQuickCommand("clear") },
                            onNewSession = { viewModel.addNewSession() },
                            onToggleTheme = { viewModel.toggleTheme() },
                            onOpenAbout = { viewModel.toggleAboutDialog(true) }
                        )
                    },
                    bottomBar = {
                        // Official 4-Tab Bottom Navigation Bar — IA abre el sheet (SECCIÓN 10)
                        PortraitBottomNavBar(
                            currentScreen = uiState.currentScreen,
                            onSelectTab = { screen ->
                                when (screen) {
                                    AppScreen.MAS -> scope.launch { drawerState.open() }
                                    AppScreen.IA -> viewModel.showAiSheet(false)
                                    else -> viewModel.setScreen(screen)
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            // SECCIÓN 1: en TERMINAL el terminal toca los bordes de su
                            // contenedor (sin márgenes que desperdicien píxeles)
                            .then(
                                if (uiState.currentScreen == AppScreen.TERMINAL) Modifier
                                else Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                    ) {
                        when (uiState.currentScreen) {
                            AppScreen.TERMINAL -> {
                                // SECCIÓN 1: el terminal ocupa TODO el espacio entre las
                                // pestañas y la toolbar; sin panel IA fijo (PROHIBIDO portrait)
                                TerminalView(
                                    sessions = uiState.sessions,
                                    activeSessionId = uiState.activeSessionId,
                                    currentInput = uiState.currentInput,
                                    isKeyboardVisible = uiState.isKeyboardVisible,
                                    onInputChange = { viewModel.onInputChange(it) },
                                    onSubmitCommand = { viewModel.submitCommand() },
                                    onSwitchSession = { viewModel.switchSession(it) },
                                    onAddNewSession = { viewModel.addNewSession() },
                                    onCloseSession = { viewModel.closeSession(it) },
                                    onToggleKeyboard = { viewModel.toggleKeyboard() },
                                    onInsertKey = { viewModel.insertSpecialKey(it) },
                                    onQuickAction = { viewModel.executeQuickCommand(it) },
                                    onOpenFileExplorer = { viewModel.setScreen(AppScreen.ARCHIVOS) },
                                    terminalFontSizeSp = uiState.terminalFontSize,
                                    liveTail = uiState.liveTail,
                                    showLivePanel = uiState.isCommandRunning || uiState.isInteractiveMode,
                                    isInteractiveMode = uiState.isInteractiveMode,
                                    isCommandRunning = uiState.isCommandRunning,
                                    onToggleInteractive = { viewModel.setInteractiveMode(!uiState.isInteractiveMode) },
                                    onHardwareControlKey = { viewModel.onHardwareControlKey(it) },
                                    onPasteToPty = { viewModel.pasteClipboardToPty() },
                                    provideCopyText = { action -> viewModel.buildCopyText(action) },
                                    copyLineCount = { viewModel.copyLineCount() },
                                    onSendToAi = { viewModel.sendSelectionToAgent(it) },
                                    onOpenAiSheet = { viewModel.showAiSheet(false) },
                                    onOpenAiSheetExpanded = { viewModel.showAiSheet(true) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            AppScreen.IA -> {
                                // v0.3.0: el panel IA es un bottom sheet (SECCIÓN 8); la
                                // pestaña IA lo abre y el terminal permanece intacto debajo.
                            }
                            AppScreen.AGENTE_IA -> {
                                // v0.5.0: pantalla de configuración del Agente IA
                                // (proveedor, modelo y API key cifrada en el dispositivo).
                                AiAgentSettingsScreen(
                                    onBack = { viewModel.setScreen(AppScreen.TERMINAL) }
                                )
                            }
                            AppScreen.PROYECTOS -> {
                                ProjectsScreen(
                                    projects = uiState.projects,
                                    onCreateProject = {
                                        viewModel.setScreen(AppScreen.TERMINAL)
                                        viewModel.executeQuickCommand("mkdir -p ~/projects/nuevo-proyecto && cd ~/projects/nuevo-proyecto")
                                    },
                                    onOpenInTerminal = { cmd ->
                                        viewModel.setScreen(AppScreen.TERMINAL)
                                        viewModel.executeQuickCommand(cmd)
                                    }
                                )
                            }
                            AppScreen.ARCHIVOS -> {
                                FilesScreen(
                                    files = uiState.files,
                                    currentPath = uiState.currentFilePath,
                                    onOpenFileInTerminal = { cmd ->
                                        viewModel.setScreen(AppScreen.TERMINAL)
                                        viewModel.executeQuickCommand(cmd)
                                    }
                                )
                            }
                            AppScreen.PAQUETES -> {
                                PackagesScreen(
                                    packages = uiState.packages,
                                    onInstallPackage = { pkg ->
                                        viewModel.installPackage(pkg)
                                        viewModel.setScreen(AppScreen.TERMINAL)
                                    }
                                )
                            }
                            AppScreen.HERRAMIENTAS -> {
                                ToolsScreen(
                                    onRunTool = { cmd ->
                                        viewModel.setScreen(AppScreen.TERMINAL)
                                        viewModel.executeQuickCommand(cmd)
                                    }
                                )
                            }
                            AppScreen.AJUSTES -> {
                                SettingsScreen(
                                    isDarkMode = uiState.isDarkMode,
                                    onToggleTheme = { viewModel.toggleTheme() },
                                    terminalFontSize = uiState.terminalFontSize,
                                    onFontSizeChange = { viewModel.setTerminalFontSize(it) }
                                )
                            }
                            AppScreen.MAS -> {
                                // Handled by opening drawer
                            }
                            AppScreen.ACERCA_DE -> {
                                // Shown as dialog
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (uiState.showSystemDialog) {
            SystemInfoDialog(
                stats = uiState.systemStats,
                onDismiss = { viewModel.toggleSystemDialog(false) }
            )
        }

        if (uiState.showAboutDialog) {
            AboutDialog(
                onDismiss = { viewModel.toggleAboutDialog(false) }
            )
        }

        // v0.3.0 SECCIÓN 8 — Panel IA como bottom sheet (portrait). La sesión del
        // terminal SIGUE VIVA: vive en el ViewModel, el sheet no la toca. Tap fuera,
        // arrastrar hacia abajo o ✕ cierran; al cerrar el terminal vuelve intacto.
        if (!isTabletOrLandscape && uiState.showAiSheet) {
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = false,
                confirmValueChange = { true }
            )
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideAiSheet() },
                sheetState = sheetState,
                containerColor = SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(if (uiState.aiSheetExpanded) 0.9f else 0.45f)
                ) {
                    AiAgentSheetContent(
                        messages = uiState.aiMessages,
                        inputValue = uiState.aiInputText,
                        onInputChange = { viewModel.onAiInputChange(it) },
                        onSendMessage = { prompt -> viewModel.sendAiMessage(prompt) },
                        onExecuteCommandInTerminal = { cmd ->
                            viewModel.hideAiSheet()
                            viewModel.setScreen(AppScreen.TERMINAL)
                            viewModel.executeQuickCommand(cmd)
                        },
                        contextText = uiState.aiContext,
                        onClearContext = { viewModel.clearAiContext() },
                        onClose = { viewModel.hideAiSheet() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitTopBar(
    subtitle: String,
    onOpenDrawer: () -> Unit,
    onOpenSystemDialog: () -> Unit,
    showOverflowMenu: Boolean,
    onToggleOverflowMenu: (Boolean) -> Unit,
    onClearScreen: () -> Unit,
    onNewSession: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenAbout: () -> Unit
) {
    Surface(
        color = FondoHeader,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Hamburger menu + Brand Logo + Title + Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("drawer_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Abrir menú",
                        tint = TextoPrimario,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Brand Logo
                Icon(
                    painter = painterResource(id = R.drawable.ic_terminalhouse_logo),
                    contentDescription = "Logo TerminalHouse",
                    tint = AccentOrange,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row {
                        Text(
                            text = "Terminal",
                            color = TextoPrimario,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "House",
                            color = AccentOrange,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = subtitle,
                        color = TextoSecundario,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            // Right: Notification Bell + Settings Gear Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bell (Notificaciones / Diagnóstico)
                IconButton(
                    onClick = onOpenSystemDialog,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("top_bar_notifications_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones del sistema",
                            tint = TextoSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ColorExito)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                Box {
                    IconButton(
                        onClick = { onToggleOverflowMenu(true) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("top_bar_overflow_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes y opciones",
                            tint = TextoSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { onToggleOverflowMenu(false) },
                        modifier = Modifier
                            .background(FondoMenuContextual)
                            .border(1.dp, BordesSutiles, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Diagnóstico del sistema", color = TextoPrimario) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = AccentOrange) },
                            onClick = {
                                onToggleOverflowMenu(false)
                                onOpenSystemDialog()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Nueva sesión de terminal", color = TextoPrimario) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = TextoSecundario) },
                            onClick = {
                                onToggleOverflowMenu(false)
                                onNewSession()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Limpiar terminal", color = TextoPrimario) },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = TextoSecundario) },
                            onClick = {
                                onToggleOverflowMenu(false)
                                onClearScreen()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cambiar tema (Oscuro/Claro)", color = TextoPrimario) },
                            leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, tint = TextoSecundario) },
                            onClick = {
                                onToggleOverflowMenu(false)
                                onToggleTheme()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Acerca de TerminalHouse", color = AccentOrange) },
                            leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AccentOrange) },
                            onClick = {
                                onToggleOverflowMenu(false)
                                onOpenAbout()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortraitBottomNavBar(
    currentScreen: AppScreen,
    onSelectTab: (AppScreen) -> Unit
) {
    Surface(
        color = FondoToolbar,
        border = androidx.compose.foundation.BorderStroke(1.dp, BordesSutiles),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Terminal,
                label = "Terminal",
                isSelected = currentScreen == AppScreen.TERMINAL,
                onClick = { onSelectTab(AppScreen.TERMINAL) },
                testTag = "nav_tab_terminal"
            )

            BottomNavItem(
                icon = Icons.Default.SmartToy,
                label = "IA",
                isSelected = currentScreen == AppScreen.IA,
                onClick = { onSelectTab(AppScreen.IA) },
                testTag = "nav_tab_ia"
            )

            BottomNavItem(
                icon = Icons.Default.Folder,
                label = "Proyectos",
                isSelected = currentScreen == AppScreen.PROYECTOS,
                onClick = { onSelectTab(AppScreen.PROYECTOS) },
                testTag = "nav_tab_proyectos"
            )

            BottomNavItem(
                icon = Icons.Default.GridView,
                label = "Más",
                isSelected = currentScreen == AppScreen.MAS,
                onClick = { onSelectTab(AppScreen.MAS) },
                testTag = "nav_tab_mas"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            // Orange active highlight
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentOrange.copy(alpha = 0.2f))
                    .border(1.dp, AccentOrange, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = AccentOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFA0A0B0),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            color = if (isSelected) AccentOrange else Color(0xFFA0A0B0),
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
