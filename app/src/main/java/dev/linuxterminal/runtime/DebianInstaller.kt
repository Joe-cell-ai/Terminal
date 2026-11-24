package dev.linuxterminal.runtime

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
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
import java.util.Locale

/**
 * Downloads the current Debian Bookworm ARM64 LXC rootfs, verifies its published SHA-256,
 * then safely extracts it into app-private storage. No root access is used.
 */
class DebianInstaller(context: Context) {
    private val root = File(context.filesDir, "debian")
    private val archive = File(context.cacheDir, "debian-bookworm-arm64-rootfs.tar.xz")
    private val baseIndex = "https://images.linuxcontainers.org/images/debian/bookworm/arm64/default/"

    data class Progress(val message: String, val percent: Int, val installed: Boolean = false)

    fun isInstalled(): Boolean = File(root, "etc/debian_version").isFile

    suspend fun install(onProgress: (Progress) -> Unit) = withContext(Dispatchers.IO) {
        if (isInstalled()) {
            onProgress(Progress("Debian is already installed", 100, true))
            return@withContext
        }

        val stat = StatFs(root.parentFile!!.absolutePath)
        val available = stat.availableBlocksLong * stat.blockSizeLong
        require(available >= 1_500_000_000L) {
            "At least 1.5 GB of free internal storage is required for download and extraction."
        }

        val imagePath = latestImagePath()
        val imageUrl = URL(baseIndex + imagePath)
        val sums = readText(URL(imageUrl, "SHA256SUMS"))
        val expected = Regex("(?m)^([a-fA-F0-9]{64})\\s+\\*?rootfs\\.tar\\.xz\\s*$")
            .find(sums)?.groupValues?.get(1)?.lowercase(Locale.US)
            ?: error("The Debian image checksum file did not contain rootfs.tar.xz.")

        onProgress(Progress("Downloading Debian Bookworm ARM64 root filesystem…", 0))
        download(URL(imageUrl, "rootfs.tar.xz")) { downloaded, total ->
            val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 99) else 0
            onProgress(Progress("Downloading Debian… ${formatBytes(downloaded)} / ${formatBytes(total)}", percent))
        }

        onProgress(Progress("Verifying SHA-256 checksum…", 99))
        val actual = sha256(archive)
        require(actual == expected) { "Checksum verification failed. The downloaded archive was rejected." }

        val staging = File(root.parentFile, "debian-installing")
        staging.deleteRecursively()
        staging.mkdirs()
        try {
            onProgress(Progress("Extracting Debian safely…", 99))
            extractTarXz(archive, staging)
            require(File(staging, "etc/debian_version").isFile) {
                "The archive did not contain a valid Debian root filesystem."
            }
            File(root.parentFile, "debian-old").deleteRecursively()
            if (root.exists() && !root.renameTo(File(root.parentFile, "debian-old"))) {
                error("Could not move the previous Debian directory.")
            }
            if (!staging.renameTo(root)) {
                File(root.parentFile, "debian-old").renameTo(root)
                error("Could not finalize the Debian installation.")
            }
            File(root.parentFile, "debian-old").deleteRecursively()
            archive.delete()
            onProgress(Progress("Debian Bookworm ARM64 installed", 100, true))
        } catch (t: Throwable) {
            staging.deleteRecursively()
            throw t
        }
    }

    private fun latestImagePath(): String {
        val html = readText(URL(baseIndex))
        val candidates = Regex("href=\"(\\d{8}_\\d{2}(?::|%3A)\\d{2}/)\"")
            .findAll(html).map { it.groupValues[1] }.toList()
        return candidates.maxOrNull() ?: error("Could not find a Debian image build on the image server.")
    }

    private fun readText(url: URL): String {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "Terminal-Android/0.1")
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun download(url: URL, onProgress: (Long, Long) -> Unit) {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("User-Agent", "Terminal-Android/0.1")
        try {
            connection.connect()
            require(connection.responseCode in 200..299) { "Debian download failed: HTTP ${connection.responseCode}" }
            val total = connection.contentLengthLong
            archive.parentFile?.mkdirs()
            FileOutputStream(archive).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var count: Int
                    var downloaded = 0L
                    while (input.read(buffer).also { count = it } != -1) {
                        output.write(buffer, 0, count)
                        downloaded += count
                        onProgress(downloaded, total)
                    }
                    output.fd.sync()
                }
            }
            require(archive.length() > 0L) { "The downloaded Debian archive is empty." }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(128 * 1024)
            var count: Int
            while (input.read(buffer).also { count = it } != -1) digest.update(buffer, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractTarXz(file: File, destination: File) {
        val canonicalRoot = destination.canonicalFile.toPath()
        TarArchiveInputStream(XZCompressorInputStream(BufferedInputStream(FileInputStream(file)))).use { tar ->
            var entry = tar.nextTarEntry
            val buffer = ByteArray(64 * 1024)
            var entryCount = 0
            var expandedBytes = 0L
            while (entry != null) {
                entryCount += 1
                require(entryCount <= 500_000) { "Debian archive contains too many entries." }

                // Never reinterpret an absolute archive path as a relative one.
                val rawName = entry.name
                require(!rawName.startsWith("/") && !rawName.split('/').contains("..")) {
                    "Unsafe path found in Debian archive."
                }
                // Debian rootfs tarballs commonly contain a top-level "." directory entry.
                // It represents the extraction root itself, so safely skip it.
                if (rawName == "." || rawName == "./" || rawName.isBlank()) {
                    entry = tar.nextTarEntry
                    continue
                }
                val name = rawName.removePrefix("./").trimEnd('/')
                if (name.isBlank() || name == ".") {
                    entry = tar.nextTarEntry
                    continue
                }
                val target = File(destination, name).canonicalFile
                require(target.toPath().startsWith(canonicalRoot) && target.toPath() != canonicalRoot) {
                    "Unsafe path found in Debian archive."
                }

                when {
                    entry.isDirectory -> {
                        require(target.mkdirs() || target.isDirectory) { "Could not create Debian directory: $name" }
                    }
                    entry.isSymbolicLink -> {
                        val link = File(entry.linkName)
                        require(!link.isAbsolute) {
                            "Unsafe symbolic link in Debian archive."
                        }
                        val resolved = target.parentFile.toPath().resolve(link.toPath()).normalize()
                        require(resolved.startsWith(canonicalRoot)) { "Unsafe symbolic link in Debian archive." }
                        target.parentFile?.mkdirs()
                        require(!java.nio.file.Files.exists(target.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                            "Duplicate archive entry: $name"
                        }
                        java.nio.file.Files.createSymbolicLink(target.toPath(), link.toPath())
                    }
                    entry.isFile -> {
                        require(entry.size >= 0 && entry.size <= 2_000_000_000L) { "Invalid file size in Debian archive." }
                        expandedBytes += entry.size
                        require(expandedBytes <= 8_000_000_000L) { "Debian archive expands beyond the 8 GB safety limit." }
                        target.parentFile?.mkdirs()
                        require(!java.nio.file.Files.exists(target.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                            "Duplicate archive entry: $name"
                        }
                        FileOutputStream(target).use { output ->
                            var count: Int
                            while (tar.read(buffer).also { count = it } != -1) output.write(buffer, 0, count)
                        }
                        target.setExecutable((entry.mode and 0b001001001) != 0, false)
                        target.setReadable((entry.mode and 0b100100100) != 0, false)
                        target.setWritable((entry.mode and 0b010010010) != 0, false)
                    }
                    else -> error("Unsupported special or hard-link entry in Debian archive: $name")
                }
                entry = tar.nextTarEntry
            }
        }
    }

    private fun formatBytes(value: Long): String =
        if (value <= 0) "size unknown" else String.format(Locale.US, "%.1f MB", value / 1_000_000.0)
}
