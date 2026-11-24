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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

/**
 * Package catalogue and safe APT command preparation.
 * Actual APT execution remains gated until the Debian PRoot shell is connected.
 */
@Composable
fun PackageManagerScreen() {
    var query by remember { mutableStateOf("") }
    val filtered = suggestedPackages.filter {
        it.name.contains(query.trim(), ignoreCase = true) ||
            it.description.contains(query.trim(), ignoreCase = true)
    }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Package manager", style = MaterialTheme.typography.headlineSmall)
        Text("Debian APT · Bookworm ARM64", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Debian shell not connected", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The package catalogue and command validation are ready. Install, remove, update and upgrade will unlock after the verified PRoot runtime is connected to Debian.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}, enabled = false) { Text("Update lists") }
                    OutlinedButton(onClick = {}, enabled = false) { Text("Upgrade") }
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Find a package") },
            placeholder = { Text("e.g. git, python3, nano") }
        )
        Text("Popular developer packages", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.name }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(3.dp))
                            Text(item.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                        }
                        OutlinedButton(onClick = {}, enabled = false) { Text("Install") }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item { Text("No matching suggested packages.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f)) }
            }
        }
    }
}
