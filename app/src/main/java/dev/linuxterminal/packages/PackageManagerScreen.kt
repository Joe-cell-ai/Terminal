package dev.linuxterminal.packages

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linuxterminal.runtime.DebianCommandRunner
import kotlinx.coroutines.launch

private data class SuggestedPackage(val name: String, val description: String)
private val suggestedPackages = listOf(
    SuggestedPackage("git", "Distributed version control"),
    SuggestedPackage("python3", "Python 3 interpreter"),
    SuggestedPackage("python3-pip", "Python package installer"),
    SuggestedPackage("nodejs", "JavaScript runtime"),
    SuggestedPackage("npm", "Node.js package manager"),
    SuggestedPackage("nano", "Terminal text editor"),
    SuggestedPackage("vim", "Vi improved text editor"),
    SuggestedPackage("openssh-client", "SSH client tools"),
    SuggestedPackage("curl", "Transfer data from URLs"),
    SuggestedPackage("build-essential", "C/C++ compiler toolchain")
)

/** APT operations execute only through the validated Debian PRoot argument-vector runner. */
@Composable
fun PackageManagerScreen() {
    val context = LocalContext.current
    val runner = remember(context) { DebianCommandRunner(context) }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready") }
    var output by remember { mutableStateOf("") }
    val filtered = suggestedPackages.filter {
        it.name.contains(query.trim(), ignoreCase = true) ||
            it.description.contains(query.trim(), ignoreCase = true)
    }

    fun execute(label: String, args: List<String>) {
        if (running) return
        scope.launch {
            running = true
            status = label
            output = ""
            try {
                val result = runner.run(args) { chunk ->
                    output = (output + chunk).takeLast(12000)
                }
                status = if (result.exitCode == 0) "$label completed" else "$label failed · exit ${result.exitCode}"
            } catch (t: Throwable) {
                status = "Error: ${t.message ?: "Debian command failed"}"
            } finally {
                running = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Package manager", style = MaterialTheme.typography.headlineSmall)
        Text("Debian APT · Bookworm ARM64", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(status, style = MaterialTheme.typography.titleMedium)
                if (running) LinearProgressIndicator(Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { execute("APT update", AptCommand.update()) }, enabled = !running) { Text("Update lists") }
                    OutlinedButton(onClick = { execute("System upgrade", AptCommand.upgrade()) }, enabled = !running) { Text("Upgrade") }
                }
                Text("Commands run inside Debian as root through PRoot. An internet connection is required.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f), fontSize = 12.sp)
                if (output.isNotBlank()) {
                    Text(output, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp, maxLines = 12, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f))
                }
            }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, label = { Text("Find a package") }, placeholder = { Text("e.g. git, python3, nano") })
        Text("Popular developer packages", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.name }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(3.dp))
                            Text(item.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            OutlinedButton(onClick = { execute("Installing ${item.name}", AptCommand.install(item.name)) }, enabled = !running) {
                                Text("Install")
                            }
                            TextButton(onClick = { execute("Removing ${item.name}", AptCommand.remove(item.name)) }, enabled = !running) {
                                Text("Remove", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty()) item { Text("No matching suggested packages.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f)) }
        }
    }
}
