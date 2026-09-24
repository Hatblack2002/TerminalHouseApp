package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.R
import com.example.model.TerminalSession
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.viewmodel.AppScreen

@Composable
fun TabletDexLayout(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
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
    onOpenSystemDialog: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
            .testTag("tablet_dex_layout")
    ) {
        // Left Navigation Rail
        Surface(
            color = Color(0xFF14141A),
            modifier = Modifier
                .fillMaxHeight()
                .width(160.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NavRailItem(
                    icon = Icons.Default.Terminal,
                    label = "Terminal",
                    isSelected = currentScreen == AppScreen.TERMINAL,
                    onClick = { onSelectScreen(AppScreen.TERMINAL) }
                )
                NavRailItem(
                    icon = Icons.Default.SmartToy,
                    label = "Agente IA",
                    isSelected = currentScreen == AppScreen.IA,
                    onClick = { onSelectScreen(AppScreen.IA) }
                )
                NavRailItem(
                    icon = Icons.Default.Folder,
                    label = "Proyectos",
                    isSelected = currentScreen == AppScreen.PROYECTOS,
                    onClick = { onSelectScreen(AppScreen.PROYECTOS) }
                )
                NavRailItem(
                    icon = Icons.Default.FolderOpen,
                    label = "Archivos",
                    isSelected = currentScreen == AppScreen.ARCHIVOS,
                    onClick = { onSelectScreen(AppScreen.ARCHIVOS) }
                )
                NavRailItem(
                    icon = Icons.Default.Inventory2,
                    label = "Paquetes",
                    isSelected = currentScreen == AppScreen.PAQUETES,
                    onClick = { onSelectScreen(AppScreen.PAQUETES) }
                )
                NavRailItem(
                    icon = Icons.Default.Build,
                    label = "Herramientas",
                    isSelected = currentScreen == AppScreen.HERRAMIENTAS,
                    onClick = { onSelectScreen(AppScreen.HERRAMIENTAS) }
                )
                NavRailItem(
                    icon = Icons.Default.Settings,
                    label = "Ajustes",
                    isSelected = currentScreen == AppScreen.AJUSTES,
                    onClick = { onSelectScreen(AppScreen.AJUSTES) }
                )
                NavRailItem(
                    icon = Icons.Default.GridView,
                    label = "Más",
                    isSelected = currentScreen == AppScreen.MAS,
                    onClick = { onSelectScreen(AppScreen.MAS) }
                )
            }
        }

        // Center + Right Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_terminalhouse_logo),
                        contentDescription = "Logo",
                        tint = AccentOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row {
                            Text(
                                text = "Terminal",
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "House",
                                color = AccentOrange,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Ubuntu 24.04.5 LTS • ARM64",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E26))
                            .clickable { onOpenSystemDialog() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onOpenSystemDialog) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
                            tint = Color(0xFFA0A0B0),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { onSelectScreen(AppScreen.AJUSTES) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = Color(0xFFA0A0B0),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = Color(0xFFA0A0B0),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Main Body: Terminal (Center) + AI & Quick Actions (Right)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Terminal Area
                TerminalView(
                    sessions = sessions,
                    activeSessionId = activeSessionId,
                    currentInput = currentInput,
                    isKeyboardVisible = isKeyboardVisible,
                    onInputChange = onInputChange,
                    onSubmitCommand = onSubmitCommand,
                    onSwitchSession = onSwitchSession,
                    onAddNewSession = onAddNewSession,
                    onCloseSession = onCloseSession,
                    onToggleKeyboard = onToggleKeyboard,
                    onInsertKey = onInsertKey,
                    onQuickAction = onQuickAction,
                    onOpenFileExplorer = { onSelectScreen(AppScreen.ARCHIVOS) },
                    modifier = Modifier.weight(1.4f)
                )

                // Right Panel: Agente IA + Accesos rápidos
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Agente IA Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF16161D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262634)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = AccentOrange,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Agente IA",
                                        color = TextPrimaryDark,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Hola, soy tu agente IA. Puedo ayudarte a crear apps, instalar herramientas, ejecutar comandos y mucho más.",
                                color = Color(0xFFCBCBD8),
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            ElevatedButton(
                                onClick = { onSelectScreen(AppScreen.IA) },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = AccentOrange,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Iniciar chat", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Accesos rápidos Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF16161D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262634)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Accesos rápidos",
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickAccessTile(
                                        icon = Icons.Default.Code,
                                        label = "Crear proyecto",
                                        modifier = Modifier.weight(1f),
                                        onClick = { onSelectScreen(AppScreen.PROYECTOS) }
                                    )
                                    QuickAccessTile(
                                        icon = Icons.Default.Inventory2,
                                        label = "Instalar paquete",
                                        modifier = Modifier.weight(1f),
                                        onClick = { onSelectScreen(AppScreen.PAQUETES) }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickAccessTile(
                                        icon = Icons.Default.PlayArrow,
                                        label = "Ejecutar comando",
                                        modifier = Modifier.weight(1f),
                                        onClick = { onQuickAction("neofetch") }
                                    )
                                    QuickAccessTile(
                                        icon = Icons.Default.Folder,
                                        label = "Abrir carpeta",
                                        modifier = Modifier.weight(1f),
                                        onClick = { onSelectScreen(AppScreen.ARCHIVOS) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavRailItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) AccentOrange else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFFA0A0B0),
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun QuickAccessTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E1E28),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C3A)),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentOrange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = TextPrimaryDark,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
