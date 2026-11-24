package dev.linuxterminal.runtime

import android.content.Context
import java.io.File
import java.io.RandomAccessFile

/**
 * Resolves the app-private Debian rootfs and PRoot executable.
 *
 * The PRoot binary is deliberately not downloaded from an unverified mirror. It must be
 * supplied by a separately verified runtime package at files/runtime/proot.
 */
data class TerminalLaunch(
    val executable: String,
    val workingDirectory: String,
    val arguments: Array<String>,
    val environment: Array<String>,
    val isDebian: Boolean
)

class ProotRuntime(context: Context) {
    private val appFiles = context.filesDir.canonicalFile
    private val rootfs = File(appFiles, "debian").canonicalFile
    private val runtimeDirectory = File(appFiles, "runtime").canonicalFile
    private val proot = File(runtimeDirectory, "proot").canonicalFile

    fun resolve(): TerminalLaunch? {
        if (!rootfs.isDirectory || !File(rootfs, "etc/debian_version").isFile) return null
        if (!proot.isFile || !proot.canExecute() || !isAarch64Elf(proot)) return null
        if (!proot.toPath().startsWith(runtimeDirectory.toPath())) return null

        val args = mutableListOf(
            "--link2symlink",
            "-0",
            "-r", rootfs.absolutePath,
            "-w", "/root",
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys"
        )
        val sharedStorage = File("/sdcard")
        if (sharedStorage.exists()) {
            args += listOf("-b", "/sdcard")
        }
        args += listOf(
            "/usr/bin/env", "-i",
            "HOME=/root",
            "USER=root",
            "LOGNAME=root",
            "TERM=xterm-256color",
            "LANG=C.UTF-8",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "/bin/bash", "--login"
        )
        return TerminalLaunch(
            executable = proot.absolutePath,
            workingDirectory = rootfs.absolutePath,
            arguments = args.toTypedArray(),
            environment = arrayOf(
                "HOME=${appFiles.absolutePath}",
                "TERM=xterm-256color",
                "LANG=C.UTF-8",
                "PATH=/system/bin:/system/xbin",
                "LD_LIBRARY_PATH=" + File(runtimeDirectory, "lib").absolutePath,
                "PROOT_LOADER=" + File(runtimeDirectory, "libexec/proot/loader").absolutePath
            ),
            isDebian = true
        )
    }

    private fun isAarch64Elf(file: File): Boolean = try {
        RandomAccessFile(file, "r").use { input ->
            if (input.length() < 20) return false
            val header = ByteArray(20)
            input.readFully(header)
            header[0] == 0x7f.toByte() &&
                header[1] == 'E'.code.toByte() &&
                header[2] == 'L'.code.toByte() &&
                header[3] == 'F'.code.toByte() &&
                header[4] == 2.toByte() &&
                header[5] == 1.toByte() &&
                (header[18].toInt() and 0xff) == 183 &&
                (header[19].toInt() and 0xff) == 0
        }
    } catch (_: Exception) {
        false
    }
}
