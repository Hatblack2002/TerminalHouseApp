package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AiMessage
import com.example.model.FileItem
import com.example.model.LineType
import com.example.model.PackageItem
import com.example.model.ProjectItem
import com.example.model.SystemStats
import com.example.model.TerminalLine
import com.example.model.TerminalSession
import com.example.service.AiAgentService
import com.example.service.LinuxBootstrap
import com.example.service.PtyBridge
import com.example.service.SystemMonitor
import com.example.service.TerminalEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class AppScreen {
    TERMINAL,
    IA,
    PROYECTOS,
    MAS,
    ARCHIVOS,
    PAQUETES,
    HERRAMIENTAS,
    AJUSTES,
    ACERCA_DE
}

data class TerminalUiState(
    val sessions: List<TerminalSession> = emptyList(),
    val activeSessionId: String = "",
    val currentInput: String = "",
    val isKeyboardVisible: Boolean = false,
    val showSystemDialog: Boolean = false,
    val showAboutDialog: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val currentScreen: AppScreen = AppScreen.TERMINAL,
    val systemStats: SystemStats? = null,
    val aiMessages: List<AiMessage> = emptyList(),
    val aiInputText: String = "",
    val isDarkMode: Boolean = true,
    val projects: List<ProjectItem> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val currentFilePath: String = "/root",
    val packages: List<PackageItem> = emptyList(),
    val isSplashActive: Boolean = true
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    init {
        initSession()
        startStatsMonitor()
        startBootstrap()
        dismissSplashAfterDelay()
    }

    private fun dismissSplashAfterDelay() {
        viewModelScope.launch {
            delay(1200)
            _uiState.update { it.copy(isSplashActive = false) }
        }
    }

    private fun app() = getApplication<Application>()

    // ------------------------------------------------------------------
    // Bootstrap REAL del rootfs (descarga + SHA256 + extracción + verificación)
    // ------------------------------------------------------------------
    private fun startBootstrap() {
        viewModelScope.launch {
            appendToActive(TerminalLine("[bootstrap] Iniciando entorno Ubuntu real (arm64/armhf según dispositivo).", LineType.SYSTEM))
            val job = launch {
                LinuxBootstrap.status.collect { st ->
                    val line = when (st) {
                        is LinuxBootstrap.Status.Downloading -> {
                            val pct = if (st.totalBytes > 0) " (${st.downloadedBytes * 100 / st.totalBytes}%)" else ""
                            "[bootstrap] Descargando ${st.downloadedBytes / (1024 * 1024)} MB${pct} de ${st.source}"
                        }
                        is LinuxBootstrap.Status.VerifyingHash -> "[bootstrap] Verificando SHA256 del rootfs..."
                        is LinuxBootstrap.Status.Extracting -> "[bootstrap] Extrayendo rootfs (${st.processed} entradas, última: ${st.lastPath})"
                        is LinuxBootstrap.Status.Hardening -> "[bootstrap] Configurando DNS y servicios del rootfs..."
                        is LinuxBootstrap.Status.Checking -> "[bootstrap] Verificando estructura (bash/apt/python3/os-release/root)..."
                        else -> null
                    }
                    if (line != null) appendToActive(TerminalLine(line, LineType.SYSTEM))
                }
            }
            val result = LinuxBootstrap.ensure(app())
            job.cancel()
            when (result) {
                is LinuxBootstrap.Status.Ready -> {
                    appendToActive(TerminalLine("[bootstrap] SHA256 verificado y rootfs completo.", LineType.SYSTEM))
                    appendToActive(TerminalLine("[bootstrap] SO real: ${result.info.osPrettyName} • ${result.info.dpkgPackages} paquetes dpkg • ${result.info.fileCount} archivos • ${result.info.symlinkCount} symlinks", LineType.SYSTEM))
                    appendToActive(TerminalLine("[bootstrap] PRoot: ${PtyBridge.prootFile(app()).absolutePath}", LineType.SYSTEM))
                    appendToActive(TerminalLine("[bootstrap] Lanzando /bin/bash --login via PRoot sobre PTY real...", LineType.SYSTEM))
                    refreshRealData()
                    val s = PtyBridge.ensureSession(activeSessionId(), app())
                    if (s == null) {
                        appendToActive(TerminalLine("[motor] No se pudo iniciar PRoot. Revisa diagnósticos.", LineType.ERROR))
                    } else {
                        appendToActive(TerminalLine("[motor] Sesión activa (pid=${s.getPid()}). Escribe un comando real.", LineType.SYSTEM))
                    }
                }
                is LinuxBootstrap.Status.Failed -> {
                    appendToActive(TerminalLine("[bootstrap] FALLÓ: ${result.reason}", LineType.ERROR))
                    appendToActive(TerminalLine("[bootstrap] El entorno NO está disponible. Reintenta con: bootstrap", LineType.ERROR))
                }
                else -> Unit
            }
        }
    }

    private fun refreshRealData() {
        viewModelScope.launch {
            // Paquetes reales desde dpkg status
            _uiState.update { it.copy(packages = LinuxBootstrap.loadInstalledPackages(app())) }
            // Archivos reales de /root
            _uiState.update { it.copy(files = realRootFiles(), currentFilePath = "/root") }
            // Proyectos reales: directorios dentro de /root
            val rootDir = File(LinuxBootstrap.rootfsDir(app()), "root")
            val proys = rootDir.listFiles { f -> f.isDirectory }
                ?.take(20)
                ?.map { ProjectItem(UUID.randomUUID().toString(), it.name, "directorio real en /root", "/root/${it.name}", "N/D", "N/D") }
                ?: emptyList()
            _uiState.update { it.copy(projects = proys) }
        }
    }

    private fun realRootFiles(): List<FileItem> {
        val rootDir = File(LinuxBootstrap.rootfsDir(app()), "root")
        return rootDir.listFiles()
            ?.take(50)
            ?.map { f ->
                FileItem(
                    name = f.name,
                    path = "/root/${f.name}",
                    isDirectory = f.isDirectory,
                    size = if (f.isFile) humanSize(f.length()) else "N/D",
                    permissions = realPermissions(f)
                )
            } ?: emptyList()
    }

    private fun humanSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }

    private fun realPermissions(f: File): String {
        val r = if (f.canRead()) "r" else "-"
        val w = if (f.canWrite()) "w" else "-"
        val x = if (f.canExecute()) "x" else "-"
        return "${if (f.isDirectory) "d" else "-"}$r$w$x$r$w$x$r$w$x"
    }

    // ------------------------------------------------------------------
    // Sesión inicial (líneas reales de arranque, sin neofetch falso)
    // ------------------------------------------------------------------
    private fun initSession() {
        val initialLines = TerminalEngine.createInitialSessionLines(app())
        val defaultSession = TerminalSession(
            id = "term-1",
            title = "Terminal 1",
            lines = initialLines,
            workingDir = "~"
        )
        _uiState.update {
            it.copy(
                sessions = listOf(defaultSession),
                activeSessionId = defaultSession.id,
                systemStats = SystemMonitor.getRealStats(app()),
                aiMessages = listOf(AiAgentService.getInitialGreeting())
            )
        }
    }

    private fun startStatsMonitor() {
        viewModelScope.launch {
            while (isActive) {
                val stats = SystemMonitor.getRealStats(app())
                _uiState.update { it.copy(systemStats = stats) }
                delay(4000)
            }
        }
    }

    private fun activeSessionId(): String = _uiState.value.activeSessionId

    private fun activeSession(): TerminalSession? =
        _uiState.value.sessions.find { it.id == _uiState.value.activeSessionId }

    private fun appendToActive(line: TerminalLine) {
        val s = activeSession() ?: return
        updateSession(s.copy(lines = s.lines + line))
    }

    fun onInputChange(newInput: String) {
        _uiState.update { it.copy(currentInput = newInput) }
    }

    fun submitCommand() {
        val input = _uiState.value.currentInput.trim()
        val activeId = activeSessionId()
        val session = activeSession() ?: return

        _uiState.update { it.copy(currentInput = "") }
        if (input.isEmpty()) return

        viewModelScope.launch {
            val updatedHistory = if (!session.history.contains(input)) session.history + input else session.history
            updateSession(session.copy(history = updatedHistory))

            val result = TerminalEngine.executeCommand(input, session.workingDir, app(), activeId)

            val current = _uiState.value.sessions.find { it.id == activeId } ?: return@launch
            val finalLines = if (result.shouldClear) emptyList() else current.lines + result.lines
            updateSession(current.copy(lines = finalLines))
        }
    }

    fun executeQuickCommand(command: String) {
        _uiState.update { it.copy(currentInput = command) }
        submitCommand()
    }

    fun addNewSession() {
        val count = _uiState.value.sessions.size + 1
        val newSessionId = "term-${UUID.randomUUID().toString().take(6)}"
        val newSession = TerminalSession(
            id = newSessionId,
            title = "Terminal $count",
            lines = listOf(
                TerminalLine("[TerminalHouse] Nueva sesión PTY real (PRoot + Ubuntu).", LineType.SYSTEM)
            ),
            workingDir = "~"
        )
        _uiState.update {
            it.copy(
                sessions = it.sessions + newSession,
                activeSessionId = newSessionId
            )
        }
        viewModelScope.launch {
            PtyBridge.ensureSession(newSessionId, app())
        }
    }

    fun switchSession(sessionId: String) {
        _uiState.update { it.copy(activeSessionId = sessionId) }
    }

    fun closeSession(sessionId: String) {
        val currentSessions = _uiState.value.sessions
        if (currentSessions.size <= 1) {
            // No cerrar la única sesión: reiniciar su vista y su shell
            PtyBridge.closeSession(sessionId)
            val reset = currentSessions.first().copy(
                lines = TerminalEngine.createInitialSessionLines(app()),
                workingDir = "~",
                history = emptyList(),
                historyIndex = -1
            )
            updateSession(reset)
            viewModelScope.launch { PtyBridge.ensureSession(sessionId, app()) }
            return
        }

        PtyBridge.closeSession(sessionId)
        val remaining = currentSessions.filter { it.id != sessionId }
        val newActiveId = if (_uiState.value.activeSessionId == sessionId) {
            remaining.last().id
        } else {
            _uiState.value.activeSessionId
        }
        _uiState.update {
            it.copy(sessions = remaining, activeSessionId = newActiveId)
        }
    }

    private fun updateSession(updated: TerminalSession) {
        _uiState.update { state ->
            state.copy(sessions = state.sessions.map { if (it.id == updated.id) updated else it })
        }
    }

    fun toggleKeyboard() {
        _uiState.update { it.copy(isKeyboardVisible = !it.isKeyboardVisible) }
    }

    /**
     * Teclas de control: se escriben CRUDAS al PTY (bytes reales), como un terminal real.
     * Ctrl+C interrumpe el proceso en primer plano del rootfs; flechas recorren el
     * historial de bash real; TAB completa en el shell real.
     */
    fun insertSpecialKey(keyText: String) {
        when (keyText) {
            "CTRL+C" -> {
                PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x03))
                _uiState.update { it.copy(currentInput = "") }
            }
            "CTRL+D" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x04))
            "ESC" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x1B))
            "TAB" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x09))
            "UP" -> PtyBridge.writeRaw(activeSessionId(), "\u001B[A".toByteArray(Charsets.UTF_8))
            "DOWN" -> PtyBridge.writeRaw(activeSessionId(), "\u001B[B".toByteArray(Charsets.UTF_8))
            "CLEAR" -> executeQuickCommand("clear")
            else -> _uiState.update { it.copy(currentInput = _uiState.value.currentInput + keyText) }
        }
    }

    fun toggleSystemDialog(show: Boolean) {
        _uiState.update { it.copy(showSystemDialog = show) }
    }

    fun toggleAboutDialog(show: Boolean) {
        _uiState.update { it.copy(showAboutDialog = show) }
    }

    fun toggleDrawer(open: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = open) }
    }

    fun setScreen(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, isDrawerOpen = false) }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun onAiInputChange(text: String) {
        _uiState.update { it.copy(aiInputText = text) }
    }

    fun sendAiMessage(promptText: String? = null) {
        val textToSend = promptText ?: _uiState.value.aiInputText.trim()
        if (textToSend.isEmpty()) return

        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = textToSend
        )
        _uiState.update {
            it.copy(aiMessages = it.aiMessages + userMessage, aiInputText = "")
        }
        viewModelScope.launch {
            delay(300)
            val response = AiAgentService.processQuery(textToSend)
            _uiState.update { it.copy(aiMessages = it.aiMessages + response) }
        }
    }

    fun installPackage(pkg: PackageItem) {
        // NO se marca "instalado" por adelantado: el estado real lo decide apt.
        // El comando se ejecuta de verdad dentro del rootfs vía PTY.
        executeQuickCommand("apt install -y ${pkg.name}")
    }
}
