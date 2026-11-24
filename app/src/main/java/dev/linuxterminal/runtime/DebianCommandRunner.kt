package dev.linuxterminal.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Executes a validated argument vector inside the installed Debian PRoot rootfs. */
class DebianCommandRunner(context: Context) {
    private val appContext = context.applicationContext

    data class Result(val exitCode: Int, val output: String)

    suspend fun run(arguments: List<String>, onOutput: (String) -> Unit = {}): Result =
        withContext(Dispatchers.IO) {
            require(arguments.isNotEmpty()) { "A Debian command is required." }
            val launch = ProotRuntime(appContext).resolve()
                ?: error("Debian Bash is not ready. Install Debian and the PRoot runtime from Home first.")
            val command = mutableListOf(launch.executable).apply {
                addAll(launch.arguments.takeWhile { it != "/usr/bin/env" })
                addAll(listOf("/usr/bin/env", "-i", "HOME=/root", "USER=root", "LOGNAME=root",
                    "TERM=xterm-256color", "LANG=C.UTF-8",
                    "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"))
                addAll(arguments)
            }
            val process = ProcessBuilder(command).directory(java.io.File(launch.workingDirectory)).apply {
                environment().clear()
                launch.environment.forEach { entry ->
                    val index = entry.indexOf('=')
                    if (index > 0) environment()[entry.substring(0,index)] = entry.substring(index+1)
                }
            }.redirectErrorStream(true).start()
            val output = StringBuilder()
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val text = line + "\n"
                    output.append(text)
                    onOutput(text)
                }
            }
            val exit = process.waitFor()
            Result(exit, output.toString())
        }
}
