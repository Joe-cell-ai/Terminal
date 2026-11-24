package dev.linuxterminal.tools

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Tool(val title: String, val packageName: String, val detail: String, val command: String)

private val tools = listOf(
    Tool("Python", "python3", "Python interpreter for scripts and automation", "apt install python3 python3-pip"),
    Tool("Git", "git", "Version control for source code", "apt install git"),
    Tool("Node.js", "nodejs npm", "JavaScript runtime and npm package manager", "apt install nodejs npm"),
    Tool("SSH", "openssh-client", "Connect securely to remote development machines", "apt install openssh-client"),
    Tool("Nano", "nano", "Beginner-friendly terminal text editor", "apt install nano"),
    Tool("Vim", "vim", "Modal terminal text editor", "apt install vim"),
    Tool("C/C++ build tools", "build-essential", "Compiler, linker and standard build tools", "apt install build-essential"),
    Tool("Curl", "curl", "HTTP requests and API testing", "apt install curl")
)

@Composable
fun DeveloperToolsScreen() {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Developer tools", style = MaterialTheme.typography.headlineSmall)
        Text("Optional Debian packages · install only what you need", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Runtime connection required", style = MaterialTheme.typography.titleMedium)
                Text(
                    "These tools install inside Debian, not Android. The command reference is ready, but one-tap installation is disabled until the verified PRoot Debian shell and APT execution are connected.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f), fontSize = 12.sp
                )
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tools, key = { it.title }) { tool ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tool.title, style = MaterialTheme.typography.titleMedium)
                        Text(tool.detail, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f), fontSize = 12.sp)
                        Text("Package: ${tool.packageName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {}, enabled = false) { Text("Install") }
                            Button(onClick = {}, enabled = false) { Text("Not connected") }
                        }
                        Text(tool.command, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                    }
                }
            }
        }
    }
}
