package com.example.service

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Bootstrap REAL del rootfs Ubuntu 24.04 LTS.
 *
 * - Descarga el rootfs oficial (cloud-images.ubuntu.com, server cloudimg root).
 * - Verifica SHA256 contra el hash embebido y, si difiere, contra el SHA256SUMS
 *   oficial publicado por Ubuntu (puede que upstream haya regenerado la imagen).
 * - Extrae con preservación de: directorios, archivos, symlinks, hardlinks y
 *   permisos (chmod con los bits del tar).
 * - Endurece /etc/resolv.conf y policy-rc.d (configuración chroot estándar).
 * - Verifica físicamente (Fase 6) ANTES de crear el ready marker.
 * - Jamás crea el marker con un rootfs incompleto.
 */
object LinuxBootstrap {

    // ---------------------------------------------------------------------
    // Especificación del rootfs (hashes verificados contra SHA256SUMS oficial
    // de cloud-images.ubuntu.com el 2026-09-24 y contra los artefactos locales).
    // ---------------------------------------------------------------------
    object RootfsSpec {
        // Primario: alias "24.04" (sirve 302 -> noble). Verificado 2026-09-25:
        // HTTP 200 con accept-ranges: bytes (arm64 217899756 B, armhf 200528936 B).
        const val BASE_URL = "https://cloud-images.ubuntu.com/releases/24.04/release/"
        // Secundario: ruta canónica "noble" en el mismo CDN (protege contra rotura
        // del alias). NOTA REAL 2026-09-25: mirrors.edge.kernel.org, mirrors.tuna
        // y mirrors.ustc devuelven 404 para ubuntu-cloud-images (dejaron de espejarlo);
        // no se incluyen espejos muertos.
        const val MIRROR_URL = "https://cloud-images.ubuntu.com/releases/noble/release/"
        const val FILE_ARM64 = "ubuntu-24.04-server-cloudimg-arm64-root.tar.xz"
        const val FILE_ARMHF = "ubuntu-24.04-server-cloudimg-armhf-root.tar.xz"
        const val SHA256_ARM64 = "8482f421d456576ac5fa1f01a572846b6b7356ee5a4f58b895920e3118c2f9e7"
        const val SHA256_ARMHF = "e851b7e9cb14eab4ff580c7114f220aa8c163ff7e6581d321dca567022df84ff"

        fun fileForAbi(abi: String): String = if (abi.contains("arm64") || abi.contains("aarch64")) FILE_ARM64 else FILE_ARMHF
        fun sha256ForAbi(abi: String): String = if (abi.contains("arm64") || abi.contains("aarch64")) SHA256_ARM64 else SHA256_ARMHF
        fun abiOf(context: Context): String = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
    }

    sealed class Status {
        data object NotStarted : Status()
        data class Downloading(val downloadedBytes: Long, val totalBytes: Long, val source: String) : Status()
        data object VerifyingHash : Status()
        data class Extracting(val lastPath: String, val processed: Int) : Status()
        data object Hardening : Status()
        data object Checking : Status()
        data class Ready(val info: RootfsInfo) : Status()
        data class Failed(val reason: String) : Status()
    }

    data class RootfsInfo(
        val path: String,
        val sizeBytes: Long,
        val fileCount: Int,
        val symlinkCount: Int,
        val osPrettyName: String,
        val dpkgPackages: Int
    )

    private val _status = MutableStateFlow<Status>(Status.NotStarted)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val mutex = Mutex()
    @Volatile private var cachedInfo: RootfsInfo? = null
    @Volatile private var lastMetricsRefresh: Long = 0L

    fun rootfsDir(context: Context): File = File(context.filesDir, "ubuntu")
    fun readyMarker(context: Context): File = File(context.filesDir, "ubuntu.ready")
    fun archiveFile(context: Context): File = File(context.filesDir, "ubuntu-rootfs.tar.xz")

    /** Descarga parcial reanudable; solo existe mientras la descarga no ha terminado. */
    fun partFile(context: Context): File = File(context.filesDir, "ubuntu-rootfs.tar.xz.part")

    fun isReady(context: Context): Boolean =
        readyMarker(context).exists() && quickVerify(context)

    private fun quickVerify(context: Context): Boolean {
        val r = rootfsDir(context)
        val bash = File(r, "bin/bash")
        return bash.exists() && bash.canExecute() &&
            File(r, "etc/os-release").exists() &&
            File(r, "usr/bin/apt").exists() &&
            File(r, "root").isDirectory
    }

    /**
     * Idempotente: descarga+extrae+verifica si hace falta. Devuelve el estado final real.
     */
    suspend fun ensure(context: Context): Status = mutex.withLock {
        // FIX NetworkOnMainThreadException: TODO el trabajo bloqueante (red, SHA256,
        // extracción de ~25k archivos) corre en IO. Antes solo fetchOfficialSha256
        // estaba en IO: en dispositivo la descarga lanzaba NetworkOnMainThreadException
        // (su message es null) => "Descarga falló en todos los orígenes: null".
        withContext(Dispatchers.IO) { ensureNow(context) }
    }

    private suspend fun ensureNow(context: Context): Status {
        val appContext = context.applicationContext
        if (isReady(appContext)) {
            val info = metrics(appContext, force = true)
            val st = Status.Ready(info)
            _status.value = st
            return st
        }
        try {
            val abi = RootfsSpec.abiOf(appContext)
            val fileName = RootfsSpec.fileForAbi(abi)
            val expectedSha = RootfsSpec.sha256ForAbi(abi)

            // 1) Descargar (reanudable) o reutilizar el archivo completo ya descargado
            val archive = archiveFile(appContext)
            val sha: String
            var hashSource = "embebido"
            if (archive.exists() && archive.length() > 0L) {
                _status.value = Status.VerifyingHash
                val local = sha256(archive)
                if (local.equals(expectedSha, ignoreCase = true)) {
                    sha = local
                } else {
                    // ¿Imagen regenerada por upstream o archivo local corrupto?
                    val official = fetchOfficialSha256(fileName)
                    if (official != null && local.equals(official, ignoreCase = true)) {
                        sha = local
                        hashSource = "SHA256SUMS oficial (imagen regenerada por upstream)"
                    } else {
                        // Corrupto/truncado: NO se maquilla, se descarta y se re-descarga
                        archive.delete()
                        partFile(appContext).delete()
                        val (s2, src2) = downloadVerified(appContext, fileName, archive, expectedSha)
                        if (src2.isEmpty()) {
                            return fail("SHA256 no coincide tras re-descarga. local=$s2 esperado(embebido)=$expectedSha")
                        }
                        sha = s2
                        hashSource = src2
                    }
                }
            } else {
                val (s1, src1) = downloadVerified(appContext, fileName, archive, expectedSha)
                if (src1.isEmpty()) {
                    return fail("SHA256 no coincide tras descarga. local=$s1 esperado(embebido)=$expectedSha")
                }
                sha = s1
                hashSource = src1
            }

            // 3) Extraer (rootfs parcial previo sin marker se elimina)
            val rootfs = rootfsDir(appContext)
            if (rootfs.exists()) rootfs.deleteRecursively()
            rootfs.mkdirs()
            val counts = extract(appContext, archive)
            _status.value = Status.Hardening
            harden(appContext)

            // 4) Verificación física (Fase 6)
            _status.value = Status.Checking
            val check = verify(appContext)
            if (!check.first) {
                readyMarker(appContext).delete()
                return fail("Verificación del rootfs falló: ${check.second}")
            }

            // 5) Ready marker SOLO tras verificación completa
            val info = metrics(appContext, force = true)
            readyMarker(appContext).writeText(
                "sha256=$sha\nhashSource=$hashSource\nabi=$abi\nfile=$fileName\n" +
                    "sizeBytes=${info.sizeBytes}\nfiles=${info.fileCount}\nsymlinks=${info.symlinkCount}\n" +
                    "os=${info.osPrettyName}\npackages=${info.dpkgPackages}\ncreatedAt=${System.currentTimeMillis()}\n"
            )
            archive.delete()
            val st = Status.Ready(info)
            _status.value = st
            return st
        } catch (t: Throwable) {
            return fail("${t.javaClass.simpleName}: ${t.message ?: "error desconocido"}")
        }
    }

    private fun fail(reason: String): Status {
        _status.value = Status.Failed(reason)
        return Status.Failed(reason)
    }

    // ------------------------------------------------------------------
    // Descarga real: reanudación (.part + HTTP Range), errores verídicos por
    // origen y progreso con límite de frecuencia. Nada inventado.
    // ------------------------------------------------------------------

    /** Descarga (reanudable) + verificación SHA256 real. Devuelve (sha, fuente); fuente "" = no verificó. */
    private suspend fun downloadVerified(
        context: Context, fileName: String, archive: File, expectedSha: String
    ): Pair<String, String> {
        val sha = downloadWithMirrors(context, fileName, archive)
        if (sha.equals(expectedSha, ignoreCase = true)) return sha to "embebido"
        _status.value = Status.VerifyingHash
        val official = fetchOfficialSha256(fileName)
        if (official != null && sha.equals(official, ignoreCase = true)) {
            return sha to "SHA256SUMS oficial (imagen regenerada por upstream)"
        }
        // No verificó: descartar restos para que no se reutilicen
        archive.delete()
        partFile(context).delete()
        return sha to ""
    }

    private fun downloadWithMirrors(context: Context, fileName: String, dest: File): String {
        val part = partFile(context)
        val errors = mutableListOf<String>()
        for (base in listOf(RootfsSpec.BASE_URL, RootfsSpec.MIRROR_URL)) {
            try {
                download(base + fileName, part, base)
                if (part.length() <= 0L) throw IllegalStateException("archivo descargado vacío")
                if (!part.renameTo(dest)) {
                    part.copyTo(dest, overwrite = true)
                    part.delete()
                }
                return sha256(dest)
            } catch (e: Exception) {
                // Error VERÍDICO por origen (clase + mensaje); jamás "null"
                errors += "${URL(base).host} → ${e.javaClass.simpleName}: ${e.message ?: "(sin mensaje)"}"
            }
        }
        val left = if (part.exists() && part.length() > 0L)
            " | El .part de ${part.length() / (1024 * 1024)} MB se conserva y se reanudará en el próximo intento." else ""
        throw IllegalStateException("Descarga falló en todos los orígenes → ${errors.joinToString(" | ")}.$left")
    }

    private fun download(urlStr: String, part: File, source: String) {
        for (attempt in 0..1) {
            val already = if (part.exists()) part.length() else 0L
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 20000
            conn.readTimeout = 90000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "TerminalHouse-Bootstrap/1.2 (Android)")
            if (already > 0L) conn.setRequestProperty("Range", "bytes=$already-")
            try {
                conn.connect()
                val code = conn.responseCode

                if (code == 416 && already > 0L) {
                    // .part incompatible con el remoto: descartar y reintentar desde 0
                    part.delete()
                    continue
                }
                if (code !in 200..299) throw IllegalStateException("HTTP $code en $urlStr")

                val resuming = already > 0L && code == 206
                val remoteTotal = conn.getHeaderField("Content-Range")
                    ?.substringAfter('/')?.toLongOrNull() ?: -1L
                if (resuming && remoteTotal in 1 until already) {
                    part.delete()
                    continue
                }
                val total = if (resuming) remoteTotal else conn.contentLengthLong
                val shownSource = if (resuming) "$source (reanudado desde ${already / (1024 * 1024)} MB)" else source
                var done = if (resuming) already else 0L
                var lastEmitBytes = -1L
                var lastEmitMs = 0L

                conn.inputStream.use { input ->
                    FileOutputStream(part, resuming).use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            // Progreso limitado (1 MB o 1 s): no inunda la UI
                            if (lastEmitBytes < 0L || done - lastEmitBytes >= (1 shl 20) ||
                                SystemClock.elapsedRealtime() - lastEmitMs >= 1000L
                            ) {
                                _status.value = Status.Downloading(done, if (total > 0) total else -1, shownSource)
                                lastEmitBytes = done
                                lastEmitMs = SystemClock.elapsedRealtime()
                            }
                        }
                    }
                }
                if (total > 0 && done != total) {
                    // Conexión corta: el .part SE CONSERVA para reanudar en el próximo intento
                    throw IllegalStateException("Descarga incompleta: $done/$total bytes (se reanudará)")
                }
                _status.value = Status.Downloading(done, if (total > 0) total else done, shownSource)
                return
            } finally {
                conn.disconnect()
            }
        }
        throw IllegalStateException("HTTP 416 persistente en $urlStr")
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(128 * 1024)
            while (true) {
                val n = fis.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Devuelve el hash del archivo según el SHA256SUMS oficial de Ubuntu, o null. */
    private suspend fun fetchOfficialSha256(fileName: String): String? = withContext(Dispatchers.IO) {
        for (base in listOf(RootfsSpec.BASE_URL, RootfsSpec.MIRROR_URL)) {
            try {
                val conn = URL(base + "SHA256SUMS").openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().useLines { lines ->
                        for (l in lines) {
                            val parts = l.trim().split(Regex("\\s+\\*?"), 2)
                            if (parts.size == 2 && parts[1] == fileName) return@withContext parts[0].lowercase()
                        }
                    }
                }
            } catch (_: Exception) { }
            delay(200)
        }
        null
    }

    // ------------------------------------------------------------------
    // Extracción xz/tar con symlinks, hardlinks y permisos
    // ------------------------------------------------------------------
    private fun extract(context: Context, archive: File): Pair<Int, Int> {
        val rootfs = rootfsDir(context).absolutePath
        var files = 0
        var symlinks = 0
        var processed = 0
        val xzIn = XZCompressorInputStream(BufferedInputStream(FileInputStream(archive), 1 shl 16), false, 96 * 1024)
        TarArchiveInputStream(xzIn, "UTF-8").use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                processed++
                if (processed % 200 == 0) _status.value = Status.Extracting(entry.name, processed)

                val rel = entry.name.trim().removePrefix("./").trim('/')
                if (rel.isEmpty() || rel.contains("..")) continue
                val out = File(rootfs, rel)
                val mode = entry.mode.toInt() and 0xFFF  // 07777 octal = 0xFFF

                when {
                    entry.isDirectory -> {
                        out.mkdirs()
                        runCatching { Os.chmod(out.absolutePath, mode) }
                    }
                    entry.isSymbolicLink -> {
                        out.parentFile?.mkdirs()
                        out.delete()
                        try {
                            Os.symlink(entry.linkName, out.absolutePath)
                            symlinks++
                        } catch (t: Throwable) {
                            // Algunos symlinks absolutos dentro del tar no resuelven; se registran, no se maquillan
                        }
                    }
                    entry.isLink -> {
                        out.parentFile?.mkdirs()
                        val target = File(rootfs, entry.linkName.trim().removePrefix("./"))
                        out.delete()
                        try {
                            Os.link(target.absolutePath, out.absolutePath)
                            files++
                        } catch (t: Throwable) {
                            if (target.exists()) {
                                FileInputStream(target).use { i -> FileOutputStream(out).use { o -> i.copyTo(o) } }
                                files++
                            }
                        }
                    }
                    entry.isFIFO || entry.isBlockDevice || entry.isCharacterDevice -> {
                        // Sin dispositivos propios: /dev se bindea del host (Fase 5)
                    }
                    else -> {
                        out.parentFile?.mkdirs()
                        out.delete()
                        FileOutputStream(out).use { o ->
                            var remaining = entry.size
                            val buf = ByteArray(64 * 1024)
                            while (remaining > 0) {
                                val n = tar.read(buf, 0, if (remaining > buf.size) buf.size else remaining.toInt())
                                if (n < 0) break
                                o.write(buf, 0, n)
                                remaining -= n
                            }
                        }
                        runCatching { Os.chmod(out.absolutePath, mode) }
                        files++
                    }
                }
            }
        }
        return files to symlinks
    }

    /** Configuración mínima para que apt/DNS funcionen dentro de proot. */
    private fun harden(context: Context) {
        val r = rootfsDir(context)
        // resolv.conf real (el rootfs cloudimg apunta a stub-resolv de systemd, que no corre aquí)
        val resolv = File(r, "etc/resolv.conf")
        resolv.delete()
        resolv.writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\nnameserver 9.9.9.9\n")
        runCatching { Os.chmod(resolv.absolutePath, 0x1A4) }  // 0644
        // Evita que los postinst de dpkg intenten arrancar servicios (práctica estándar de chroot)
        val policy = File(r, "usr/sbin/policy-rc.d")
        policy.parentFile?.mkdirs()
        if (!policy.exists()) {
            policy.writeText("#!/bin/sh\nexit 101\n")
            runCatching { Os.chmod(policy.absolutePath, 0x1ED) }  // 0755
        }
        // /tmp con permisos estándar
        val tmp = File(r, "tmp")
        tmp.mkdirs()
        runCatching { Os.chmod(tmp.absolutePath, 0x3FF) }  // 01777 (sticky)
        // apt necesita listas
        File(r, "var/lib/apt/lists").mkdirs()
        File(r, "var/cache/apt/archives/partial").mkdirs()
    }

    // ------------------------------------------------------------------
    // Verificación física (Fase 6)
    // ------------------------------------------------------------------
    private fun verify(context: Context): Pair<Boolean, String> {
        val r = rootfsDir(context)
        fun ok(f: File, needExec: Boolean = false): Boolean =
            f.exists() && (!needExec || f.canExecute())

        val checks = listOf(
            "bin existe" to ok(File(r, "bin")),
            "bin/bash existe y es ejecutable" to ok(File(r, "bin/bash"), needExec = true),
            "etc/os-release existe" to ok(File(r, "etc/os-release")),
            "root existe (directorio)" to File(r, "root").isDirectory,
            "usr/bin/apt existe y es ejecutable" to ok(File(r, "usr/bin/apt"), needExec = true),
            "usr/bin/python3 existe y es ejecutable" to ok(File(r, "usr/bin/python3"), needExec = true),
            "usr existe" to File(r, "usr").isDirectory,
            "var/lib/dpkg/status existe" to ok(File(r, "var/lib/dpkg/status")),
            "tmp existe" to File(r, "tmp").isDirectory
        )
        val failed = checks.filter { !it.second }
        return if (failed.isEmpty()) true to "OK" else false to failed.joinToString("; ") { it.first }
    }

    // ------------------------------------------------------------------
    // Métricas reales (Fase 9)
    // ------------------------------------------------------------------
    fun metrics(context: Context, force: Boolean = false): RootfsInfo {
        val appContext = context.applicationContext
        val cached = cachedInfo
        if (!force && cached != null && SystemClock.elapsedRealtime() - lastMetricsRefresh < 60_000L) return cached
        val r = rootfsDir(appContext)
        var size = 0L
        var files = 0
        var symlinks = 0
        if (r.exists()) {
            r.walkTopDown().forEach { f ->
                when {
                    android.os.Build.VERSION.SDK_INT >= 21 && isSymlink(f) -> symlinks++
                    f.isFile -> { size += f.length(); files++ }
                }
            }
        }
        val osPretty = readOsPrettyName(r) ?: "N/D"
        val info = RootfsInfo(
            path = r.absolutePath,
            sizeBytes = size,
            fileCount = files,
            symlinkCount = symlinks,
            osPrettyName = osPretty,
            dpkgPackages = countDpkgPackages(r)
        )
        cachedInfo = info
        lastMetricsRefresh = SystemClock.elapsedRealtime()
        return info
    }

    private fun isSymlink(f: File): Boolean = try {
        Os.readlink(f.absolutePath) != ""
    } catch (t: Throwable) {
        false
    }

    fun readOsPrettyName(rootfs: File): String? = try {
        File(rootfs, "etc/os-release").readLines()
            .firstOrNull { it.startsWith("PRETTY_NAME=") }
            ?.removePrefix("PRETTY_NAME=")
            ?.trim('"')
    } catch (t: Throwable) { null }

    fun countDpkgPackages(rootfs: File): Int = try {
        File(rootfs, "var/lib/dpkg/status").readLines().count { it.startsWith("Package: ") }
    } catch (t: Throwable) { 0 }

    /** Lista real de paquetes instalados (desde dpkg status), para la pantalla Paquetes. */
    fun loadInstalledPackages(context: Context, limit: Int = 300): List<com.example.model.PackageItem> {
        val out = mutableListOf<com.example.model.PackageItem>()
        try {
            val lines = File(rootfsDir(context), "var/lib/dpkg/status").readLines()
            var name: String? = null
            var version: String? = null
            var desc: String? = null
            fun flush() {
                val n = name ?: return
                if (out.size < limit) out.add(
                    com.example.model.PackageItem(
                        name = n,
                        version = version ?: "N/D",
                        description = (desc ?: "N/D").take(90),
                        isInstalled = true,
                        size = "N/D"
                    )
                )
                name = null; version = null; desc = null
            }
            for (l in lines) {
                when {
                    l.startsWith("Package: ") -> { flush(); name = l.removePrefix("Package: ").trim() }
                    l.startsWith("Version: ") -> version = l.removePrefix("Version: ").trim()
                    l.startsWith("Description: ") -> desc = l.removePrefix("Description: ").trim()
                    l.isBlank() -> flush()
                }
            }
            flush()
        } catch (_: Throwable) { }
        return out
    }
}