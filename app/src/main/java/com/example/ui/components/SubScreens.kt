package com.example.ui.components

import androidx.compose.foundation.background
import com.example.service.SystemMonitor
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FileItem
import com.example.model.PackageItem
import com.example.model.ProjectItem
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun ProjectsScreen(
    projects: List<ProjectItem>,
    onCreateProject: () -> Unit,
    onOpenInTerminal: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("projects_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Proyectos",
                    color = TextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Espacio de trabajo local en Ubuntu",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }

            ElevatedButton(
                onClick = onCreateProject,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = AccentOrange,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Nuevo", fontSize = 12.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(projects) { project ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A1A24),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282836)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentOrange.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = project.name,
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = project.description,
                                    color = TextSecondaryDark,
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${project.language} • ${project.lastModified}",
                                    color = Color(0xFF8E8EA0),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { onOpenInTerminal("cd ${project.path} && ls -la") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Abrir en terminal",
                                tint = AccentOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilesScreen(
    files: List<FileItem>,
    currentPath: String,
    onOpenFileInTerminal: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("files_screen")
    ) {
        Column {
            Text(
                text = "Archivos",
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ruta actual: $currentPath",
                color = AccentOrange,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(files) { file ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1A1A22),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262634)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (file.isDirectory) {
                                onOpenFileInTerminal("cd ${file.path} && ls -lah")
                            } else {
                                onOpenFileInTerminal("cat ${file.path}")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (file.isDirectory) AccentOrange else Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                color = TextPrimaryDark,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${file.permissions} • ${file.size}",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PackagesScreen(
    packages: List<PackageItem>,
    onInstallPackage: (PackageItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("packages_screen")
    ) {
        Column {
            Text(
                text = "Paquetes (apt/dpkg)",
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Gestor de paquetes de Ubuntu Noble 24.04",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(packages) { pkg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1A1A24),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282834)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = pkg.name,
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "v${pkg.version}",
                                    color = AccentOrange,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = pkg.description,
                                color = TextSecondaryDark,
                                fontSize = 11.5.sp
                            )
                        }

                        if (pkg.isInstalled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Instalado",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            ElevatedButton(
                                onClick = { onInstallPackage(pkg) },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = AccentOrange,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(text = "Instalar", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsScreen(
    onRunTool: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("tools_screen")
    ) {
        Column {
            Text(
                text = "Herramientas del Sistema",
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Diagnóstico y utilidades rápidas",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val toolItems = listOf(
            Triple("Información del sistema", "Muestra la distribución real del rootfs", "cat /etc/os-release"),
            Triple("Uso de Memoria (free)", "Inspecciona memoria libre y búferes", "free -h"),
            Triple("Kernel y Arquitectura (uname)", "Detalles del kernel Linux y plataforma", "uname -a"),
            Triple("Espacio en Disco (df)", "Espacio libre en rootfs y particiones", "df -h"),
            Triple("Lista de Procesos (ps)", "Procesos activos del entorno de terminal", "ps aux"),
            Triple("Prueba de Red (ping)", "Comprueba conectividad saliente a internet", "ping -c 3 8.8.8.8")
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(toolItems) { (name, desc, cmd) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A1A24),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282836)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRunTool(cmd) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = desc,
                                color = TextSecondaryDark,
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$ $cmd",
                                color = AccentOrange,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Ejecutar",
                            tint = AccentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Ajustes",
            color = TextPrimaryDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configuración del entorno TerminalHouse",
            color = TextSecondaryDark,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1A1A24),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282836)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tema Oscuro / Claro",
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isDarkMode) "Fondo #0F0F12 activado" else "Fondo claro activado",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentOrange,
                            checkedTrackColor = AccentOrange.copy(alpha = 0.4f)
                        )
                    )
                }

                HorizontalDivider(
                    color = Color(0xFF262634),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Shell Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Shell Predeterminada",
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "/bin/bash (${if (SystemMonitor.lastRootfsReady) SystemMonitor.lastOsPrettyName else "no verificado"})",
                            color = AccentOrange,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFF262634),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Architecture Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Arquitectura de Ejecución",
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "aarch64 (ARM 64-bit)",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_terminalhouse_logo),
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row {
                    Text(
                        text = "Terminal",
                        color = TextPrimaryDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "House",
                        color = AccentOrange,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Tu entorno Linux en Android",
                    color = TextSecondaryDark,
                    fontSize = 12.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Potencia • Privacidad • Libertad\nVersión 0.1.0",
                    color = Color(0xFFA0A0B0),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                ElevatedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = AccentOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Entendido")
                }
            }
        }
    }
}
