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
        loadInitialData()
        startStatsMonitor()
        dismissSplashAfterDelay()
    }

    private fun dismissSplashAfterDelay() {
        viewModelScope.launch {
            delay(1200)
            _uiState.update { it.copy(isSplashActive = false) }
        }
    }

    private fun initSession() {
        val initialLines = TerminalEngine.createInitialSessionLines(getApplication())
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
                systemStats = SystemMonitor.getRealStats(getApplication()),
                aiMessages = listOf(AiAgentService.getInitialGreeting())
            )
        }
    }

    private fun loadInitialData() {
        val defaultProjects = listOf(
            ProjectItem("1", "android-terminal-app", "Entorno nativo Kotlin + Compose", "/root/projects/android-terminal-app", "Kotlin", "Hoy 12:15"),
            ProjectItem("2", "ubuntu-rootfs-builder", "Scripts de configuración de rootfs Noble", "/root/projects/ubuntu-rootfs-builder", "Shell", "Ayer 18:40"),
            ProjectItem("3", "python-ai-agent", "Microservicio de automatización local", "/root/projects/python-ai-agent", "Python", "Hace 2 días")
        )

        val defaultPackages = listOf(
            PackageItem("bash", "5.2.21", "GNU Bourne Again SHell", true, "1.4 MB"),
            PackageItem("coreutils", "9.4-3ubuntu6", "GNU core utilities (ls, cat, mkdir...)", true, "3.1 MB"),
            PackageItem("neofetch", "7.1.0-4", "Fast, highly customizable system info script", true, "115 KB"),
            PackageItem("python3", "3.12.3", "Interactive high-level object-oriented language", true, "18.2 MB"),
            PackageItem("git", "2.43.0", "Fast, scalable, distributed revision control system", true, "14.5 MB"),
            PackageItem("curl", "8.5.0-2ubuntu10", "Command line tool for transferring data with URLs", true, "240 KB"),
            PackageItem("htop", "3.3.0-4", "Interactive processes viewer", true, "178 KB"),
            PackageItem("build-essential", "12.10ubuntu1", "Informational list of build-essential packages", false, "4.8 MB"),
            PackageItem("nodejs", "20.12.2", "Evented I/O for V8 JavaScript", false, "32.1 MB"),
            PackageItem("vim", "9.1.0-1ubuntu1", "Vi IMproved - enhanced vi editor", false, "2.9 MB")
        )

        val rootFiles = listOf(
            FileItem(".bashrc", "/root/.bashrc", false, "3.8 KB", "-rw-r--r--"),
            FileItem(".profile", "/root/.profile", false, "807 B", "-rw-r--r--"),
            FileItem("projects", "/root/projects", true, "4.0 KB", "drwxr-xr-x"),
            FileItem("downloads", "/root/downloads", true, "4.0 KB", "drwxr-xr-x"),
            FileItem("terminalhouse.conf", "/root/terminalhouse.conf", false, "512 B", "-rw-r--r--")
        )

        _uiState.update {
            it.copy(
                projects = defaultProjects,
                packages = defaultPackages,
                files = rootFiles
            )
        }
    }

    private fun startStatsMonitor() {
        viewModelScope.launch {
            while (isActive) {
                val stats = SystemMonitor.getRealStats(getApplication())
                _uiState.update { it.copy(systemStats = stats) }
                delay(4000)
            }
        }
    }

    fun onInputChange(newInput: String) {
        _uiState.update { it.copy(currentInput = newInput) }
    }

    fun submitCommand() {
        val input = _uiState.value.currentInput.trim()
        val activeId = _uiState.value.activeSessionId
        val session = _uiState.value.sessions.find { it.id == activeId } ?: return

        _uiState.update { it.copy(currentInput = "") }

        viewModelScope.launch {
            val promptLine = TerminalLine("root@ubuntu:${session.workingDir}# $input", LineType.INPUT)
            val updatedHistory = if (input.isNotEmpty() && !session.history.contains(input)) {
                session.history + input
            } else session.history

            val currentLines = session.lines.toMutableList()
            currentLines.add(promptLine)

            if (input.isEmpty()) {
                val updatedSession = session.copy(
                    lines = currentLines,
                    history = updatedHistory
                )
                updateSession(updatedSession)
                return@launch
            }

            val result = TerminalEngine.executeCommand(input, session.workingDir, getApplication())

            val finalLines = if (result.shouldClear) {
                emptyList()
            } else {
                currentLines + result.lines
            }

            val updatedSession = session.copy(
                lines = finalLines,
                workingDir = result.newDir,
                history = updatedHistory,
                historyIndex = -1
            )
            updateSession(updatedSession)
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
                TerminalLine("TerminalHouse Session $count - Ubuntu 24.04.5 LTS", LineType.SYSTEM),
                TerminalLine("Type 'help' for built-in tools.", LineType.OUTPUT)
            ),
            workingDir = "~"
        )
        _uiState.update {
            it.copy(
                sessions = it.sessions + newSession,
                activeSessionId = newSessionId
            )
        }
    }

    fun switchSession(sessionId: String) {
        _uiState.update { it.copy(activeSessionId = sessionId) }
    }

    fun closeSession(sessionId: String) {
        val currentSessions = _uiState.value.sessions
        if (currentSessions.size <= 1) {
            // Don't close the only session, just reset it
            val initialLines = TerminalEngine.createInitialSessionLines(getApplication())
            val resetSession = currentSessions.first().copy(lines = initialLines, workingDir = "~")
            updateSession(resetSession)
            return
        }

        val remaining = currentSessions.filter { it.id != sessionId }
        val newActiveId = if (_uiState.value.activeSessionId == sessionId) {
            remaining.last().id
        } else {
            _uiState.value.activeSessionId
        }

        _uiState.update {
            it.copy(
                sessions = remaining,
                activeSessionId = newActiveId
            )
        }
    }

    private fun updateSession(updated: TerminalSession) {
        _uiState.update { state ->
            val updatedList = state.sessions.map {
                if (it.id == updated.id) updated else it
            }
            state.copy(sessions = updatedList)
        }
    }

    fun toggleKeyboard() {
        _uiState.update { it.copy(isKeyboardVisible = !it.isKeyboardVisible) }
    }

    fun insertSpecialKey(keyText: String) {
        val current = _uiState.value.currentInput
        when (keyText) {
            "TAB" -> _uiState.update { it.copy(currentInput = current + "    ") }
            "ESC" -> _uiState.update { it.copy(currentInput = "") }
            "CTRL+C" -> {
                val activeId = _uiState.value.activeSessionId
                val session = _uiState.value.sessions.find { it.id == activeId }
                if (session != null) {
                    val lines = session.lines + TerminalLine("root@ubuntu:${session.workingDir}# ^C", LineType.INPUT)
                    updateSession(session.copy(lines = lines))
                }
                _uiState.update { it.copy(currentInput = "") }
            }
            "CLEAR" -> executeQuickCommand("clear")
            "UP" -> navigateHistory(up = true)
            "DOWN" -> navigateHistory(up = false)
            else -> _uiState.update { it.copy(currentInput = current + keyText) }
        }
    }

    private fun navigateHistory(up: Boolean) {
        val activeId = _uiState.value.activeSessionId
        val session = _uiState.value.sessions.find { it.id == activeId } ?: return
        if (session.history.isEmpty()) return

        var newIndex = if (up) {
            if (session.historyIndex == -1) session.history.size - 1 else (session.historyIndex - 1).coerceAtLeast(0)
        } else {
            if (session.historyIndex == -1) -1 else (session.historyIndex + 1)
        }

        if (newIndex >= session.history.size) newIndex = -1

        val command = if (newIndex in session.history.indices) session.history[newIndex] else ""
        updateSession(session.copy(historyIndex = newIndex))
        _uiState.update { it.copy(currentInput = command) }
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
            it.copy(
                aiMessages = it.aiMessages + userMessage,
                aiInputText = ""
            )
        }

        viewModelScope.launch {
            delay(300)
            val response = AiAgentService.processQuery(textToSend)
            _uiState.update { it.copy(aiMessages = it.aiMessages + response) }
        }
    }

    fun installPackage(pkg: PackageItem) {
        val updated = _uiState.value.packages.map {
            if (it.name == pkg.name) it.copy(isInstalled = true) else it
        }
        _uiState.update { it.copy(packages = updated) }
        executeQuickCommand("apt install ${pkg.name}")
    }
}
