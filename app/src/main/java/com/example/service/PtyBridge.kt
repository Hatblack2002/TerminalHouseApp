package com.example.service

import android.content.Context
import android.util.Log
import com.example.model.LineType
import com.example.model.TerminalLine
import com.terminalhouse.terminal.TerminalSession
import com.terminalhouse.terminal.TerminalSessionClient

/**
 * Puente entre la UI de TerminalHouse (Gemini) y el motor real :terminal
 * (termux-app GPLv3, carpeta terminal-emulator; PTY nativo vía JNI libtermux.so).
 *
 * Mantiene una sesión interactiva persistente de /system/bin/sh sobre un PTY real
 * (fork/openpty vía JNI) y traduce la salida del emulador de terminal a las
 * [TerminalLine] que consume la UI existente. La API pública de la UI no cambia:
 * este objeto es el "motor" detrás de TerminalEngine.executeCommand().
 */
object PtyBridge {

    private const val TAG = "PtyBridge"
    private const val COLUMNS = 120
    private const val ROWS = 40
    private const val TRANSCRIPT_ROWS = 500

    @Volatile
    private var session: TerminalSession? = null

    private val client = object : TerminalSessionClient {
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

    /** Sesión PTY viva; la recrea si el shell anterior terminó. */
    @Synchronized
    fun ensureSession(context: Context): TerminalSession? {
        val existing = session
        if (existing != null && existing.isRunning) return existing
        return try {
            val home = context.filesDir.absolutePath
            val env = arrayOf(
                "PATH=/system/bin:/system/xbin:/vendor/bin",
                "HOME=$home",
                "TMPDIR=${context.cacheDir.absolutePath}",
                "LD_LIBRARY_PATH=/system/lib64:/vendor/lib64:/system/lib:/vendor/lib",
                "ANDROID_ROOT=/system",
                "ANDROID_DATA=/data",
                "TERM=vt100",
                "LANG=en_US.UTF-8"
            )
            val s = TerminalSession(
                "/system/bin/sh",      // shell real del dispositivo
                home,                  // cwd
                arrayOf("-i"),         // interactivo (muestra prompt)
                env,
                Integer.valueOf(TRANSCRIPT_ROWS),
                client
            )
            // initializeEmulator crea el emulador Y arranca el proceso del shell en el PTY.
            s.initializeEmulator(COLUMNS, ROWS, 8, 16)
            session = s
            Log.i(TAG, "PTY nativo iniciado (pid=${s.getPid()}, motor libtermux.so)")
            s
        } catch (e: Throwable) {
            Log.e(TAG, "No se pudo iniciar el PTY nativo: ${e.message}", e)
            null
        }
    }

    /**
     * Ejecuta un comando en el PTY real y devuelve su salida como líneas para la UI.
     * Devuelve null si el motor nativo no está disponible (permite fallback de la UI).
     */
    @Synchronized
    fun runCommand(command: String, context: Context, timeoutMs: Long = 6000L): List<TerminalLine>? {
        val s = ensureSession(context) ?: return null
        val bytes = (command + "\n").toByteArray(Charsets.UTF_8)
        val before = snapshot(s)
        s.write(bytes, 0, bytes.size)

        // Sondeo hasta que la salida se estabilice (o timeout).
        var last = snapshot(s)
        var stable = 0L
        val deadline = System.currentTimeMillis() + timeoutMs
        while (stable < 400L && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(120) } catch (_: InterruptedException) { }
            val current = snapshot(s)
            if (current == last) stable += 120L else { stable = 0L; last = current }
        }

        // Delta respecto a la instantánea previa (el transcript crece de forma apendicular).
        val delta = if (last.size > before.size && last.subList(0, before.size) == before) {
            last.subList(before.size, last.size)
        } else {
            last
        }.dropLastWhile { it.isBlank() }

        if (delta.isEmpty()) return emptyList()

        return delta.mapIndexed { idx, text ->
            val t = text.trim()
            val type = when {
                idx == 0 && text.contains(command) -> LineType.INPUT
                t.endsWith("$") || t.endsWith("#") -> LineType.PROMPT
                t.contains("not found", ignoreCase = true) ||
                    t.contains("Permission denied", ignoreCase = true) ||
                    t.contains("No such file", ignoreCase = true) -> LineType.ERROR
                else -> LineType.OUTPUT
            }
            TerminalLine(text, type)
        }
    }

    /** Texto del transcript del emulador, línea a línea (sin blanks finales). */
    private fun snapshot(s: TerminalSession): List<String> {
        val text = try {
            s.emulator.screen.transcriptTextWithFullLinesJoined
        } catch (e: Exception) {
            ""
        }
        return text.split("\n").map { it.trimEnd() }
    }

    /** Indica si el motor PTY nativo está disponible en este dispositivo. */
    fun isAvailable(context: Context): Boolean = ensureSession(context) != null
}
