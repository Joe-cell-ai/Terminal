package dev.linuxterminal.files

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
private enum class EditAction { CREATE_FILE, CREATE_FOLDER, RENAME, DELETE }

/** App-private Debian rootfs browser. Android documents are transferred through SAF. */
@Composable
fun DebianFileManagerScreen() {
    val context = LocalContext.current
    val root = remember { File(context.filesDir, "debian").canonicalFile }
    val home = remember(root) { File(root, "root").canonicalFile }
    var current by remember { mutableStateOf(home) }
    var entries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var message by remember { mutableStateOf("Debian home directory") }
    var exportSource by remember { mutableStateOf<File?>(null) }
    var editAction by remember { mutableStateOf<EditAction?>(null) }
    var selected by remember { mutableStateOf<File?>(null) }
    var name by remember { mutableStateOf("") }
    var editingFile by remember { mutableStateOf<File?>(null) }
    var editorContent by remember { mutableStateOf("") }
    var externalRoot by remember { mutableStateOf<DocumentFile?>(null) }
    var externalCurrent by remember { mutableStateOf<DocumentFile?>(null) }
    var externalEntries by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val externalPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val tree = DocumentFile.fromTreeUri(context, uri)
                if (tree != null && tree.canRead()) {
                    externalRoot = tree
                    externalCurrent = tree
                    externalEntries = tree.listFiles().sortedBy { it.name?.lowercase().orEmpty() }
                    message = "External storage connected: ${tree.name ?: "Storage"}"
                } else message = "Android did not grant access to this storage."
            }.onFailure { message = "Could not open storage: ${it.message}" }
        }
    }

    fun safeChild(parent: File, childName: String): File {
        require(childName.isNotBlank() && childName != "." && childName != "..") { "Enter a valid name." }
        require(!childName.contains("/") && !childName.contains("\\\\") && childName.none { it.code < 32 }) {
            "Names cannot contain path separators or control characters."
        }
        val parentCanonical = parent.canonicalFile
        val target = File(parentCanonical, childName).canonicalFile
        require(target.parentFile == parentCanonical && parentCanonical.toPath().startsWith(root.toPath())) {
            "The selected path is outside Debian storage."
        }
        return target
    }

    fun refresh() {
        val base = root.toPath()
        val here = current.canonicalFile
        if (!here.toPath().startsWith(base) || !here.isDirectory) current = home
        entries = current.listFiles()?.filter { it.canonicalFile.toPath().startsWith(base) }
            ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?.map { Entry(it, it.isDirectory) } ?: emptyList()
    }

    fun perform(action: EditAction, target: File?, enteredName: String) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    when (action) {
                        EditAction.CREATE_FILE, EditAction.CREATE_FOLDER -> {
                            val created = safeChild(current, enteredName)
                            require(!created.exists()) { "An item with that name already exists." }
                            val ok = if (action == EditAction.CREATE_FOLDER) created.mkdirs() else created.createNewFile()
                            require(ok) { "Could not create the item." }
                            "Created ${created.name}"
                        }
                        EditAction.RENAME -> {
                            val source = requireNotNull(target).canonicalFile
                            require(source != root && source.parentFile == current.canonicalFile) { "Invalid rename target." }
                            val destination = safeChild(current, enteredName)
                            require(!destination.exists()) { "An item with that name already exists." }
                            require(source.renameTo(destination)) { "Rename failed." }
                            "Renamed to ${destination.name}"
                        }
                        EditAction.DELETE -> {
                            val source = requireNotNull(target).canonicalFile
                            require(source != root && source != home && source.parentFile == current.canonicalFile) {
                                "This protected directory cannot be deleted."
                            }
                            require(source.deleteRecursively()) { "Delete failed." }
                            "Deleted ${source.name}"
                        }
                    }
                }.getOrElse { "Operation failed: ${it.message ?: "unknown error"}" }
            }
            message = result
            refresh()
        }
    }

    LaunchedEffect(current) { if (externalCurrent == null) refresh() }
    LaunchedEffect(externalCurrent) {
        externalCurrent?.let { folder ->
            externalEntries = runCatching { folder.listFiles().sortedBy { it.name?.lowercase().orEmpty() } }
                .getOrDefault(emptyList())
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            message = withContext(Dispatchers.IO) {
                runCatching {
                    val displayName = (uri.lastPathSegment ?: "imported-file").substringAfterLast(':')
                        .substringAfterLast('/').replace(Regex("[^A-Za-z0-9._ -]"), "_").ifBlank { "imported-file" }
                    val target = safeChild(current, displayName)
                    require(!target.exists()) { "A file with that name already exists." }
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Could not read the selected document.")
                    "Imported ${target.name}"
                }.getOrElse { "Import failed: ${it.message}" }
            }
            refresh()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
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

    val debianAvailable = File(root, "etc/debian_version").isFile

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = {
                if (externalCurrent != null) {
                    val selectedFolder = externalCurrent
                    externalCurrent = if (selectedFolder == externalRoot) null else selectedFolder?.parentFile
                } else if (current != root) current = current.parentFile ?: root
                else current = home
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Parent folder")
            }
            Column(Modifier.weight(1f)) {
                Text(if (externalCurrent != null) "External storage" else "Terminal Data",
                    style = MaterialTheme.typography.headlineSmall)
                Text(if (externalCurrent != null) externalCurrent?.name ?: "USB / phone storage"
                    else current.relativeTo(root).path.let { if (it == ".") "/" else "/$it" },
                    color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, maxLines = 1)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text("Import") }
            OutlinedButton(onClick = { externalCurrent = null; current = root }) { Text("Terminal Data") }
            OutlinedButton(onClick = { externalPicker.launch(null) }) { Text("Phone / USB") }
            OutlinedButton(onClick = { externalCurrent = null; current = home }) { Text("Home") }
            IconButton(onClick = { name = ""; editAction = EditAction.CREATE_FILE }) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = "New file")
            }
            IconButton(onClick = { name = ""; editAction = EditAction.CREATE_FOLDER }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder")
            }
        }
        if (!debianAvailable && externalCurrent == null) {
            Text("Terminal Data becomes available after Debian is installed. Phone and USB storage can still be opened here.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f), fontSize = 12.sp)
        }
        Text(message, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f), fontSize = 12.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.weight(1f)) {
            if (externalCurrent != null) {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(externalEntries, key = { it.uri.toString() }) { document ->
                        Row(Modifier.fillMaxWidth().clickable {
                            if (document.isDirectory) externalCurrent = document
                            else message = "${document.name ?: "File"} · ${document.length()} bytes · external file selected"
                        }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (document.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(document.name ?: "Unnamed item", maxLines = 1)
                                Text(if (document.isDirectory) "Folder" else "${document.length()} bytes",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), fontSize = 11.sp)
                            }
                        }
                    }
                    if (externalEntries.isEmpty()) item { Text("This storage folder is empty.", Modifier.padding(16.dp)) }
                }
            } else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(entries, key = { it.file.absolutePath }) { entry ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).clickable {
                            if (entry.directory) current = entry.file
                            else {
                                val textExtensions = setOf("txt", "md", "sh", "py", "js", "ts", "json", "xml", "html", "css", "kt", "java", "c", "h", "cpp", "conf", "yaml", "yml", "toml", "ini", "log")
                                val textFile = entry.file.extension.lowercase() in textExtensions || entry.file.name in setOf(".bashrc", ".profile", ".gitignore")
                                if (textFile && entry.file.length() <= 512 * 1024) {
                                    scope.launch {
                                        val loaded = withContext(Dispatchers.IO) {
                                            runCatching { entry.file.readText(Charsets.UTF_8) }.getOrNull()
                                        }
                                        if (loaded != null) {
                                            editingFile = entry.file
                                            editorContent = loaded
                                        } else message = "Could not read this file as UTF-8 text."
                                    }
                                } else message = "${entry.file.name} · ${entry.file.length()} bytes · preview not supported"
                            }
                        }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (entry.directory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(entry.file.name, maxLines = 1)
                                Text(if (entry.directory) "Folder" else "${entry.file.length()} bytes · " +
                                    DateFormat.getDateInstance(DateFormat.SHORT).format(Date(entry.file.lastModified())),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), fontSize = 11.sp)
                            }
                        }
                        if (!entry.directory) {
                            TextButton(onClick = { exportSource = entry.file; exportLauncher.launch(entry.file.name) }) {
                                Text("Export", fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { selected = entry.file; name = entry.file.name; editAction = EditAction.RENAME }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename")
                        }
                        IconButton(onClick = { selected = entry.file; editAction = EditAction.DELETE }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
                if (entries.isEmpty()) item {
                    Text("This folder is empty.", Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                }
            }
        }
    }

    editingFile?.let { file ->
        AlertDialog(
            onDismissRequest = { editingFile = null },
            title = { Text("Edit ${file.name}") },
            text = {
                TextField(
                    value = editorContent,
                    onValueChange = { editorContent = it },
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    minLines = 12,
                    maxLines = 18
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val safe = file.canonicalFile
                                require(safe.toPath().startsWith(root.toPath()) && safe.isFile) { "File is outside Debian storage." }
                                val temp = File(safe.parentFile, safe.name + ".terminal-tmp")
                                temp.writeText(editorContent, Charsets.UTF_8)
                                require(temp.renameTo(safe)) { temp.delete(); "Could not save the file." }
                                "Saved ${safe.name}"
                            }.getOrElse { "Save failed: ${it.message}" }
                        }
                        message = result
                        editingFile = null
                        refresh()
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingFile = null }) { Text("Close") } }
        )
    }

    when (editAction) {
        EditAction.CREATE_FILE, EditAction.CREATE_FOLDER, EditAction.RENAME -> {
            val action = editAction!!
            AlertDialog(
                onDismissRequest = { editAction = null },
                title = { Text(when (action) {
                    EditAction.CREATE_FILE -> "Create file"
                    EditAction.CREATE_FOLDER -> "Create folder"
                    else -> "Rename item"
                }) },
                text = { TextField(value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text("Name") }) },
                confirmButton = {
                    TextButton(onClick = { perform(action, selected, name); editAction = null }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { editAction = null }) { Text("Cancel") } }
            )
        }
        EditAction.DELETE -> {
            val target = selected
            AlertDialog(
                onDismissRequest = { editAction = null },
                title = { Text("Delete item?") },
                text = { Text("Delete ${target?.name ?: "this item"} and its contents? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = { perform(EditAction.DELETE, target, ""); editAction = null }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { editAction = null }) { Text("Cancel") } }
            )
        }
        null -> Unit
    }
}
