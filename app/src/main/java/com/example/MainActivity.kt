package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AboutDialog
import com.example.ui.components.AiAgentFullPanel
import com.example.ui.components.AiAgentHomeCard
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
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.TerminalHouseTheme
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.viewmodel.AppScreen
import com.example.viewmodel.TerminalUiState
import com.example.viewmodel.TerminalViewModel
import kotlinx.coroutines.launch

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

@Composable
fun TerminalHouseApp(
    uiState: TerminalUiState,
    viewModel: TerminalViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showOverflowMenu by remember { mutableStateOf(false) }

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
        val isTabletOrLandscape = maxWidth >= 600.dp

        if (isTabletOrLandscape) {
            // Horizontal / Tablet / DeX Mode
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
                onOpenAbout = { viewModel.toggleAboutDialog(true) }
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
                        .navigationBarsPadding(),
                    containerColor = if (uiState.isDarkMode) BackgroundDark else BackgroundLight,
                    topBar = {
                        // Official Top App Bar
                        PortraitTopBar(
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
                        // Official 4-Tab Bottom Navigation Bar
                        PortraitBottomNavBar(
                            currentScreen = uiState.currentScreen,
                            onSelectTab = { screen ->
                                if (screen == AppScreen.MAS) {
                                    scope.launch { drawerState.open() }
                                } else {
                                    viewModel.setScreen(screen)
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        when (uiState.currentScreen) {
                            AppScreen.TERMINAL -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Main Terminal view
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
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Compact Agente IA card at the bottom of portrait home
                                    AiAgentHomeCard(
                                        inputValue = uiState.aiInputText,
                                        onInputChange = { viewModel.onAiInputChange(it) },
                                        onSubmit = {
                                            viewModel.sendAiMessage()
                                            viewModel.setScreen(AppScreen.IA)
                                        },
                                        onExpand = { viewModel.setScreen(AppScreen.IA) }
                                    )
                                }
                            }
                            AppScreen.IA -> {
                                AiAgentFullPanel(
                                    messages = uiState.aiMessages,
                                    inputValue = uiState.aiInputText,
                                    onInputChange = { viewModel.onAiInputChange(it) },
                                    onSendMessage = { prompt -> viewModel.sendAiMessage(prompt) },
                                    onExecuteCommandInTerminal = { cmd ->
                                        viewModel.setScreen(AppScreen.TERMINAL)
                                        viewModel.executeQuickCommand(cmd)
                                    }
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
                                    onToggleTheme = { viewModel.toggleTheme() }
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
    }
}

@Composable
private fun PortraitTopBar(
    onOpenDrawer: () -> Unit,
    onOpenSystemDialog: () -> Unit,
    showOverflowMenu: Boolean,
    onToggleOverflowMenu: (Boolean) -> Unit,
    onClearScreen: () -> Unit,
    onNewSession: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenAbout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Hamburger menu + Brand Logo + Title + Subtitle
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("drawer_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Abrir menú",
                    tint = TextPrimaryDark,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Brand Logo
            Icon(
                painter = painterResource(id = R.drawable.ic_terminalhouse_logo),
                contentDescription = "Logo TerminalHouse",
                tint = AccentOrange,
                modifier = Modifier.size(30.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row {
                    Text(
                        text = "Terminal",
                        color = TextPrimaryDark,
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
                    text = "Ubuntu 24.04.5 LTS • ARM64",
                    color = TextSecondaryDark,
                    fontSize = 10.5.sp
                )
            }
        }

        // Right: Status indicator + Overflow Menu
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E26))
                    .clickable { onOpenSystemDialog() }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
                    .testTag("status_indicator_pill")
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StatusOnlineGreen)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "En línea",
                    color = StatusOnlineGreen,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box {
                IconButton(
                    onClick = { onToggleOverflowMenu(true) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("top_bar_overflow_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Más opciones",
                        tint = Color(0xFFA0A0B0),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { onToggleOverflowMenu(false) },
                    modifier = Modifier.background(Color(0xFF1E1E28))
                ) {
                    DropdownMenuItem(
                        text = { Text("Diagnóstico del sistema", color = TextPrimaryDark) },
                        onClick = {
                            onToggleOverflowMenu(false)
                            onOpenSystemDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nueva sesión de terminal", color = TextPrimaryDark) },
                        onClick = {
                            onToggleOverflowMenu(false)
                            onNewSession()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Limpiar terminal", color = TextPrimaryDark) },
                        onClick = {
                            onToggleOverflowMenu(false)
                            onClearScreen()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cambiar tema (Oscuro/Claro)", color = TextPrimaryDark) },
                        onClick = {
                            onToggleOverflowMenu(false)
                            onToggleTheme()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Acerca de TerminalHouse", color = AccentOrange) },
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

@Composable
private fun PortraitBottomNavBar(
    currentScreen: AppScreen,
    onSelectTab: (AppScreen) -> Unit
) {
    Surface(
        color = Color(0xFF141419),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
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
                icon = Icons.Default.AutoAwesome,
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
