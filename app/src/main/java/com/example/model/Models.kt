package com.example.model

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.OUTPUT,
    val colorHex: String? = null
)

enum class LineType {
    PROMPT,
    INPUT,
    OUTPUT,
    ERROR,
    SYSTEM,
    ASCII_ART
}

data class TerminalSession(
    val id: String,
    val title: String,
    val lines: List<TerminalLine> = emptyList(),
    val workingDir: String = "~",
    val history: List<String> = emptyList(),
    val historyIndex: Int = -1
)

data class SystemStats(
    val cpuUsagePercent: Int,
    val ramUsedPercent: Int,
    val ramTotalMb: Long,
    val ramUsedMb: Long,
    val ramFreeMb: Long,
    val storageUsedPercent: Int,
    val storageTotalGb: Double,
    val storageUsedGb: Double,
    val osVersion: String = "24.04.5 LTS",
    val architecture: String = "aarch64",
    val rootfsSizeMb: Double = 238.77,
    val processCount: Int = 4,
    val pid: Int = 4827,
    val kernelVersion: String = "5.15.148-android13-8-00001",
    val uptimeMinutes: Long = 6,
    val packagesCount: Int = 52
)

data class AiMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedCommand: String? = null
)

data class ProjectItem(
    val id: String,
    val name: String,
    val description: String,
    val path: String,
    val language: String,
    val lastModified: String
)

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: String,
    val permissions: String = "rw-r--r--"
)

data class PackageItem(
    val name: String,
    val version: String,
    val description: String,
    val isInstalled: Boolean = true,
    val size: String = "1.2 MB"
)
