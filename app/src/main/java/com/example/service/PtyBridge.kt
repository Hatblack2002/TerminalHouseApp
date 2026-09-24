package com.example.service

import android.content.Context
import android.util.Log
import com.example.model.LineType
import com.example.model.TerminalLine
import com.terminalhouse.terminal.TerminalSession
import com.terminalhouse.terminal.TerminalSessionClient
import java.io.File

/**
 * Puente entre la UI de TerminalHouse y el motor real:
 *
 *   TerminalSession (termux-app GPLv3, PTY nativo via JNI libtermux.so)
 *     -> PRoot real (libproot.so de Termux, con loader real libloader.so)
 *       -> rootfs Ubuntu 24.04 real (context.filesDir/ubuntu)
 *         -> /bin/bash --login
 *           -> userspace Ubuntu real
 *
 * La sesión ya NO lanza /system/bin/sh. Todo comando del usuario llega al
 * shell real del rootfs a través del PTY.
 */
object PtyBridge {

    private const val TAG = "PtyBridge"
    private const val COLUMNS = 120
    private const val ROWS = 40
    private const val TRANSCRIPT_ROWS = 500

    private class Shell(
        val session: TerminalSession,
        val client: TerminalSessionClient
    )

    private val sessions = LinkedHashMap<String, Shell>()

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
            "TERM=xterm-256color"
        )
        if (loader32File(context).exists()) env.add("PROOT_LOADER32=${loader32File(context).absolutePath}")
        // Compatibilidad: algunos kernels Android rechazan el filtro seccomp de proot.
        // Sin seccomp propio, proot sigue siendo plenamente funcional (trazado por ptrace).
        env.add("PROOT_NO_SECCOMP=1")
        return env
    }

    @Synchronized
    fun ensureSession(sessionId: String, context: Context): TerminalSession? {
        val appContext = context.applicationContext
        sessions[sessionId]?.let { if (it.session.isRunning) return it.session else closeSession(sessionId) }

        val check = prootCheck(appContext)
        if (!check.first) {
            Log.e(TAG, "PRoot no disponible: ${check.second}")
            return null
        }
        if (!LinuxBootstrap.isReady(appContext)) {
            Log.e(TAG, "Rootfs no verificado; no se lanza la sesión")
            return null
        }

        val client = object : TerminalSessionClient {
            override fun onTextChanged(changedSession: TerminalSession) = Unit
            override fun onTitleChanged(changedSession: TerminalSession) = Unit
            override fun onSessionFinished(finishedSession: TerminalSession) {
                Log.w(TAG, "Sesión PTY finalizada (exit=${finishedSession.exitStatus})")
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

        return try {
            val argv = buildArgv(appContext)
            val envp = buildEnvp(appContext)
            val cwdGuest = File(LinuxBootstrap.rootfsDir(appContext), "root").let {
                if (it.isDirectory) it.absolutePath else LinuxBootstrap.rootfsDir(appContext).absolutePath
            }
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
            sessions[sessionId] = Shell(session, client)
            Log.i(TAG, "PRoot+bash iniciado (pid=${session.getPid()}, argv0=${argv[0]})")
            session
        } catch (e: Throwable) {
            Log.e(TAG, "No se pudo iniciar PRoot sobre PTY: ${e.message}", e)
            null
        }
    }

    // ------------------------------------------------------------------
    // E/S
    // ------------------------------------------------------------------
    @Synchronized
    fun writeRaw(sessionId: String, bytes: ByteArray) {
        sessions[sessionId]?.session?.write(bytes, 0, bytes.size)
    }

    @Synchronized
    fun runCommand(
        sessionId: String,
        command: String,
        context: Context,
        timeoutMs: Long = 90_000L
    ): List<TerminalLine>? {
        val s = ensureSession(sessionId, context) ?: return null
        val bytes = (command + "\n").toByteArray(Charsets.UTF_8)
        val before = snapshot(s)
        s.write(bytes, 0, bytes.size)

        // Sondeo hasta que la salida se estabilice (700 ms sin cambios) o timeout.
        var last = snapshot(s)
        var stable = 0L
        val deadline = System.currentTimeMillis() + timeoutMs
        while (stable < 700L && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(150) } catch (_: InterruptedException) { }
            val current = snapshot(s)
            if (current == last) stable += 150L else { stable = 0L; last = current }
        }

        val delta = if (last.size > before.size && last.subList(0, before.size) == before) {
            last.subList(before.size, last.size)
        } else {
            last
        }.dropLastWhile { it.isBlank() }

        if (delta.isEmpty()) return emptyList()

        return delta.mapIndexed { idx, text ->
            val t = text.trim()
            val type = when {
                idx == 0 && t == command.trim() -> LineType.INPUT
                t.endsWith("$") || t.endsWith("#") -> LineType.PROMPT
                else -> LineType.OUTPUT
            }
            TerminalLine(text, type)
        }
    }

    private fun snapshot(s: TerminalSession): List<String> {
        val text = try {
            s.emulator.screen.transcriptTextWithFullLinesJoined
        } catch (e: Exception) {
            ""
        }
        return text.split("\n").map { it.trimEnd() }
    }

    /** Últimas líneas reales del transcript del PTY (diagnóstico Fase 9). */
    @Synchronized
    fun transcriptTail(sessionId: String, maxLines: Int = 20): List<String> {
        val s = sessions[sessionId]?.session ?: return emptyList()
        val lines = snapshot(s)
        return lines.takeLast(maxLines)
    }

    // ------------------------------------------------------------------
    // Sesiones
    // ------------------------------------------------------------------
    @Synchronized
    fun closeSession(sessionId: String) {
        sessions.remove(sessionId)?.session?.finishIfRunning()
    }

    @Synchronized
    fun isRunning(sessionId: String): Boolean = sessions[sessionId]?.session?.isRunning ?: false

    @Synchronized
    fun getPid(sessionId: String): Int = sessions[sessionId]?.session?.getPid() ?: -1

    // ------------------------------------------------------------------
    // Diagnósticos reales (Fase 9)
    // ------------------------------------------------------------------
    fun diagnostics(context: Context, sessionId: String): List<Pair<String, String>> {
        val appContext = context.applicationContext
        val argv = buildArgv(appContext)
        val envp = buildEnvp(appContext)
        val check = prootCheck(appContext)
        val info = LinuxBootstrap.metrics(appContext)
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
        out.add("PID sesión" to "${getPid(sessionId)}")
        out.add("Estado sesión" to (if (isRunning(sessionId)) "activa" else "inactiva"))
        out.add("libtermux.so" to (if (File(nativeLibDir(appContext), "libtermux.so").exists()) "presente" else "AUSENTE"))
        return out
    }
}
