package com.example.service

import android.content.Context
import com.example.model.LineType
import com.example.model.TerminalLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Motor de comandos de TerminalHouse.
 *
 * FASE 1 (anti-simulación): NO existe dispatcher que fabrique respuestas.
 * Todo comando del usuario se envía tal cual al PTY real:
 *
 *   TerminalEngine -> PtyBridge -> TerminalSession (PTY, termux-app GPLv3)
 *     -> PRoot real -> rootfs Ubuntu -> /bin/bash --login
 *
 * Las salidas que ve el usuario provienen EXCLUSIVAMENTE del shell real.
 */
object TerminalEngine {

    fun createInitialSessionLines(context: Context): List<TerminalLine> = listOf(
        TerminalLine("[TerminalHouse] Motor: PTY nativo (termux-app GPLv3) + PRoot + rootfs Ubuntu 24.04", LineType.SYSTEM),
        TerminalLine("[TerminalHouse] Preparando el entorno real (bootstrap). Este proceso es real: descarga, SHA256, extracción y verificación.", LineType.SYSTEM)
    )

    suspend fun executeCommand(
        command: String,
        currentDir: String,
        context: Context,
        sessionId: String = "term-1"
    ): CommandResult = withContext(Dispatchers.IO) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return@withContext CommandResult(emptyList(), currentDir)
        }

        // 'clear' es un comando real del shell; además la UI limpia su lista.
        val shouldClear = trimmed == "clear"

        val lines = PtyBridge.runCommand(sessionId, trimmed, context)
            ?: listOf(
                // Task 6: el fallo JAMÁS queda ciego — incluye el motivo real registrado
                // por PtyBridge.ensureSession (excepción, PRoot ausente o rootfs no listo).
                TerminalLine(
                    "[motor] Sesión no disponible. Causa: ${PtyBridge.lastFailureReason(sessionId)}. Estado real del bootstrap: ${describe(LinuxBootstrap.status.value)}",
                    LineType.ERROR
                )
            )

        CommandResult(lines, currentDir, shouldClear)
    }

    fun describe(status: LinuxBootstrap.Status): String = when (status) {
        is LinuxBootstrap.Status.Downloading ->
            "descargando rootfs ${status.downloadedBytes / (1024 * 1024)} MB de ${if (status.totalBytes > 0) "${status.totalBytes / (1024 * 1024)} MB" else "?"}"
        is LinuxBootstrap.Status.VerifyingHash -> "verificando SHA256"
        is LinuxBootstrap.Status.Extracting -> "extrayendo (${status.processed} entradas)"
        is LinuxBootstrap.Status.Hardening -> "configurando rootfs"
        is LinuxBootstrap.Status.Checking -> "verificando rootfs"
        is LinuxBootstrap.Status.Ready -> "rootfs listo (${status.info.osPrettyName}, ${status.info.dpkgPackages} paquetes dpkg)"
        is LinuxBootstrap.Status.Failed -> "FALLÓ: ${status.reason}"
        LinuxBootstrap.Status.NotStarted -> "sin iniciar (pulsa bootstrap o reinicia la app)"
    }
}

data class CommandResult(
    val lines: List<TerminalLine>,
    val newDir: String,
    val shouldClear: Boolean = false
)
