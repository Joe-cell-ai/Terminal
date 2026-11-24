package dev.linuxterminal.runtime

import android.content.Context
import android.os.Build
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/** Installs the separately published ARM64 PRoot runtime; the APK stays small. */
class ProotRuntimeInstaller(context: Context) {
    private val files = context.filesDir.canonicalFile
    private val destination = File(files, "runtime")
    private val rootfs = File(files, "debian")
    private val archive = File(context.cacheDir, "proot-runtime-aarch64.tar.gz")
    private val base = "https://github.com/Joe-cell-ai/Terminal/releases/download/proot-runtime/"
    data class Progress(val message: String, val percent: Int, val installed: Boolean = false)

    fun isInstalled(): Boolean =
        File(destination, "proot").let { it.isFile && it.canExecute() } &&
            File(destination, "libexec/proot/loader").isFile &&
            File(destination, "lib/libtalloc.so.2.5.0").isFile &&
            File(destination, "lib/libandroid-shmem.so").isFile

    suspend fun install(onProgress: (Progress) -> Unit) = withContext(Dispatchers.IO) {
        require(Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) {
            "The PRoot runtime requires a 64-bit ARM Android device."
        }
        val free = StatFs(files.absolutePath).let { it.availableBlocksLong * it.blockSizeLong }
        require(free >= 20L * 1024 * 1024) { "At least 20 MB of free storage is required for the PRoot runtime." }
        onProgress(Progress("Downloading PRoot runtime checksum…", 0))
        val checksumText = readText(URL(base + "proot-runtime-aarch64.tar.gz.sha256")).trim()
        val expected = checksumText.substringBefore(Regex("\\s").find(checksumText)?.value ?: " ")
        require(expected.matches(Regex("[a-fA-F0-9]{64}"))) { "The runtime checksum manifest is invalid." }
        download(URL(base + "proot-runtime-aarch64.tar.gz")) { done, total ->
            onProgress(Progress("Downloading PRoot… ${format(done)} / ${format(total)}",
                if (total > 0) (done * 85 / total).toInt().coerceIn(1, 85) else 5))
        }
        require(sha256(archive).equals(expected, true)) {
            "PRoot runtime checksum verification failed. The archive was rejected."
        }
        onProgress(Progress("Verifying and installing PRoot runtime…", 90))
        val staging = File(files, "runtime-installing")
        staging.deleteRecursively()
        staging.mkdirs()
        try {
            extract(archive, staging)
            val payload = File(staging, "proot-runtime")
            val binary = File(payload, "proot")
            val loader = File(payload, "libexec/proot/loader")
            require(binary.isFile && loader.isFile &&
                File(payload, "lib/libtalloc.so.2.5.0").isFile &&
                File(payload, "lib/libandroid-shmem.so").isFile) { "Runtime package is incomplete." }
            require(isAarch64Elf(binary)) { "The downloaded PRoot executable is not ARM64." }
            binary.setExecutable(true, true)
            loader.setExecutable(true, true)
            File(payload, "lib/libandroid-shmem.so").setExecutable(true, true)
            val old = File(files, "runtime-old")
            old.deleteRecursively()
            if (destination.exists()) require(destination.renameTo(old)) { "Could not stage the existing runtime." }
            if (!payload.renameTo(destination)) {
                old.renameTo(destination)
                error("Could not activate the PRoot runtime.")
            }
            old.deleteRecursively()
            require(isInstalled()) { "PRoot runtime validation failed after installation." }
            onProgress(Progress("PRoot runtime installed", 100, true))
            archive.delete()
        } catch (t: Throwable) {
            staging.deleteRecursively()
            throw t
        }
    }

    private fun extract(file: File, target: File) {
        val root = target.canonicalFile.toPath()
        TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(FileInputStream(file)))).use { tar ->
            var count = 0
            var expanded = 0L
            var entry = tar.nextTarEntry
            val buffer = ByteArray(32 * 1024)
            while (entry != null) {
                require(++count <= 5000) { "Runtime archive has too many entries." }
                val raw = entry.name
                require(!raw.startsWith("/") && !raw.split('/').contains("..")) { "Unsafe runtime archive path." }
                val out = File(target, raw).canonicalFile
                require(out.toPath().startsWith(root) && out.toPath() != root) { "Runtime archive path escaped its install directory." }
                when {
                    entry.isDirectory -> require(out.mkdirs() || out.isDirectory) { "Could not create runtime directory." }
                    entry.isSymbolicLink -> {
                        val link = File(entry.linkName)
                        require(!link.isAbsolute && out.parentFile != null) { "Unsafe runtime symbolic link." }
                        val resolved = out.parentFile.toPath().resolve(link.toPath()).normalize()
                        require(resolved.startsWith(root)) { "Runtime symbolic link escaped its install directory." }
                        out.parentFile.mkdirs()
                        require(!java.nio.file.Files.exists(out.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) { "Duplicate runtime path." }
                        java.nio.file.Files.createSymbolicLink(out.toPath(), link.toPath())
                    }
                    entry.isFile -> {
                        expanded += entry.size
                        require(entry.size >= 0 && expanded <= 64L * 1024 * 1024) { "Runtime archive exceeds its size limit." }
                        out.parentFile?.mkdirs()
                        require(!java.nio.file.Files.exists(out.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) { "Duplicate runtime path." }
                        FileOutputStream(out).use { output ->
                            var n: Int
                            while (tar.read(buffer).also { n = it } != -1) output.write(buffer, 0, n)
                        }
                        out.setExecutable((entry.mode and 73) != 0, true)
                    }
                    else -> error("Unsupported runtime archive entry.")
                }
                entry = tar.nextTarEntry
            }
        }
    }

    private fun readText(url: URL): String = connection(url).use { c ->
        require(c.responseCode in 200..299) { "Runtime checksum request failed: HTTP ${c.responseCode}" }
        c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun download(url: URL, progress: (Long, Long) -> Unit) {
        connection(url).use { c ->
            require(c.responseCode in 200..299) { "Runtime download failed: HTTP ${c.responseCode}" }
            FileOutputStream(archive).use { out ->
                c.inputStream.use { input ->
                    val b = ByteArray(32 * 1024)
                    var n: Int
                    var done = 0L
                    while (input.read(b).also { n = it } != -1) {
                        out.write(b, 0, n); done += n; progress(done, c.contentLengthLong)
                    }
                    out.fd.sync()
                }
            }
            require(archive.length() > 0) { "Downloaded runtime archive is empty." }
        }
    }

    private fun connection(url: URL): HttpURLConnection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 20_000
        readTimeout = 60_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Terminal-Android/0.1")
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val b = ByteArray(65_536)
            var n: Int
            while (input.read(b).also { n = it } != -1) md.update(b, 0, n)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isAarch64Elf(file: File): Boolean = FileInputStream(file).use { input ->
        val h = ByteArray(20)
        if (input.read(h) != 20) false else
            h[0] == 0x7f.toByte() && h[1] == 'E'.code.toByte() &&
            h[2] == 'L'.code.toByte() && h[3] == 'F'.code.toByte() &&
            h[4] == 2.toByte() && h[5] == 1.toByte() &&
            (h[18].toInt() and 255) == 183 && h[19] == 0.toByte()
    }

    private fun format(value: Long): String =
        if (value <= 0) "unknown size" else String.format(Locale.US, "%.1f MB", value / 1_000_000.0)
}
