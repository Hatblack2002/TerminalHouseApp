package com.example.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * v0.3.0 — acciones del menú contextual de copiado anclado al botón 📋 de la toolbar.
 */
enum class CopyAction { ALL, LAST_50, LAST_100, LAST_CMD, LAST_OUTPUT, LAST_LINE }

enum class AppScreen {
    TERMINAL,
    IA,
    AGENTE_IA,
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
    val isSplashActive: Boolean = true,
    // v0.3.0 — panel IA como bottom sheet (SECCIÓN 8)
    val showAiSheet: Boolean = false,
    val aiSheetExpanded: Boolean = false,
    val aiContext: String? = null,
    // v0.3.0 — entrada tecla a tecla al PTY (SECCIÓN 5) y vista en vivo del PTY
    val isInteractiveMode: Boolean = false,
    val isCommandRunning: Boolean = false,
    val liveTail: List<String> = emptyList(),
    // SECCIÓN 5 — tamaño de fuente del terminal (rango 10–24 en Ajustes, por defecto 14)
    val terminalFontSize: Float = 14f
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    /** v0.3.0 SECCIÓN 3 — estado real del bootstrap para el subtítulo dinámico del header. */
    val bootstrapStatus: StateFlow<LinuxBootstrap.Status> get() = LinuxBootstrap.status

    private val sessionNumber = AtomicInteger(1)
    private var liveTailJob: Job? = null

    init {
        initSession()
        startStatsMonitor()
        startBootstrap()
        dismissSplashAfterDelay()
        registerDeathTelemetry()
    }

    /**
     * Task 6 / problema 1: la muerte de un proceso PTY (PRoot/bash) ya no es
     * invisible. PtyBridge notifica la evidencia real (pid, código/senal, última
     * salida) y la UI la muestra en la sesión afectada. No se destruye nada aquí:
     * el siguiente comando relanza PRoot automáticamente.
     */
    private fun registerDeathTelemetry() {
        PtyBridge.deathListener = { info ->
            val lines = buildList {
                add(TerminalLine("[motor] Sesión PTY (pid=${info.pid}) terminó: ${info.decoded}.", LineType.SYSTEM))
                if (info.transcriptTail.isNotEmpty()) {
                    add(TerminalLine("[motor] Última salida del PTY antes de morir:", LineType.SYSTEM))
                    info.transcriptTail.forEach { add(TerminalLine("    $it", LineType.OUTPUT)) }
                }
                add(TerminalLine("[motor] El próximo comando relanzará PRoot automáticamente (el rootfs NO se toca).", LineType.SYSTEM))
            }
            _uiState.update { state ->
                state.copy(sessions = state.sessions.map { s ->
                    if (s.id == info.sessionId) s.copy(lines = s.lines + lines) else s
                })
            }
        }
    }

    override fun onCleared() {
        PtyBridge.deathListener = null
        liveTailJob?.cancel()
        super.onCleared()
    }

    // ------------------------------------------------------------------
    // v0.3.0 SECCIÓN 3 — Subtítulo dinámico (PROHIBIDO texto estático)
    // ------------------------------------------------------------------
    fun dynamicSubtitle(status: LinuxBootstrap.Status = LinuxBootstrap.status.value): String {
        val arch = SystemMonitor.lastArchitecture.ifBlank { "aarch64" }
        val st = status
        val estado = when (st) {
            is LinuxBootstrap.Status.Downloading ->
                if (st.totalBytes > 0) "descargando ${st.downloadedBytes * 100 / st.totalBytes}%" else "descargando"
            is LinuxBootstrap.Status.VerifyingHash -> "verificando integridad"
            is LinuxBootstrap.Status.Extracting -> "extrayendo (${st.processed})"
            is LinuxBootstrap.Status.Hardening -> "configurando rootfs"
            is LinuxBootstrap.Status.Checking -> "verificando estructura"
            is LinuxBootstrap.Status.Ready -> st.info.osPrettyName
            is LinuxBootstrap.Status.Failed -> "error: ${st.reason}"
            LinuxBootstrap.Status.NotStarted ->
                // "Nunca mostrar Ready si /bin/bash no es ejecutable" y "nunca Idle si
                // existe el marker": SystemMonitor.lastRootfsReady se calcula con
                // dpkgPackages>0 && bin/bash existe; el marker real es ubuntu.ready.
                if (SystemMonitor.lastRootfsReady && LinuxBootstrap.readyMarker(app()).exists()) {
                    SystemMonitor.lastOsPrettyName.ifBlank { "Ubuntu" }
                } else "listo para instalar"
        }
        return "TerminalHouse · $arch · $estado"
    }

    // ------------------------------------------------------------------
    // v0.3.0 SECCIÓN 5 — entrada tecla a tecla al PTY (sin buffer hasta Enter)
    // ------------------------------------------------------------------
    /**
     * El envío por tecla está activo cuando hay un comando en curso (p. ej. apt
     * preguntando [Y/n]) o el modo interactivo está activado (htop, cat, python3 -i).
     * En reposo el texto se compone localmente y Enter lo envía como comando.
     */
    private fun rawSendActive(): Boolean =
        _uiState.value.isCommandRunning || _uiState.value.isInteractiveMode

    fun sendRawBytes(bytes: ByteArray) {
        PtyBridge.writeRaw(activeSessionId(), bytes)
    }

    fun onTerminalChar(c: Char) {
        if (rawSendActive()) sendRawBytes(c.toString().toByteArray(Charsets.UTF_8))
    }

    fun setInteractiveMode(on: Boolean) {
        _uiState.update { it.copy(isInteractiveMode = on, liveTail = if (on) it.liveTail else emptyList()) }
        if (on) startLiveTailPoller() else {
            liveTailJob?.cancel(); liveTailJob = null
        }
    }

    /**
     * Vista en vivo del PTY: últimas líneas reales del transcript del emulador vivo
     * (mismo origen que la telemetría de muerte de Task 6). Se reemplaza completa en
     * cada sondeo — nunca se concatena ni se hace diff, así es imposible duplicar
     * bytes en la UI (lección de Task 6 / problema 2).
     */
    private fun startLiveTailPoller() {
        liveTailJob?.cancel()
        liveTailJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val tail = try {
                    PtyBridge.transcriptTail(activeSessionId(), 14)
                } catch (_: Throwable) {
                    emptyList()
                }
                _uiState.update { it.copy(liveTail = tail) }
                delay(300)
            }
        }
    }

    /**
     * Tras terminar un comando cuya naturaleza es interactiva (htop, vim, cat sin
     * argumentos, python3 sin argumentos...), activa el modo interactivo: el proceso
     * sigue en primer plano del PTY y cada tecla debe llegarle al instante.
     */
    private fun maybeAutoEngageInteractive(cmd: String) {
        val c = cmd.trim()
        val first = c.substringBefore(" ").trim()
        val alwaysInteractive = setOf("htop", "top", "vim", "vi", "nano", "less", "more", "ftp", "telnet", "ssh", "sqlite3", "watch")
        val shellOnly = setOf("cat", "python3", "python", "node", "irb")
        val engage = first in alwaysInteractive || (first in shellOnly && c == first)
        if (engage && !_uiState.value.isInteractiveMode) {
            appendToActive(TerminalLine("[motor] Modo interactivo: las teclas van al PTY al instante (Ctrl+C/Ctrl+D salen del proceso).", LineType.SYSTEM))
            setInteractiveMode(true)
        }
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
                    if (line != null) {
                        // Progreso (Descargando/Extrayendo): actualiza UNA línea viva en
                        // lugar de inundar el terminal con cientos de líneas.
                        if (line.startsWith("[bootstrap] Descargando") || line.startsWith("[bootstrap] Extrayendo")) {
                            appendProgress(line)
                        } else {
                            appendToActive(TerminalLine(line, LineType.SYSTEM))
                        }
                    }
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
        viewModelScope.launch(Dispatchers.IO) {
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
        // IO: getRealStats mide el rootfs real (recorrido de ~25k archivos tras el
        // bootstrap); en el hilo principal congelaba la UI cada 4 s.
        viewModelScope.launch(Dispatchers.IO) {
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

    /** Línea de progreso vivo: reemplaza la última si también era progreso, si no, añade. */
    private fun appendProgress(text: String) {
        val s = activeSession() ?: return
        val last = s.lines.lastOrNull()
        val updated = if (last != null &&
            (last.text.startsWith("[bootstrap] Descargando") || last.text.startsWith("[bootstrap] Extrayendo"))
        ) {
            s.copy(lines = s.lines.dropLast(1) + TerminalLine(text, LineType.SYSTEM))
        } else {
            s.copy(lines = s.lines + TerminalLine(text, LineType.SYSTEM))
        }
        updateSession(updated)
    }

    /** v0.3.0 SECCIÓN 5 — scrollback mínimo 2000 líneas. */
    private fun capScrollback(lines: List<TerminalLine>): List<TerminalLine> =
        if (lines.size > SCROLLBACK_MAX) lines.takeLast(SCROLLBACK_MAX) else lines

    private companion object {
        const val SCROLLBACK_MAX = 2000
    }

    fun onInputChange(newInput: String) {
        // v0.3.0 SECCIÓN 5: con envío por tecla activo, cada carácter insertado se
        // escribe al PTY INMEDIATAMENTE (diff local para teclado virtual); las
        // supresiones se envían como \x7F. Sin esto, [Y/n], read, vim o htop no
        // funcionarían con el teclado en pantalla.
        if (rawSendActive()) {
            val old = _uiState.value.currentInput
            val min = minOf(old.length, newInput.length)
            var p = 0
            while (p < min && old[p] == newInput[p]) p++
            var s = 0
            while (s < min - p && old[old.length - 1 - s] == newInput[newInput.length - 1 - s]) s++
            val removed = old.length - p - s
            val inserted = newInput.substring(p, newInput.length - s)
            val bytes = buildString {
                repeat(removed) { append('\u007F') }
                append(inserted)
            }
            if (bytes.isNotEmpty()) sendRawBytes(bytes.toByteArray(Charsets.UTF_8))
        }
        _uiState.update { it.copy(currentInput = newInput) }
    }

    fun submitCommand() {
        val input = _uiState.value.currentInput.trim()
        val activeId = activeSessionId()
        val session = activeSession() ?: return

        // v0.3.0 SECCIÓN 5: en modo interactivo/comando en curso, Enter envía SOLO
        // el byte \n al PTY (el texto ya llegó tecla a tecla). Nunca re-enviar la línea.
        if (rawSendActive()) {
            _uiState.update { it.copy(currentInput = "") }
            sendRawBytes(byteArrayOf(0x0A))
            return
        }

        _uiState.update { it.copy(currentInput = "") }
        if (input.isEmpty()) return

        viewModelScope.launch {
            val updatedHistory = if (!session.history.contains(input)) session.history + input else session.history
            updateSession(session.copy(history = updatedHistory))

            // 'bootstrap' REAL: si el entorno no está listo, relanza el bootstrap de
            // verdad (antes el mensaje "Reintenta con: bootstrap" era un callejón sin
            // salida: el comando solo llegaba al PTY inexistente).
            if (input == "bootstrap" && !LinuxBootstrap.isReady(app())) {
                appendToActive(TerminalLine("[TerminalHouse] Reintentando bootstrap real (descarga + SHA256 + extracción)...", LineType.SYSTEM))
                startBootstrap()
                return@launch
            }

            _uiState.update { it.copy(isCommandRunning = true) }
            try {
                val result = TerminalEngine.executeCommand(input, session.workingDir, app(), activeId)

                // Actualización atómica: las líneas de telemetría de muerte (hilo
                // principal) y el bloque del comando (IO) no se pisan entre sí.
                _uiState.update { state ->
                    val current = state.sessions.find { it.id == activeId } ?: return@update state
                    val finalLines = if (result.shouldClear) emptyList() else capScrollback(current.lines + result.lines)
                    state.copy(
                        sessions = state.sessions.map { if (it.id == current.id) current.copy(lines = finalLines) else it },
                        isCommandRunning = false
                    )
                }
                maybeAutoEngageInteractive(input)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isCommandRunning = false) }
                appendToActive(TerminalLine("[motor] Error ejecutando '$input': ${t.javaClass.simpleName}: ${t.message ?: "(sin mensaje)"}", LineType.ERROR))
            }
        }
    }

    fun executeQuickCommand(command: String) {
        _uiState.update { it.copy(currentInput = command) }
        submitCommand()
    }

    /**
     * v0.3.0 SECCIÓN 4 — semántica del botón +: UNA sola sesión activa, sin
     * acumulación. Orden obligatorio: cerrar la sesión PTY actual (los hijos
     * reciben SIGHUP al cerrarse el master_fd y PRoot corre con --kill-on-exit),
     * esperar confirmación de cierre (timeout 3 s), crear la nueva TerminalSession
     * con el mismo argv/envp del SessionLauncher y renombrar a "Terminal {n+1}".
     * PROHIBIDO: acumular sesiones, dejar fds abiertos, dejar huérfanos.
     */
    fun addNewSession() {
        val current = activeSession() ?: return
        if (current.id != activeSessionId()) return
        val n = sessionNumber.incrementAndGet()
        val newTitle = "Terminal $n"
        appendToActive(TerminalLine("[TerminalHouse] Cerrando sesión PTY actual y creando '$newTitle'...", LineType.SYSTEM))
        viewModelScope.launch(Dispatchers.IO) {
            // 1-2) cierre de la sesión: SIGHUP a hijos + cierre del master_fd
            PtyBridge.closeSession(current.id)
            // 3) los readers/reapers viven en TerminalSession (hilo del session);
            //    esperar confirmación de cierre con timeout de 3 s
            val deadline = System.currentTimeMillis() + 3_000L
            while (PtyBridge.isRunning(current.id) && System.currentTimeMillis() < deadline) {
                delay(50)
            }
            // 4-5) nueva TerminalSession con argv/envp reales (PtyBridge.buildArgv)
            val s = PtyBridge.ensureSession(current.id, app())
            withContext(Dispatchers.Main.immediate) {
                _uiState.update { state ->
                    state.copy(
                        sessions = state.sessions.map { ses ->
                            if (ses.id == current.id) ses.copy(
                                title = newTitle,
                                lines = capScrollback(
                                    TerminalEngine.createInitialSessionLines(app()) + listOf(
                                        TerminalLine("[motor] '$newTitle' lista${s?.let { " (pid=${it.getPid()})" } ?: ": el próximo comando relanzará PRoot"}.", LineType.SYSTEM)
                                    )
                                ),
                                history = emptyList(),
                                historyIndex = -1,
                                workingDir = "~"
                            ) else ses
                        },
                        isInteractiveMode = false,
                        liveTail = emptyList(),
                        isCommandRunning = false
                    )
                }
            }
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

    // ------------------------------------------------------------------
    // v0.3.0 SECCIÓN 7 — sistema de copiar/pegar
    // ------------------------------------------------------------------
    private fun promptIndex(texts: List<String>): Int {
        // Último prompt real (línea que empieza con "root@" o termina en #/$);
        // si no hay, el último eco de comando (INPUT); si no, el inicio.
        val idx = texts.indexOfLast { t ->
            val t = t.trim()
            t.startsWith("root@") || t.endsWith("#") || t.endsWith("$")
        }
        if (idx >= 0) return idx
        val inputIdx = activeSession()?.lines?.indexOfLast { it.type == LineType.INPUT } ?: -1
        return if (inputIdx >= 0) inputIdx else 0
    }

    /** Texto para cada opción del menú contextual de copiado. */
    fun buildCopyText(action: CopyAction): String {
        val session = activeSession() ?: return ""
        val texts = session.lines.map { it.text }
        val nonEmpty = texts.filter { it.isNotBlank() }
        return when (action) {
            CopyAction.ALL -> texts.joinToString("\n")
            CopyAction.LAST_50 -> nonEmpty.takeLast(50).joinToString("\n")
            CopyAction.LAST_100 -> nonEmpty.takeLast(100).joinToString("\n")
            CopyAction.LAST_CMD -> texts.drop(promptIndex(texts)).joinToString("\n")
            CopyAction.LAST_OUTPUT -> texts.drop(promptIndex(texts) + 1).joinToString("\n")
            CopyAction.LAST_LINE -> nonEmpty.lastOrNull() ?: ""
        }
    }

    fun copyLineCount(): Int = activeSession()?.lines?.count { it.text.isNotBlank() } ?: 0

    /** Pegado al PTY: los bytes van tal cual, preservando \n y caracteres de control. */
    fun pasteClipboardToPty() {
        val cm = app().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        if (text.isEmpty()) return
        sendRawBytes(text.toByteArray(Charsets.UTF_8))
        if (!rawSendActive()) {
            appendToActive(TerminalLine("[TerminalHouse] Portapapeles pegado al PTY (${text.length} caracteres).", LineType.SYSTEM))
        }
    }

    // ------------------------------------------------------------------
    // v0.3.0 SECCIÓN 8 — panel IA como bottom sheet
    // ------------------------------------------------------------------
    fun showAiSheet(expanded: Boolean = false) {
        _uiState.update { it.copy(showAiSheet = true, aiSheetExpanded = expanded) }
    }

    fun hideAiSheet() {
        _uiState.update { it.copy(showAiSheet = false) }
    }

    /** "Enviar al agente IA": el agente recibe SOLO lo enviado, no todo el terminal. */
    fun sendSelectionToAgent(text: String) {
        if (text.isBlank()) return
        _uiState.update { it.copy(aiContext = text, showAiSheet = true, aiSheetExpanded = false) }
    }

    fun clearAiContext() {
        _uiState.update { it.copy(aiContext = null) }
    }

    /**
     * Teclas de control: se escriben CRUDAS al PTY (bytes reales), como un terminal real.
     * Ctrl+C interrumpe el proceso en primer plano del rootfs; flechas recorren el
     * historial de bash real; TAB completa en el shell real.
     * v0.3.0 SECCIÓN 5 — mapeo completo: Ctrl+Z/L/A/E/U/W, Home, End, Left, Right,
     * Backspace; Enter siempre \n y Tab \t.
     */
    fun insertSpecialKey(keyText: String) {
        when (keyText) {
            "CTRL+C" -> {
                PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x03))
                _uiState.update { it.copy(currentInput = "") }
            }
            "CTRL+D" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x04))
            "CTRL+Z" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x1A))
            "CTRL+L" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x0C))
            "CTRL+A" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x01))
            "CTRL+E" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x05))
            "CTRL+U" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x15))
            "CTRL+W" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x17))
            "ESC" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x1B))
            "TAB" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x09))
            "UP" -> PtyBridge.writeRaw(activeSessionId(), "\u001B[A".toByteArray(Charsets.UTF_8))
            "DOWN" -> PtyBridge.writeRaw(activeSessionId(), "\u001B[B".toByteArray(Charsets.UTF_8))
            "LEFT" -> PtyBridge.writeRaw(activeSessionId(), "\u001B[D".toByteArray(Charsets.UTF_8))
            "RIGHT" -> PtyBridge.writeRaw(activeSessionId(), "\u001B[C".toByteArray(Charsets.UTF_8))
            "HOME" -> PtyBridge.writeRaw(activeSessionId(), "\u001B[H".toByteArray(Charsets.UTF_8))
            "END" -> PtyBridge.writeRaw(activeSessionId(), "\u001B[F".toByteArray(Charsets.UTF_8))
            "BS" -> PtyBridge.writeRaw(activeSessionId(), byteArrayOf(0x7F))
            "CLEAR" -> executeQuickCommand("clear")
            else -> _uiState.update { it.copy(currentInput = _uiState.value.currentInput + keyText) }
        }
    }

    /**
     * v0.3.0 — teclado físico (DeX/OTG): los atajos Ctrl son siempre bytes crudos
     * (no producen texto en el campo); en modo interactivo/comando en curso las
     * flechas/Inicio/Fin/Escape también van directos al PTY.
     */
    fun onHardwareControlKey(controlChar: Char): Boolean {
        val byte: Byte = when (controlChar) {
            'C' -> 0x03; 'D' -> 0x04; 'Z' -> 0x1A; 'L' -> 0x0C
            'A' -> 0x01; 'E' -> 0x05; 'U' -> 0x15; 'W' -> 0x17
            else -> return false
        }
        PtyBridge.writeRaw(activeSessionId(), byteArrayOf(byte))
        if (controlChar == 'C') _uiState.update { it.copy(currentInput = "") }
        return true
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

    /** SECCIÓN 5 — tamaño de fuente del terminal, rango 10–24 sp, por defecto 14. */
    fun setTerminalFontSize(size: Float) {
        _uiState.update { it.copy(terminalFontSize = size.coerceIn(10f, 24f)) }
    }

    fun onAiInputChange(text: String) {
        _uiState.update { it.copy(aiInputText = text) }
    }

    fun sendAiMessage(promptText: String? = null) {
        val textToSend = promptText ?: _uiState.value.aiInputText.trim()
        if (textToSend.isEmpty()) return

        // v0.3.0: si hay contexto seleccionado del terminal, SOLO ese contexto viaja
        // con la pregunta (nunca el buffer completo del terminal).
        val context = _uiState.value.aiContext
        val fullText = if (context != null) "Contexto del terminal:\n$context\n\nPregunta: $textToSend" else textToSend

        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = fullText
        )
        _uiState.update {
            it.copy(aiMessages = it.aiMessages + userMessage, aiInputText = "", aiContext = null)
        }
        viewModelScope.launch {
            delay(300)
            val response = AiAgentService.processQuery(fullText)
            _uiState.update { it.copy(aiMessages = it.aiMessages + response) }
        }
    }

    fun installPackage(pkg: PackageItem) {
        // NO se marca "instalado" por adelantado: el estado real lo decide apt.
        // El comando se ejecuta de verdad dentro del rootfs vía PTY.
        executeQuickCommand("apt install -y ${pkg.name}")
    }
}
