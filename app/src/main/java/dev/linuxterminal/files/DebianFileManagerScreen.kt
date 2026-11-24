package dev.linuxterminal.files

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

private data class Entry(val file: File, val directory: Boolean)

/**
 * App-private Debian rootfs browser. Android documents are transferred through SAF;
 * no broad storage permission is requested.
 */
@Composable
fun DebianFileManagerScreen() {
    val context = LocalContext.current
    val root = remember { File(context.filesDir, "debian").canonicalFile }
    val home = remember(root) { File(root, "root").also { it.mkdirs() }.canonicalFile }
    var current by remember { mutableStateOf(home) }
    var entries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var message by remember { mutableStateOf("Debian home directory") }
    val scope = rememberCoroutineScope()

    fun refresh() {
        val base = root.toPath()
        val here = current.canonicalFile
        if (!here.toPath().startsWith(base) || !here.isDirectory) {
            current = home
        }
        entries = current.listFiles()?.filter { it.canonicalFile.toPath().startsWith(base) }
            ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?.map { Entry(it, it.isDirectory) } ?: emptyList()
    }

    LaunchedEffect(current) { refresh() }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val name = (uri.lastPathSegment ?: "imported-file").substringAfterLast(':')
                        .substringAfterLast('/').replace(Regex("[^A-Za-z0-9._ -]"), "_")
                        .ifBlank { "imported-file" }
                    val target = File(current.canonicalFile, name).canonicalFile
                    require(target.parentFile == current.canonicalFile && !target.exists()) {
                        "A file with that name already exists."
                    }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Could not read the selected document.")
                    "Imported $name"
                }.getOrElse { "Import failed: ${it.message}" }
            }
            message = result
            refresh()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        val selected = current
        // The export action is bound to the file selected immediately before the picker opens.
        val source = exportSource
        if (uri != null && source != null) scope.launch {
            message = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        source.inputStream().use { input -> input.copyTo(output) }
                    } ?: error("Could not write to the selected destination.")
                    "Exported ${source.name}"
                }.getOrElse { "Export failed: ${it.message}" }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { if (current != home) current = current.parentFile ?: home }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Parent folder")
            }
            Column(Modifier.weight(1f)) {
                Text("Debian files", style = MaterialTheme.typography.headlineSmall)
                Text(current.relativeTo(root).path.let { if (it == ".") "/root" else "/$it" },
                    color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, maxLines = 1)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text("Import") }
            OutlinedButton(onClick = { current = home }) { Text("Home") }
        }
        Text(message, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f), fontSize = 12.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(entries, key = { it.file.absolutePath }) { entry ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (entry.directory) current = entry.file else {
                                message = "${entry.file.name} · ${entry.file.length()} bytes"
                            }
                        }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (entry.directory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(entry.file.name, maxLines = 1)
                            Text(
                                if (entry.directory) "Folder" else "${entry.file.length()} bytes · " +
                                    DateFormat.getDateInstance(DateFormat.SHORT).format(Date(entry.file.lastModified())),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                                fontSize = 11.sp
                            )
                        }
                        if (!entry.directory) {
                            Text("Export", color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    exportSource = entry.file
                                    exportLauncher.launch(entry.file.name)
                                }.padding(8.dp), fontSize = 12.sp)
                        }
                    }
                }
                if (entries.isEmpty()) {
                    item { Text("This folder is empty.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)) }
                }
            }
        }
    }
}

private var exportSource: File? = null
