package dev.linuxterminal.packages

/**
 * Builds APT argument vectors without passing package names through a shell.
 * The caller must execute these arguments inside the Debian PRoot environment.
 */
object AptCommand {
    private val packageName = Regex("[a-z0-9][a-z0-9+.-]{0,127}")

    fun update(): List<String> = listOf("apt-get", "update")

    fun upgrade(): List<String> = listOf("apt-get", "upgrade")

    fun install(name: String): List<String> {
        requireValid(name)
        return listOf("apt-get", "install", "-y", name)
    }

    fun remove(name: String): List<String> {
        requireValid(name)
        return listOf("apt-get", "remove", "-y", name)
    }

    private fun requireValid(name: String) {
        require(packageName.matches(name)) { "Invalid Debian package name." }
    }
}
