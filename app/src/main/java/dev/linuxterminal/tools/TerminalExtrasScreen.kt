package dev.linuxterminal.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linuxterminal.runtime.DebianCommandRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class SshProfile(val label: String, val user: String, val host: String, val port: String)

@Composable
fun TerminalExtrasScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("terminal_extras", Context.MODE_PRIVATE) }
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val runner = remember(context) { DebianCommandRunner(context) }
    val scope = rememberCoroutineScope()
    var savedClipboard by remember { mutableStateOf(prefs.getString("clipboard_history", "") ?: "") }
    var historyQuery by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(emptyList<String>()) }
    var sshProfiles by remember { mutableStateOf(loadProfiles(prefs.getString("ssh_profiles", "") ?: "")) }
    var showSsh by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var storageSummary by remember { mutableStateOf("Tap Analyze to inspect Debian storage.") }
    var cleanupStatus by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun copy(value: String, title: String) {
        clipboard.setPrimaryClip(ClipData.newPlainText(title, value))
        cleanupStatus = "$title copied to clipboard"
    }
    fun saveProfiles(items: List<SshProfile>) {
        sshProfiles = items
        prefs.edit().putString("ssh_profiles", items.joinToString("\n") { listOf(it.label, it.user, it.host, it.port).joinToString("|") }).apply()
    }
    val shortcuts = listOf(
        "Update Debian" to "apt update",
        "Upgrade packages" to "apt upgrade",
        "Install Git + Python" to "apt install git python3 python3-pip",
        "Show disk usage" to "df -h",
        "List processes" to "ps aux",
        "Network interfaces" to "ip addr",
        "Current directory" to "pwd",
        "List files" to "ls -lah"
    )

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Terminal toolkit", style = MaterialTheme.typography.headlineSmall)
            Text("Quick commands, saved connections, clipboard and system utilities", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Command shortcuts", style = MaterialTheme.typography.titleMedium)
                    Text("Tap a command to copy it, then paste into your Debian shell.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                    shortcuts.forEach { (name, command) ->
                        Row(Modifier.fillMaxWidth().clickable { copy(command, name) }.padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(name)
                                Text(command, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("Copy", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SSH connection profiles", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showSsh = true }) { Text("Add") }
                    }
                    if (sshProfiles.isEmpty()) Text("No saved servers yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                    sshProfiles.forEach { profile ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(profile.label)
                                Text("${profile.user}@${profile.host}:${profile.port}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = { copy("ssh -p ${profile.port} ${profile.user}@${profile.host}", "SSH command") }) { Text("Copy SSH") }
                            TextButton(onClick = { saveProfiles(sshProfiles - profile) }) { Text("Remove") }
                        }
                    }
                    Text("SSH profiles store connection details only. Private keys remain in your Debian environment.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Clipboard history", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val value = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (value.isNotBlank()) {
                                val updated = (listOf(value) + savedClipboard.lines().filter { it.isNotBlank() && it != value }).take(30)
                                savedClipboard = updated.joinToString("\n")
                                prefs.edit().putString("clipboard_history", savedClipboard).apply()
                            }
                        }) { Text("Save current clipboard") }
                        OutlinedButton(onClick = { savedClipboard = ""; prefs.edit().remove("clipboard_history").apply() }) { Text("Clear") }
                    }
                    savedClipboard.lines().filter { it.isNotBlank() }.forEach { value ->
                        Text(value.take(140), Modifier.fillMaxWidth().clickable { copy(value, "Clipboard item") }.padding(vertical = 4.dp), fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Command history", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = historyQuery, onValueChange = { historyQuery = it }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, label = { Text("Search previous commands") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                history = withContext(Dispatchers.IO) {
                                    val file = File(context.filesDir, "debian/root/root/.bash_history")
                                    if (file.isFile) file.readLines().takeLast(1000).asReversed() else emptyList()
                                }
                            }
                        }) { Text("Load history") }
                    }
                    history.filter { it.contains(historyQuery, ignoreCase = true) }.take(40).forEach { command ->
                        Text(command, Modifier.fillMaxWidth().clickable { copy(command, "Command") }.padding(vertical = 3.dp),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    if (history.isEmpty()) Text("History appears after Bash writes ~/.bash_history when the shell exits.", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Storage analyzer & cleanup", style = MaterialTheme.typography.titleMedium)
                    Text(storageSummary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                storageSummary = withContext(Dispatchers.IO) {
                                    val root = File(context.filesDir, "debian")
                                    if (!root.isDirectory) "Debian rootfs is not installed."
                                    else {
                                        var bytes = 0L
                                        var count = 0
                                        root.walkTopDown().take(50000).forEach { file ->
                                            if (file.isFile) { bytes += file.length(); count++ }
                                        }
                                        "Debian usage: ${bytes / (1024 * 1024)} MB across $count files (scan capped at 50,000 entries)."
                                    }
                                }
                            }
                        }) { Text("Analyze") }
                        Button(enabled = !busy, onClick = {
                            scope.launch {
                                busy = true
                                cleanupStatus = "Cleaning APT cache…"
                                try {
                                    val result = runner.run(listOf("apt-get", "clean")) { }
                                    cleanupStatus = if (result.exitCode == 0) "APT cache cleaned." else "Cleanup failed · exit ${result.exitCode}"
                                } catch (t: Throwable) { cleanupStatus = t.message ?: "Cleanup failed" }
                                busy = false
                            }
                        }) { Text(if (busy) "Working…" else "Clean APT cache") }
                    }
                    if (cleanupStatus.isNotBlank()) Text(cleanupStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showSsh) AlertDialog(
        onDismissRequest = { showSsh = false },
        title = { Text("Add SSH profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(label, { label = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Home server") })
                OutlinedTextField(user, { user = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("username") })
                OutlinedTextField(host, { host = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("server.example.com") })
                OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5) }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("22") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (label.isNotBlank() && user.matches(Regex("[A-Za-z0-9._-]+")) &&
                    host.matches(Regex("[A-Za-z0-9.-]+")) && (port.toIntOrNull() ?: 0) in 1..65535) {
                    saveProfiles(sshProfiles + SshProfile(label.trim(), user, host, port))
                    label = ""; user = ""; host = ""; port = "22"; showSsh = false
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { showSsh = false }) { Text("Cancel") } }
    )
}

private fun loadProfiles(raw: String): List<SshProfile> = raw.lineSequence().mapNotNull { line ->
    val parts = line.split("|")
    if (parts.size == 4) SshProfile(parts[0], parts[1], parts[2], parts[3]) else null
}.toList()
