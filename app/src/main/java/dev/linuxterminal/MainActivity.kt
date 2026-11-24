package dev.linuxterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TerminalBlack = Color(0xFF080B08)
private val TerminalGreen = Color(0xFF55E879)
private val Panel = Color(0xFF111711)
private val Muted = Color(0xFF9AA69A)

private enum class Destination(val label: String) {
    Home("Home"), Terminal("Terminal"), Files("Files"), Packages("Packages"), Settings("Settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TerminalApp() }
    }
}

@Composable
private fun TerminalApp() {
    var selected by remember { mutableStateOf(Destination.Home) }
    var classic by remember { mutableStateOf(true) }
    val background = if (classic) TerminalBlack else Color(0xFF101522)
    val accent = if (classic) TerminalGreen else Color(0xFF83B7FF)

    MaterialTheme(colorScheme = darkColorScheme(
        background = background, surface = Panel, primary = accent,
        onBackground = Color(0xFFE7F0E7), onSurface = Color(0xFFE7F0E7)
    )) {
        Scaffold(
            containerColor = background,
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = accent) {
                    val destinations = listOf(
                        Destination.Home to Icons.Filled.Home,
                        Destination.Terminal to Icons.Filled.Code,
                        Destination.Files to Icons.Filled.Folder,
                        Destination.Packages to Icons.Filled.Inventory2,
                        Destination.Settings to Icons.Filled.Settings
                    )
                    destinations.forEach { (destination, icon) ->
                        NavigationBarItem(
                            selected = selected == destination,
                            onClick = { selected = destination },
                            icon = { Icon(icon, contentDescription = destination.label) },
                            label = { Text(destination.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = accent,
                                indicatorColor = accent.copy(alpha = 0.13f),
                                unselectedIconColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selected) {
                    Destination.Home -> HomeScreen(accent) { selected = Destination.Terminal }
                    Destination.Terminal -> TerminalPreview(accent)
                    Destination.Files -> PlaceholderScreen("File manager", "Browse the Debian home directory and shared Android files.")
                    Destination.Packages -> PlaceholderScreen("Packages", "Install and manage Debian packages with APT.")
                    Destination.Settings -> SettingsScreen(classic, { classic = it })
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(accent: Color, openTerminal: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("TERMINAL", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Text("Your Linux workspace.", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Debian ARM64 · Android · No root", color = Muted, fontSize = 13.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Debian environment", fontWeight = FontWeight.SemiBold)
                Text("Not installed yet", color = accent, fontSize = 13.sp)
                Text("The Debian userspace will be downloaded separately so the app stays lightweight.", color = Muted, fontSize = 13.sp)
                Button(onClick = openTerminal, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("Open terminal", color = TerminalBlack)
                }
            }
        }
        Text("Quick start", fontWeight = FontWeight.SemiBold)
        listOf("Install Debian userspace", "Open an interactive shell", "Add developer tools with APT").forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = accent.copy(alpha = .14f), shape = RoundedCornerShape(10.dp)) {
                    Text("${index + 1}", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = accent)
                }
                Text(item, color = Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TerminalPreview(accent: Color) {
    val lines = listOf(
        "Terminal 0.1.0 · preview",
        "Debian ARM64 environment is not installed.",
        "",
        "This screen is a UI preview only.",
        "The real PTY shell arrives in Batch 3."
    )
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SESSION 01", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("NOT CONNECTED", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxSize().background(Color.Black, RoundedCornerShape(14.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lines.forEach { line ->
                Text(line, color = if (line.startsWith("Terminal")) accent else Color(0xFFD2DDD2),
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text("\\$", color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Coming in a later development batch", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        Text(description, color = Muted, fontSize = 14.sp)
    }
}

@Composable
private fun SettingsScreen(classic: Boolean, onThemeChange: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Settings", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appearance", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Classic terminal")
                        Text("Black background with green accents", color = Muted, fontSize = 12.sp)
                    }
                    Switch(checked = classic, onCheckedChange = onThemeChange)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Terminal font", fontWeight = FontWeight.SemiBold)
                Text("Fira Code (selected)", color = MaterialTheme.colorScheme.primary)
                Text("The bundled font and ligature rendering will be connected with the PTY terminal renderer in Batch 3.", color = Muted, fontSize = 12.sp)
            }
        }
        Text("Runtime", fontWeight = FontWeight.SemiBold)
        Text("Debian ARM64 · PRoot · no root access", color = Muted, fontSize = 13.sp)
        Text("Root filesystem downloads separately from the APK.", color = Muted, fontSize = 13.sp)
    }
}
