package com.example.service

import android.content.Context
import android.util.Log
import com.example.model.LineType
import com.example.model.TerminalLine
import com.terminalhouse.terminal.TerminalEmulator
import com.terminalhouse.terminal.TerminalOutput
import com.terminalhouse.terminal.TerminalSession
import com.terminalhouse.terminal.TerminalSessionClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Puente entre la UI de TerminalHouse y el motor real:
 *
 *   TerminalSession (termux-app GPLv3, PTY nativo via JNI libtermux.so)
 *     -> PRoot real (libproot.so de Termux, con loader real libloader.so)
 *       -> rootfs Ubuntu 24.04 real (context.filesDir/ubuntu)
 *         -> /bin/bash --login
 *           -> userspace Ubuntu real
 *
 * Task 6 (diagnóstico y corrección, sin cambiar la arquitectura):
 *
 * 1) ANTI-DUPLICACIÓN (causa raíz eliminada): antes, el bloque de cada comando se
 *    derivaba por DIFF de snapshots del transcript del emulador vivo; con rollover
 *    (>500 filas), con redraws in-place (Ctrl+C, TAB, flechas) o ante cualquier
 *    excepción puntual de lectura, el prefijo dejaba de alinear y se re-añadía el
 *    transcript COMPLETO a la UI (los bloques repetidos del registro). Ahora los
 *    bytes crudos del PTY se capturan en un tap (TerminalSession.RawOutputTap,
 *    invocado ANTES de entregarlos al emulador) y cada comando se parsea en un
 *    EMULADOR FRESCO: el bloque es exactamente lo que ese comando produjo. Ni el
 *    emulador vivo ni la UI pueden duplicar bytes.
 *
 * 2) TELEMETRÍA DE MUERTE DE SESIÓN: ya no existe un "Sesión no disponible" sin
 *    diagnóstico. onSessionFinished registra pid, código de salida o señal (decode
 *    SIGKILL/SIGTERM/...), últimas líneas del PTY y lo notifica a la UI; el
 *    relanzamiento automático informa el pid anterior y el nuevo.
 *
 * 3) GRUPOS: buildEnvp exporta TH_CLEAR_SUPPL_GROUPS=1 para que termux.c descarte
 *    los GIDs suplementarios de Android (3003/9997/20388/50388) antes de exec.
 *
 * 4) Concurrencia: runCommand YA NO retiene el monitor global 90 s (antes
 *    writeRaw/closeSession/ensureSession desde el hilo principal podían quedar
 *    bloqueados detrás de un comando largo). El mapa es ConcurrentHashMap y la
 *    exclusión larga es por sesión (shell.commandMutex).
 */
object PtyBridge {

    private const val TAG = "PtyBridge"
    private const val COLUMNS = 120
    private const val ROWS = 40
    private const val TRANSCRIPT_ROWS = 500

    /** Registro de la muerte de un proceso PTY (telemetría visible en UI). */
    data class DeathInfo(
        val sessionId: String,
        val pid: Int,
        val exitStatus: Int, // crudo de JNI.waitFor: >=0 código; <0 = -señal
        val decoded: String,
        val killedBySignal: Boolean,
        val transcriptTail: List<String>,
        val timestampMs: Long
    )

    private class Shell(
        val id: String,
        val session: TerminalSession,
        val client: TerminalSessionClient
    ) {
        val commandMutex = Any() // serializa comandos DE ESTA sesión
        val tapLock = Any()
        val tapBytes = ByteArrayOutputStream()
        var tapTotal = 0L
        var capturing = false
    }

    private val sessions = ConcurrentHashMap<String, Shell>()

    /** Observador de muertes (la UI lo registra para telemetría visible). */
    @Volatile
    var deathListener: ((DeathInfo) -> Unit)? = null

    private val lastDeath = ConcurrentHashMap<String, DeathInfo>()
    private val relaunchReported = ConcurrentHashMap<String, String>() // sessionId -> timestampMs reportado
    private val lastFailure = ConcurrentHashMap<String, String>()      // sessionId -> motivo del fallo de creación

    // ------------------------------------------------------------------
    // Rutas reales
    // ------------------------------------------------------------------
    fun nativeLibDir(context: Context): String =
        context.applicationInfo.nativeLibraryDir ?: ""

    fun prootFile(context: Context): File = File(nativeLibDir(context), "libproot.so")
    fun loaderFile(context: Context): File = File(nativeLibDir(context), "libloader.so")
    fun loader32File(context: Context): File = File(nativeLibDir(context), "libloader32.so")

    /** Verificación física del binario PRoot: existencia + arquitectura + permisos. */
    fun prootCheck(context: Context): Triple<Boolean, String, String> {
        val f = prootFile(context)
        val exists = f.exists()
        val exec = f.canExecute()
        val size = if (exists) f.length() else -1L
        return Triple(exists && exec, "exists=$exists executable=$exec size=$size", f.absolutePath)
    }

    fun isEngineReady(context: Context): Boolean {
        val p = prootCheck(context)
        return p.first && LinuxBootstrap.isReady(context)
    }

    // ------------------------------------------------------------------
    // Sesión PRoot sobre PTY real
    // ------------------------------------------------------------------
    /**
     * argv real ejecutado dentro del PTY (execvp lo hace termux.c):
     *
     *   libproot.so --kill-on-exit -0 -w /root -r <filesDir>/ubuntu \
     *     -b /dev -b /proc -b /sys [-b /sdcard] \
     *     /usr/bin/env -i HOME=/root TERM=xterm-256color \
     *     PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
     *     SHELL=/bin/bash USER=root LOGNAME=root LANG=C.UTF-8 LC_ALL=C.UTF-8 \
     *     PWD=/root TMPDIR=/tmp /bin/bash --login
     */
    fun buildArgv(context: Context): List<String> {
        val rootfs = LinuxBootstrap.rootfsDir(context)
        val args = mutableListOf(
            prootFile(context).absolutePath,
            "--kill-on-exit",
            "-0",                       // identidad virtual root (uid 0) dentro del guest
            "-w", "/root",              // cwd inicial del guest
            "-r", rootfs.absolutePath   // raíz del rootfs real
        )
        // Fase 5: bindings del host al guest
        args += listOf("-b", "/dev")    // incluye /dev/null, /dev/urandom, /dev/random, /dev/pts
        args += listOf("-b", "/proc")   // incluye /proc/self/fd
        args += listOf("-b", "/sys")
        if (File("/sdcard").exists()) args += listOf("-b", "/sdcard")
        // Env del guest (env -i) y shell final
        args += listOf(
            "/usr/bin/env", "-i",
            "HOME=/root",
            "TERM=xterm-256color",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "SHELL=/bin/bash",
            "USER=root",
            "LOGNAME=root",
            "LANG=C.UTF-8",
            "LC_ALL=C.UTF-8",
            "PWD=/root",
            "TMPDIR=/tmp",
            "/bin/bash", "--login"
        )
        return args
    }

    /** envp real del proceso PRoot (termux.c hace clearenv() + putenv de cada entrada). */
    fun buildEnvp(context: Context): List<String> {
        val env = mutableListOf(
            "LD_LIBRARY_PATH=${nativeLibDir(context)}",
            "PROOT_LOADER=${loaderFile(context).absolutePath}",
            "PROOT_TMP_DIR=${context.cacheDir.absolutePath}",
            "PATH=/system/bin:/system/xbin",
            "HOME=/root",
            "TERM=xterm-256color",
            // Task 6 / problema 3: el hijo (pre-exec) descarta los GIDs suplementarios
            // heredados de Android antes de lanzar PRoot; el resultado real (OK o EPERM)
            // queda registrado en logcat por termux.c.
            "TH_CLEAR_SUPPL_GROUPS=1"
        )
        if (loader32File(context).exists()) env.add("PROOT_LOADER32=${loader32File(context).absolutePath}")
        // Compatibilidad: algunos kernels Android rechazan el filtro seccomp de proot.
        // Sin seccomp propio, proot sigue siendo plenamente funcional (trazado por ptrace).
        env.add("PROOT_NO_SECCOMP=1")
        return env
    }

    /** Decode legible y verídico del estado de salida de JNI.waitFor. */
    private fun decodeExit(exit: Int): String = when {
        exit == 0 -> "código 0 (salida normal del shell)"
        exit > 0 -> "código de salida $exit"
        else -> {
            val sig = -exit
            val name = when (sig) {
                1 -> "SIGHUP"; 2 -> "SIGINT"; 3 -> "SIGQUIT"; 4 -> "SIGILL"; 5 -> "SIGTRAP"
                6 -> "SIGABRT"; 7 -> "SIGBUS"; 8 -> "SIGFPE"; 9 -> "SIGKILL"; 11 -> "SIGSEGV"
                13 -> "SIGPIPE"; 14 -> "SIGALRM"; 15 -> "SIGTERM"; 17 -> "SIGCHLD"
                else -> "SIG#$sig"
            }
            val hint = if (sig == 9) " — kill externo (sistema/LMK, phantom-process killer de Android 12+, o usuario)" else ""
            "señal $name ($sig)$hint"
        }
    }

    private fun buildClient(sessionId: String): TerminalSessionClient = object : TerminalSessionClient {
        override fun onTextChanged(changedSession: TerminalSession) = Unit
        override fun onTitleChanged(changedSession: TerminalSession) = Unit
        override fun onSessionFinished(finishedSession: TerminalSession) {
            val exit = finishedSession.getExitStatus()
            val info = DeathInfo(
                sessionId = sessionId,
                pid = finishedSession.getPid(),
                exitStatus = exit,
                decoded = decodeExit(exit),
                killedBySignal = exit < 0,
                transcriptTail = transcriptTail(sessionId, 6),
                timestampMs = System.currentTimeMillis()
            )
            lastDeath[sessionId] = info
            Log.w(TAG, "Sesión PTY finalizada (sesión=$sessionId pid=${info.pid} ${info.decoded}; master fd cerrado por cleanupResources de TerminalSession)")
            Log.i(TAG, "Últimas líneas del PTY antes de morir: ${info.transcriptTail.joinToString(" | ")}")
            try {
                deathListener?.invoke(info)
            } catch (e: Throwable) {
                Log.e(TAG, "deathListener falló: ${e.message}", e)
            }
        }
        override fun onCopyTextToClipboard(session: TerminalSession, text: String) = Unit
        override fun onPasteTextFromClipboard(session: TerminalSession?) = Unit
        override fun onBell(session: TerminalSession) = Unit
        override fun onColorsChanged(session: TerminalSession) = Unit
        override fun onTerminalCursorStateChange(state: Boolean) = Unit
        override fun setTerminalShellPid(session: TerminalSession, pid: Int) = Unit
        override fun getTerminalCursorStyle(): Int = 0
        override fun logError(tag: String, message: String) { Log.e(tag, message) }
        override fun logWarn(tag: String, message: String) { Log.w(tag, message) }
        override fun logInfo(tag: String, message: String) { Log.i(tag, message) }
        override fun logDebug(tag: String, message: String) { Log.d(tag, message) }
        override fun logVerbose(tag: String, message: String) { Log.v(tag, message) }
        override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) { Log.e(tag, message, e) }
        override fun logStackTrace(tag: String, e: Exception) { Log.e(tag, "stacktrace", e) }
    }

    @Synchronized
    fun ensureSession(sessionId: String, context: Context): TerminalSession? {
        val appContext = context.applicationContext
        lastFailure.remove(sessionId)
        sessions[sessionId]?.let { prev ->
            if (prev.session.isRunning) return prev.session
            // Sesión previa MUERTA: no se oculta; se relanza y se informa con evidencia.
            val death = lastDeath[sessionId]
            Log.w(TAG, "Sesión previa $sessionId muerta (pid=${prev.session.getPid()}, exit=${prev.session.getExitStatus()}); relanzando PRoot")
            closeSession(sessionId)
            if (death != null) relaunchReported.remove("$sessionId@${death.timestampMs}")
        }

        val check = prootCheck(appContext)
        if (!check.first) {
            lastFailure[sessionId] = "PRoot no disponible: ${check.second}"
            Log.e(TAG, lastFailure[sessionId]!!)
            return null
        }
        if (!LinuxBootstrap.isReady(appContext)) {
            lastFailure[sessionId] = "rootfs no verificado (marcador ubuntu.ready ausente o verificación rápida fallida)"
            Log.e(TAG, "Rootfs no verificado; no se lanza la sesión")
            return null
        }

        return try {
            val argv = buildArgv(appContext)
            val envp = buildEnvp(appContext)
            val cwdGuest = File(LinuxBootstrap.rootfsDir(appContext), "root").let {
                if (it.isDirectory) it.absolutePath else LinuxBootstrap.rootfsDir(appContext).absolutePath
            }
            val client = buildClient(sessionId)
            val session = TerminalSession(
                argv[0],                    // libproot.so (binario real)
                cwdGuest,                   // chdir pre-exec (ruta real del host)
                argv.drop(1).toTypedArray(),
                envp.toTypedArray(),
                Integer.valueOf(TRANSCRIPT_ROWS),
                client
            )
            // initializeEmulator arranca execvp(argv) en el PTY (JNI termux.c)
            session.initializeEmulator(COLUMNS, ROWS, 8, 16)
            val shell = Shell(sessionId, session, client)
            // Tap de bytes crudos: se captura SOLO durante un comando (capturing=true).
            session.setRawOutputTap { buf, len ->
                synchronized(shell.tapLock) {
                    if (shell.capturing) {
                        shell.tapBytes.write(buf, 0, len)
                        shell.tapTotal += len
                    }
                }
            }
            sessions[sessionId] = shell
            Log.i(TAG, "PRoot+bash iniciado (sesión=$sessionId pid=${session.getPid()} master_fd=${session.terminalFileDescriptor} argv0=${argv[0]})")
            session
        } catch (e: Throwable) {
            lastFailure[sessionId] = "${e.javaClass.simpleName}: ${e.message ?: "(sin mensaje)"}"
            Log.e(TAG, "No se pudo iniciar PRoot sobre PTY: ${e.message}", e)
            null
        }
    }

    // ------------------------------------------------------------------
    // E/S
    // ------------------------------------------------------------------
    fun writeRaw(sessionId: String, bytes: ByteArray) {
        // Sin monitor del objeto: nunca bloquea el hilo principal aunque haya un
        // comando largo en curso (Ctrl+C sigue llegando al PTY al instante).
        sessions[sessionId]?.session?.write(bytes, 0, bytes.size)
    }

    fun runCommand(
        sessionId: String,
        command: String,
        context: Context,
        timeoutMs: Long = 90_000L
    ): List<TerminalLine>? {
        val s = ensureSession(sessionId, context) ?: return null
        val shell = sessions[sessionId] ?: return null

        synchronized(shell.commandMutex) {
            // Captura EXCLUSIVA de los bytes que produzca este comando:
            synchronized(shell.tapLock) {
                shell.tapBytes.reset()
                shell.capturing = true
            }
            val bytes = (command + "\n").toByteArray(Charsets.UTF_8)
            s.write(bytes, 0, bytes.size)

            // Sondeo por BYTES (no por snapshot): estable cuando dejan de llegar
            // bytes crudos del PTY (700 ms sin cambios) o timeout.
            var last = synchronized(shell.tapLock) { shell.tapTotal }
            var stable = 0L
            val deadline = System.currentTimeMillis() + timeoutMs
            while (stable < 700L && System.currentTimeMillis() < deadline) {
                try { Thread.sleep(150) } catch (_: InterruptedException) { }
                val current = synchronized(shell.tapLock) { shell.tapTotal }
                if (current == last) stable += 150L else { stable = 0L; last = current }
            }
            synchronized(shell.tapLock) { shell.capturing = false }
            val raw = synchronized(shell.tapLock) { shell.tapBytes.toByteArray() }
            Log.i(TAG, "Comando '$command': bytes crudos del PTY=${raw.size} (capacidad real consumida; una sola entrega al emulador)")

            val block = parseBlock(shell, command, raw)
            return relaunchNotice(sessionId, s) + block
        }
    }

    /**
     * Parsea los bytes crudos del comando en un emulador FRESCO (misma clase que el
     * emulador vivo). El resultado es exactamente el bloque de ese comando: eco del
     * comando, salida y prompt siguiente. Sin diffs, sin rollover, sin duplicación.
     */
    private fun parseBlock(shell: Shell, command: String, raw: ByteArray): List<TerminalLine> {
        if (raw.isEmpty()) return emptyList()
        return try {
            val emu = TerminalEmulator(
                DummyOutput(), COLUMNS, ROWS, 8, 16,
                Integer.valueOf(TRANSCRIPT_ROWS), shell.client
            )
            emu.append(raw, raw.size)
            val text = emu.screen.transcriptTextWithFullLinesJoined
            val lines = text.split("\n").map { it.trimEnd() }.dropLastWhile { it.isBlank() }
            lines.mapIndexed { idx, txt ->
                val t = txt.trim()
                val type = when {
                    idx == 0 && t == command.trim() -> LineType.INPUT
                    t.endsWith("$") || t.endsWith("#") -> LineType.PROMPT
                    else -> LineType.OUTPUT
                }
                TerminalLine(txt, type)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "parseBlock falló (${e.javaClass.simpleName}: ${e.message}); bloque vacío, sin duplicar nada", e)
            emptyList()
        }
    }

    /** Subclase mínima de TerminalOutput para el emulador de parseo (sin efectos). */
    private class DummyOutput : TerminalOutput() {
        override fun write(data: ByteArray?, offset: Int, count: Int) = Unit
        override fun titleChanged(oldTitle: String?, newTitle: String?) = Unit
        override fun onCopyTextToClipboard(text: String?) = Unit
        override fun onPasteTextFromClipboard() = Unit
        override fun onBell() = Unit
        override fun onColorsChanged() = Unit
    }

    /**
     * Si la sesión murió y esta es su primera ejecución tras el relanzamiento,
     * antepone el aviso con la evidencia (pid anterior, causa, pid nuevo).
     */
    private fun relaunchNotice(sessionId: String, nueva: TerminalSession): List<TerminalLine> {
        val death = lastDeath[sessionId] ?: return emptyList()
        val key = "$sessionId@${death.timestampMs}"
        val primeraVez = relaunchReported.putIfAbsent(key, "reported") == null
        if (!primeraVez) return emptyList()
        val lines = mutableListOf<TerminalLine>()
        lines += TerminalLine("[motor] La sesión anterior (pid=${death.pid}) terminó: ${death.decoded}.", LineType.SYSTEM)
        if (death.transcriptTail.isNotEmpty()) {
            lines += TerminalLine("[motor] Última salida del PTY antes de morir: ${death.transcriptTail.takeLast(3).joinToString(" | ")}", LineType.SYSTEM)
        }
        lines += TerminalLine("[motor] PRoot relanzado automáticamente (pid=${nueva.getPid()}).", LineType.SYSTEM)
        return lines
    }

    private fun snapshot(s: TerminalSession): List<String> {
        val text = try {
            s.emulator.screen.transcriptTextWithFullLinesJoined
        } catch (e: Exception) {
            ""
        }
        return text.split("\n").map { it.trimEnd() }
    }

    /** Últimas líneas reales del transcript del PTY (diagnóstico). */
    fun transcriptTail(sessionId: String, maxLines: Int = 20): List<String> {
        val s = sessions[sessionId]?.session ?: return emptyList()
        val lines = snapshot(s)
        return lines.takeLast(maxLines)
    }

    // ------------------------------------------------------------------
    // Sesiones
    // ------------------------------------------------------------------
    fun closeSession(sessionId: String) {
        val prev = sessions.remove(sessionId) ?: return
        val caller = Thread.currentThread().stackTrace
            .drop(1).take(3)
            .joinToString(" <- ") { "${it.className.substringAfterLast('.').substringBefore('$')}.${it.methodName}:${it.lineNumber}" }
        Log.i(TAG, "closeSession($sessionId) llamado desde: $caller")
        prev.session.finishIfRunning()
    }

    fun isRunning(sessionId: String): Boolean = sessions[sessionId]?.session?.isRunning ?: false

    fun getPid(sessionId: String): Int = sessions[sessionId]?.session?.getPid() ?: -1

    /** Motivo del último fallo de creación de sesión (para no mostrar un "no disponible" ciego). */
    fun lastFailureReason(sessionId: String): String = lastFailure[sessionId] ?: "motivo no registrado (sesión nunca creada)"

    fun deathInfo(sessionId: String): DeathInfo? = lastDeath[sessionId]

    // ------------------------------------------------------------------
    // Sondas /proc (evidencia física de procesos, fd y grupos)
    // ------------------------------------------------------------------
    private fun procState(pid: Int): String = try {
        val stat = File("/proc/$pid/stat").readText()
        val fields = stat.substringAfterLast(") ").trim().split(" ")
        "estado=${fields.getOrElse(0) { "?" }}"
    } catch (e: Exception) {
        "estado=inexistente (proceso terminado)"
    }

    private fun childPids(pid: Int): List<Int> = try {
        File("/proc/$pid/task/$pid/children").readText().trim()
            .split(" ").filter { it.isNotBlank() }.map { it.toInt() }
    } catch (e: Exception) {
        emptyList()
    }

    private fun groupsLine(pid: Int): String = try {
        File("/proc/$pid/status").readLines()
            .firstOrNull { it.startsWith("Groups:") }?.trim() ?: "n/d"
    } catch (e: Exception) {
        "n/d (proceso terminado)"
    }

    // ------------------------------------------------------------------
    // Diagnósticos reales (Fase 9 + Task 6)
    // ------------------------------------------------------------------
    fun diagnostics(context: Context, sessionId: String): List<Pair<String, String>> {
        val appContext = context.applicationContext
        val argv = buildArgv(appContext)
        val envp = buildEnvp(appContext)
        val check = prootCheck(appContext)
        val info = LinuxBootstrap.metrics(appContext)
        val shell = sessions[sessionId]
        val out = mutableListOf<Pair<String, String>>()
        out.add("Ruta rootfs" to info.path)
        out.add("Tamaño rootfs" to "%.1f MB".format(info.sizeBytes / (1024.0 * 1024.0)))
        out.add("Archivos rootfs" to "${info.fileCount}")
        out.add("Symlinks rootfs" to "${info.symlinkCount}")
        out.add("/bin/bash" to (if (File(LinuxBootstrap.rootfsDir(appContext), "bin/bash").exists()) "presente" else "AUSENTE"))
        out.add("SO rootfs" to info.osPrettyName)
        out.add("Paquetes dpkg" to "${info.dpkgPackages}")
        out.add("Ruta PRoot" to check.third)
        out.add("PRoot check" to check.second)
        out.add("Loader" to loaderFile(appContext).absolutePath)
        out.add("Loader32" to (if (loader32File(appContext).exists()) loader32File(appContext).absolutePath else "n/a (32-bit host)"))
        out.add("argv" to argv.joinToString(" "))
        out.add("envp" to envp.joinToString(" "))
        out.add("libtermux.so" to (if (File(nativeLibDir(appContext), "libtermux.so").exists()) "presente" else "AUSENTE"))
        val pid = shell?.session?.getPid() ?: -1
        out.add("PID sesión" to "$pid")
        out.add("Estado sesión" to (if (isRunning(sessionId)) "activa" else "inactiva"))
        if (shell != null && pid > 0) {
            out.add("master_fd" to "${shell.session.terminalFileDescriptor}")
            out.add("Estado PRoot" to procState(pid))
            val children = childPids(pid)
            out.add("PID(s) hijos (bash)" to (if (children.isEmpty()) "sin hijos registrados" else children.joinToString(", ") { c ->
                "$c ${procState(c)}"
            }))
            out.add("GIDs del proceso (host)" to groupsLine(pid))
        }
        val death = lastDeath[sessionId]
        out.add("Última muerte" to (death?.let { "pid=${it.pid}, ${it.decoded}" } ?: "ninguna registrada"))
        out.add("Motivo último fallo" to lastFailureReason(sessionId))
        return out
    }
}
