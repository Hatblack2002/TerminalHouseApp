package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.viewmodel.AppScreen

@Composable
fun DrawerContent(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    onCloseDrawer: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF14141A),
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .testTag("terminal_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_terminalhouse_logo),
                        contentDescription = "Logo",
                        tint = AccentOrange,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
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
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar menú",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation items list
            DrawerMenuItem(
                icon = Icons.Default.Terminal,
                title = "Terminal",
                isSelected = currentScreen == AppScreen.TERMINAL,
                onClick = { onSelectScreen(AppScreen.TERMINAL) }
            )

            DrawerMenuItem(
                icon = Icons.Default.SmartToy,
                title = "Agente IA",
                isSelected = currentScreen == AppScreen.IA,
                onClick = { onSelectScreen(AppScreen.IA) }
            )

            DrawerMenuItem(
                icon = Icons.Default.Folder,
                title = "Proyectos",
                isSelected = currentScreen == AppScreen.PROYECTOS,
                onClick = { onSelectScreen(AppScreen.PROYECTOS) }
            )

            DrawerMenuItem(
                icon = Icons.Default.FolderOpen,
                title = "Archivos",
                isSelected = currentScreen == AppScreen.ARCHIVOS,
                onClick = { onSelectScreen(AppScreen.ARCHIVOS) }
            )

            DrawerMenuItem(
                icon = Icons.Default.Inventory2,
                title = "Paquetes",
                isSelected = currentScreen == AppScreen.PAQUETES,
                onClick = { onSelectScreen(AppScreen.PAQUETES) }
            )

            DrawerMenuItem(
                icon = Icons.Default.Build,
                title = "Herramientas",
                isSelected = currentScreen == AppScreen.HERRAMIENTAS,
                onClick = { onSelectScreen(AppScreen.HERRAMIENTAS) }
            )

            DrawerMenuItem(
                icon = Icons.Default.Settings,
                title = "Ajustes",
                isSelected = currentScreen == AppScreen.AJUSTES,
                onClick = { onSelectScreen(AppScreen.AJUSTES) }
            )

            HorizontalDivider(
                color = Color(0xFF262634),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            DrawerMenuItem(
                icon = Icons.Default.Info,
                title = "Acerca de",
                isSelected = false,
                onClick = {
                    onCloseDrawer()
                    onOpenAbout()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer version
            Text(
                text = "TerminalHouse v0.1.0\nPotencia • Privacidad • Libertad",
                color = Color(0xFF6B6B7C),
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
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
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFFA0A0B0),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
