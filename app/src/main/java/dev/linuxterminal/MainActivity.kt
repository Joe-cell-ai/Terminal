package dev.linuxterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.linuxterminal.runtime.DebianInstallViewModel
import dev.linuxterminal.runtime.InteractiveTerminalScreen
import dev.linuxterminal.packages.PackageManagerScreen
import dev.linuxterminal.files.DebianFileManagerScreen
import dev.linuxterminal.tools.DeveloperToolsScreen

private val TerminalBlack = Color(0xFF080B08)
private val TerminalGreen = Color(0xFF55E879)
private val Panel = Color(0xFF111711)
private val Muted = Color(0xFF9AA69A)

private enum class Destination(val label: String) {
    Home("Home"), Terminal("Terminal"), Files("Files"), Packages("Packages"), Tools("Tools"), Settings("Settings")
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
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("terminal_preferences", Context.MODE_PRIVATE) }
    var classic by remember { mutableStateOf(preferences.getBoolean("classic_theme", true)) }
    var fontSize by remember { mutableFloatStateOf(preferences.getFloat("font_size", 14f)) }
    var scrollback by remember { mutableIntStateOf(preferences.getInt("scrollback", 2000)) }
    var sessionCount by remember { mutableIntStateOf(1) }
    var selectedSession by remember { mutableIntStateOf(0) }
    val installViewModel: DebianInstallViewModel = viewModel()
    val installProgress by installViewModel.progress.collectAsState()
    val installing by installViewModel.busy.collectAsState()
    val installError by installViewModel.error.collectAsState()\n    val runtimeProgress by installViewModel.runtimeProgress.collectAsState()\n    val runtimeBusy by installViewModel.runtimeBusy.collectAsState()\n    val runtimeError by installViewModel.runtimeError.collectAsState()
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
                        Destination.Tools to Icons.Filled.Build,
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
                    Destination.Home -> HomeScreen(
                        accent = accent,
                        progressMessage = installProgress.message,
                        progress = installProgress.percent,
                        installed = installProgress.installed,
                        installing = installing,
                        error = installError,
                        runtimeMessage = runtimeProgress.message,
                        runtimePercent = runtimeProgress.percent,
                        runtimeInstalled = runtimeProgress.installed,
                        runtimeBusy = runtimeBusy,
                        runtimeError = runtimeError,
                        onInstall = { installViewModel.install() },
                        onInstallRuntime = { installViewModel.installRuntime() },
                        openTerminal = { selected = Destination.Terminal }
                    )
                    Destination.Terminal -> Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(sessionCount) { index ->
                                FilterChip(
                                    selected = selectedSession == index,
                                    onClick = { selectedSession = index },
                                    label = { Text("Session ${index + 1}") }
                                )
                            }
                            TextButton(onClick = {
                                if (sessionCount < 4) {
                                    sessionCount += 1
                                    selectedSession = sessionCount - 1
                                }
                            }) { Text("+") }
                        }
                        InteractiveTerminalScreen(fontSize = fontSize, scrollback = scrollback, sessionKey = selectedSession)
                    }
                    Destination.Files -> DebianFileManagerScreen()
                    Destination.Packages -> PackageManagerScreen()
                    Destination.Tools -> DeveloperToolsScreen()
                    Destination.Settings -> SettingsScreen(
                        classic = classic,
                        onThemeChange = {
                            classic = it
                            preferences.edit().putBoolean("classic_theme", it).apply()
                        },
                        fontSize = fontSize,
                        onFontSizeChange = {
                            fontSize = it
                            preferences.edit().putFloat("font_size", it).apply()
                        },
                        scrollback = scrollback,
                        onScrollbackChange = {
                            scrollback = it
                            preferences.edit().putInt("scrollback", it).apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    accent: Color,
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
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("TERMINAL", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Text("Your Linux workspace.", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Debian ARM64 · Android · No root", color = Muted, fontSize = 13.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Debian environment", fontWeight = FontWeight.SemiBold)
                Text(if (installed) "Installed · Bookworm ARM64" else "Not installed", color = accent, fontSize = 13.sp)
                Text("Debian downloads separately from the APK and is checked against its published SHA-256 checksum.", color = Muted, fontSize = 13.sp)
                if (installing || progress > 0) {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth(), color = accent)
                    Text(progressMessage, color = Muted, fontSize = 12.sp)
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onInstall, enabled = !installing && !installed,
                        colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text(if (installed) "Debian installed" else if (installing) "Installing…" else "Install Debian", color = TerminalBlack)
                    }
                    OutlinedButton(onClick = openTerminal) { Text("Terminal") }
                }
                Text("Requires at least 1.5 GB free internal storage and an internet connection.", color = Muted, fontSize = 11.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("PRoot ARM64 runtime", fontWeight = FontWeight.SemiBold)
                Text(if (runtimeInstalled) "Installed · ready for Debian Bash" else "Separate ARM64 runtime", color = accent, fontSize = 13.sp)
                if (runtimeBusy || runtimePercent > 0) {
                    LinearProgressIndicator(progress = { runtimePercent / 100f }, modifier = Modifier.fillMaxWidth(), color = accent)
                    Text(runtimeMessage, color = Muted, fontSize = 12.sp)
                }
                if (runtimeError != null) Text(runtimeError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Button(onClick = onInstallRuntime, enabled = !runtimeBusy && !runtimeInstalled,
                    colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text(if (runtimeInstalled) "PRoot installed" else if (runtimeBusy) "Installing…" else "Install PRoot runtime", color = TerminalBlack)
                }
                Text("The runtime is downloaded separately and is not bundled in the APK.", color = Muted, fontSize = 11.sp)
            }
        }
        Text("Quick start", fontWeight = FontWeight.SemiBold)
        listOf("Install Debian userspace", "Install PRoot runtime", "Start an interactive Debian shell").forEachIndexed { index, item ->
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
private fun TerminalPreview(accent: Color, debianInstalled: Boolean) {
    val lines = listOf(
        "Terminal 0.2.0 · runtime setup",
        if (debianInstalled) "Debian Bookworm ARM64 files are installed." else "Debian ARM64 environment is not installed.",
        "",
        "PRoot launcher and PTY shell are not connected yet.",
        "Command execution will be enabled in a later batch."
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
private fun SettingsScreen(
    classic: Boolean,
    onThemeChange: (Boolean) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    scrollback: Int,
    onScrollbackChange: (Int) -> Unit
) {
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
                Text("Terminal text size · ${fontSize.toInt()} sp", color = MaterialTheme.colorScheme.primary)
                Slider(value = fontSize, onValueChange = onFontSizeChange, valueRange = 10f..24f, steps = 13)
                Text("Scrollback · $scrollback lines", color = MaterialTheme.colorScheme.primary)
                Slider(value = scrollback.toFloat(), onValueChange = { onScrollbackChange(it.toInt()) }, valueRange = 500f..10000f, steps = 18)
                Text("Uses the system monospace font until Fira Code is bundled.", color = Muted, fontSize = 12.sp)
            }
        }
        Text("Runtime", fontWeight = FontWeight.SemiBold)
        Text("Debian ARM64 · PRoot · no root access", color = Muted, fontSize = 13.sp)
        Text("Root filesystem downloads separately from the APK.", color = Muted, fontSize = 13.sp)
    }
}
