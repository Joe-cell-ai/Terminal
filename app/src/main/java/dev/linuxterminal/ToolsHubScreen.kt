package dev.linuxterminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.linuxterminal.files.DebianFileManagerScreen
import dev.linuxterminal.packages.PackageManagerScreen
import dev.linuxterminal.tools.DeveloperToolsScreen
import dev.linuxterminal.tools.TerminalExtrasScreen

@Composable
fun ToolsHubScreen(
    accent: androidx.compose.ui.graphics.Color,
    progressMessage: String,
    progress: Int,
    installed: Boolean,
    installing: Boolean,
    error: String?,
    runtimeMessage: String,
    runtimePercent: Int,
    runtimeInstalled: Boolean,
    runtimeBusy: Boolean,
    runtimeError: String?,
    onInstall: () -> Unit,
    onInstallRuntime: () -> Unit,
    openTerminal: () -> Unit
) {
    var selected by remember { mutableIntStateOf(0) }
    val tabs = listOf("Setup", "Packages", "Tools", "File Explorer", "Utilities")
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color.White,
        onPrimary = Color.Black,
        background = Color.Black,
        surface = Color(0xFF101010),
        onSurface = Color.White,
        onBackground = Color.White
    )) {
    Column(Modifier.fillMaxSize()) {
        Text("Workspace", modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 8.dp),
            style = MaterialTheme.typography.headlineSmall)
        TabRow(selectedTabIndex = selected, containerColor = MaterialTheme.colorScheme.background,
            contentColor = accent) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selected == index, onClick = { selected = index },
                    text = { Text(title, maxLines = 1) })
            }
        }
        when (selected) {
            0 -> HomeScreen(accent, progressMessage, progress, installed, installing, error,
                runtimeMessage, runtimePercent, runtimeInstalled, runtimeBusy, runtimeError,
                onInstall, onInstallRuntime, openTerminal)
            1 -> PackageManagerScreen()
            2 -> DeveloperToolsScreen()
            3 -> DebianFileManagerScreen()
            else -> TerminalExtrasScreen()
        }
    }
    }
}

@Composable
fun HomeLandingScreen(openTerminal: () -> Unit, openTools: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("TERMINAL", color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge)
        Text("Your Linux workspace.", style = MaterialTheme.typography.headlineLarge)
        Text("A lightweight Debian terminal for Android.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
        androidx.compose.material3.Button(onClick = openTerminal, modifier = Modifier.fillMaxWidth()) {
            Text("Open Terminal")
        }
        androidx.compose.material3.OutlinedButton(onClick = openTools, modifier = Modifier.fillMaxWidth()) {
            Text("Setup, packages & tools")
        }
    }
}
