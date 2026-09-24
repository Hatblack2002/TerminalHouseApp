package com.example.service

import android.content.Context
import com.example.model.LineType
import com.example.model.TerminalLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object TerminalEngine {

    fun createInitialSessionLines(context: Context): List<TerminalLine> {
        val lines = mutableListOf<TerminalLine>()
        val stats = SystemMonitor.getRealStats(context)

        // Command 1: neofetch
        lines.add(TerminalLine("root@ubuntu:~# neofetch", LineType.INPUT))

        // Neofetch ASCII art and system info
        val ramUsedGi = String.format("%.1f", stats.ramUsedMb / 1024.0).replace('.', ',')
        val ramTotalGi = String.format("%.1f", stats.ramTotalMb / 1024.0).replace('.', ',')

        lines.add(TerminalLine("            .:+oooo+:.           root@ubuntu", LineType.ASCII_ART))
        lines.add(TerminalLine("        .:+oooooooooooo+:.       OS: Ubuntu 24.04.5 LTS aarch64", LineType.ASCII_ART))
        lines.add(TerminalLine("      .:+oooooooooooooooo+:.     Host: Linux 5.15.148-android13-8-00001", LineType.ASCII_ART))
        lines.add(TerminalLine("    .:oooooooooooooooooooooo:.   Uptime: ${stats.uptimeMinutes} mins", LineType.ASCII_ART))
        lines.add(TerminalLine("    +oooooooooooooooooooooooo+   Packages: ${stats.packagesCount} (dpkg)", LineType.ASCII_ART))
        lines.add(TerminalLine("    +oooooooooooooooooooooooo+   Shell: bash 5.2.21", LineType.ASCII_ART))
        lines.add(TerminalLine("    .:oooooooooooooooooooooo:.   CPU: (8) ARM Cortex-A77 (2.84GHz)", LineType.ASCII_ART))
        lines.add(TerminalLine("      .:+oooooooooooooooo+:.     Memory: ${stats.ramUsedMb}MiB / ${stats.ramTotalMb}MiB", LineType.ASCII_ART))
        lines.add(TerminalLine("        .:+oooooooooooo+:.       ■■■■■■■■", LineType.ASCII_ART))
        lines.add(TerminalLine("            .:+oooo+:.", LineType.ASCII_ART))
        lines.add(TerminalLine("", LineType.OUTPUT))

        // Command 2: uname -a
        lines.add(TerminalLine("root@ubuntu:~# uname -a", LineType.INPUT))
        lines.add(TerminalLine("Linux ubuntu ${stats.kernelVersion} ${stats.architecture} GNU/Linux", LineType.OUTPUT))
        lines.add(TerminalLine("", LineType.OUTPUT))

        // Command 3: free -h
        lines.add(TerminalLine("root@ubuntu:~# free -h", LineType.INPUT))
        lines.add(TerminalLine("               total        used        free      shared  buff/cache   available", LineType.OUTPUT))
        lines.add(TerminalLine("Mem:          ${ramTotalGi}Gi       ${ramUsedGi}Gi       3,2Gi        12Mi       946Mi       4,0Gi", LineType.OUTPUT))
        lines.add(TerminalLine("Swap:            0B          0B          0B", LineType.OUTPUT))
        lines.add(TerminalLine("", LineType.OUTPUT))

        return lines
    }

    suspend fun executeCommand(
        command: String,
        currentDir: String,
        context: Context
    ): CommandResult = withContext(Dispatchers.IO) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return@withContext CommandResult(emptyList(), currentDir)
        }

        val parts = trimmed.split("\\s+".toRegex())
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        val output = mutableListOf<TerminalLine>()
        var newDir = currentDir

        when (cmd) {
            "clear", "cls" -> {
                return@withContext CommandResult(emptyList(), currentDir, shouldClear = true)
            }
            "neofetch" -> {
                val stats = SystemMonitor.getRealStats(context)
                output.add(TerminalLine("            .:+oooo+:.           root@ubuntu", LineType.ASCII_ART))
                output.add(TerminalLine("        .:+oooooooooooo+:.       OS: Ubuntu 24.04.5 LTS aarch64", LineType.ASCII_ART))
                output.add(TerminalLine("      .:+oooooooooooooooo+:.     Host: Linux 5.15.148-android13-8-00001", LineType.ASCII_ART))
                output.add(TerminalLine("    .:oooooooooooooooooooooo:.   Uptime: ${stats.uptimeMinutes} mins", LineType.ASCII_ART))
                output.add(TerminalLine("    +oooooooooooooooooooooooo+   Packages: ${stats.packagesCount} (dpkg)", LineType.ASCII_ART))
                output.add(TerminalLine("    +oooooooooooooooooooooooo+   Shell: bash 5.2.21", LineType.ASCII_ART))
                output.add(TerminalLine("    .:oooooooooooooooooooooo:.   CPU: (8) ARM Cortex-A77 (2.84GHz)", LineType.ASCII_ART))
                output.add(TerminalLine("      .:+oooooooooooooooo+:.     Memory: ${stats.ramUsedMb}MiB / ${stats.ramTotalMb}MiB", LineType.ASCII_ART))
                output.add(TerminalLine("        .:+oooooooooooo+:.       ■■■■■■■■", LineType.ASCII_ART))
                output.add(TerminalLine("            .:+oooo+:.", LineType.ASCII_ART))
            }
            "uname" -> {
                val stats = SystemMonitor.getRealStats(context)
                if (args.contains("-a")) {
                    output.add(TerminalLine("Linux ubuntu ${stats.kernelVersion} ${stats.architecture} GNU/Linux", LineType.OUTPUT))
                } else if (args.contains("-r")) {
                    output.add(TerminalLine(stats.kernelVersion, LineType.OUTPUT))
                } else if (args.contains("-m")) {
                    output.add(TerminalLine(stats.architecture, LineType.OUTPUT))
                } else {
                    output.add(TerminalLine("Linux", LineType.OUTPUT))
                }
            }
            "free" -> {
                val stats = SystemMonitor.getRealStats(context)
                val ramUsedGi = String.format("%.1f", stats.ramUsedMb / 1024.0).replace('.', ',')
                val ramTotalGi = String.format("%.1f", stats.ramTotalMb / 1024.0).replace('.', ',')
                output.add(TerminalLine("               total        used        free      shared  buff/cache   available", LineType.OUTPUT))
                output.add(TerminalLine("Mem:          ${ramTotalGi}Gi       ${ramUsedGi}Gi       3,2Gi        12Mi       946Mi       4,0Gi", LineType.OUTPUT))
                output.add(TerminalLine("Swap:            0B          0B          0B", LineType.OUTPUT))
            }
            "whoami" -> {
                output.add(TerminalLine("root", LineType.OUTPUT))
            }
            "pwd" -> {
                output.add(TerminalLine(if (currentDir == "~") "/root" else currentDir, LineType.OUTPUT))
            }
            "cd" -> {
                val target = args.firstOrNull() ?: "~"
                newDir = if (target == "~" || target == "/root") "~" else target
            }
            "uptime" -> {
                val stats = SystemMonitor.getRealStats(context)
                output.add(TerminalLine(" 12:30:00 up ${stats.uptimeMinutes} min, 1 user, load average: 0.12, 0.15, 0.18", LineType.OUTPUT))
            }
            "help" -> {
                output.add(TerminalLine("TerminalHouse Shell v0.1.0 (Ubuntu 24.04.5 LTS ARM64)", LineType.SYSTEM))
                output.add(TerminalLine("Available built-in commands:", LineType.OUTPUT))
                output.add(TerminalLine("  neofetch, uname -a, free -h, uptime, top, ps", LineType.OUTPUT))
                output.add(TerminalLine("  ls, cd, pwd, cat, echo, mkdir, rm, df -h", LineType.OUTPUT))
                output.add(TerminalLine("  apt, dpkg, git, python3, node, ping", LineType.OUTPUT))
                output.add(TerminalLine("  clear, help, exit", LineType.OUTPUT))
            }
            "apt", "apt-get" -> {
                val subCmd = args.firstOrNull() ?: ""
                when (subCmd) {
                    "update" -> {
                        output.add(TerminalLine("Hit:1 http://ports.ubuntu.com/ubuntu-ports noble InRelease", LineType.OUTPUT))
                        output.add(TerminalLine("Hit:2 http://ports.ubuntu.com/ubuntu-ports noble-updates InRelease", LineType.OUTPUT))
                        output.add(TerminalLine("Hit:3 http://ports.ubuntu.com/ubuntu-ports noble-security InRelease", LineType.OUTPUT))
                        output.add(TerminalLine("Reading package lists... Done", LineType.OUTPUT))
                        output.add(TerminalLine("Building dependency tree... Done", LineType.OUTPUT))
                        output.add(TerminalLine("All packages are up to date.", LineType.OUTPUT))
                    }
                    "install" -> {
                        val pkg = args.drop(1).joinToString(" ")
                        output.add(TerminalLine("Reading package lists... Done", LineType.OUTPUT))
                        output.add(TerminalLine("Building dependency tree... Done", LineType.OUTPUT))
                        output.add(TerminalLine("The following NEW packages will be installed: $pkg", LineType.OUTPUT))
                        output.add(TerminalLine("0 upgraded, 1 newly installed, 0 to remove and 0 not upgraded.", LineType.OUTPUT))
                        output.add(TerminalLine("Setting up $pkg (latest)... Done", LineType.OUTPUT))
                    }
                    else -> {
                        output.add(TerminalLine("apt 2.8.1 (aarch64) - Package manager for Ubuntu", LineType.OUTPUT))
                        output.add(TerminalLine("Usage: apt [update | install | search | list]", LineType.OUTPUT))
                    }
                }
            }
            "python", "python3" -> {
                output.add(TerminalLine("Python 3.12.3 (main, Apr 10 2024, 05:33:47) [GCC 13.2.0] on linux", LineType.OUTPUT))
                output.add(TerminalLine("Type \"help\", \"copyright\", \"credits\" or \"license\" for more information.", LineType.OUTPUT))
            }
            "node" -> {
                output.add(TerminalLine("v20.12.2", LineType.OUTPUT))
            }
            "git" -> {
                output.add(TerminalLine("git version 2.43.0", LineType.OUTPUT))
            }
            "df" -> {
                val stats = SystemMonitor.getRealStats(context)
                output.add(TerminalLine("Filesystem     1K-blocks      Used Available Use% Mounted on", LineType.OUTPUT))
                output.add(TerminalLine("/dev/root       62914560  20132659  42781901  32% /", LineType.OUTPUT))
                output.add(TerminalLine("tmpfs            2790400     12288   2778112   1% /dev/shm", LineType.OUTPUT))
            }
            "ps" -> {
                val stats = SystemMonitor.getRealStats(context)
                output.add(TerminalLine("  PID TTY          TIME CMD", LineType.OUTPUT))
                output.add(TerminalLine("    1 ?        00:00:01 systemd", LineType.OUTPUT))
                output.add(TerminalLine("  204 ?        00:00:00 dbus-daemon", LineType.OUTPUT))
                output.add(TerminalLine(" 1209 pts/0    00:00:00 bash", LineType.OUTPUT))
                output.add(TerminalLine(" ${stats.pid} pts/0    00:00:00 ps", LineType.OUTPUT))
            }
            "ls" -> {
                output.add(TerminalLine("bin  boot  dev  etc  home  lib  lib64  media  mnt  opt  proc  root  run  sbin  srv  sys  tmp  usr  var", LineType.OUTPUT))
            }
            else -> {
                // Motor real primero: PTY nativo vía :terminal (libtermux.so, termux-app GPLv3).
                // Solo si el motor nativo no está disponible se conserva el fallback histórico.
                val ptyLines = PtyBridge.runCommand(trimmed, context)
                if (ptyLines != null) {
                    if (ptyLines.isEmpty()) {
                        output.add(TerminalLine("", LineType.OUTPUT))
                    } else {
                        output.addAll(ptyLines)
                    }
                } else {
                    // Try executing using system shell
                    try {
                        val process = ProcessBuilder("/system/bin/sh", "-c", trimmed)
                            .directory(context.filesDir)
                            .redirectErrorStream(true)
                            .start()

                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String? = reader.readLine()
                        var count = 0
                        while (line != null && count < 50) {
                            output.add(TerminalLine(line, LineType.OUTPUT))
                            line = reader.readLine()
                            count++
                        }
                        process.waitFor()
                        if (output.isEmpty() && process.exitValue() != 0) {
                            output.add(TerminalLine("bash: $cmd: command not found", LineType.ERROR))
                        }
                    } catch (e: Exception) {
                        output.add(TerminalLine("bash: $cmd: command not found", LineType.ERROR))
                    }
                }
            }
        }

        CommandResult(output, newDir)
    }
}

data class CommandResult(
    val lines: List<TerminalLine>,
    val newDir: String,
    val shouldClear: Boolean = false
)
