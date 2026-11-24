package dev.linuxterminal.runtime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

private class AppSessionClient(private val context: Context) : TerminalSessionClient {
    var terminalView: TerminalView? = null

    override fun onTextChanged(changedSession: TerminalSession) {
        terminalView?.post { terminalView?.onScreenUpdated() }
    }
    override fun onTitleChanged(changedSession: TerminalSession) {}
    override fun onSessionFinished(finishedSession: TerminalSession) {}
    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Terminal", text))
    }
    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: return
        session?.write(text.toByteArray(Charsets.UTF_8), 0, text.toByteArray(Charsets.UTF_8).size)
    }
    override fun onBell(session: TerminalSession) {}
    override fun onColorsChanged(session: TerminalSession) { terminalView?.invalidate() }
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
    override fun getTerminalCursorStyle(): Int? = null
    override fun logError(tag: String, message: String) { Log.e(tag, message) }
    override fun logWarn(tag: String, message: String) { Log.w(tag, message) }
    override fun logInfo(tag: String, message: String) { Log.i(tag, message) }
    override fun logDebug(tag: String, message: String) { Log.d(tag, message) }
    override fun logVerbose(tag: String, message: String) { Log.v(tag, message) }
    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) { Log.e(tag, message, e) }
    override fun logStackTrace(tag: String, e: Exception) { Log.e(tag, e.message, e) }
}

private class AppViewClient : TerminalViewClient {
    override fun onScale(scale: Float): Float = scale.coerceIn(0.7f, 2.0f)
    override fun onSingleTapUp(e: MotionEvent) {}
    override fun shouldBackButtonBeMappedToEscape(): Boolean = true
    override fun shouldEnforceCharBasedInput(): Boolean = false
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) {}
    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
    override fun onLongPress(event: MotionEvent): Boolean = false
    override fun readControlKey(): Boolean = false
    override fun readAltKey(): Boolean = false
    override fun readShiftKey(): Boolean = false
    override fun readFnKey(): Boolean = false
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false
    override fun onEmulatorSet() {}
    override fun logError(tag: String, message: String) { Log.e(tag, message) }
    override fun logWarn(tag: String, message: String) { Log.w(tag, message) }
    override fun logInfo(tag: String, message: String) { Log.i(tag, message) }
    override fun logDebug(tag: String, message: String) { Log.d(tag, message) }
    override fun logVerbose(tag: String, message: String) { Log.v(tag, message) }
    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) { Log.e(tag, message, e) }
    override fun logStackTrace(tag: String, e: Exception) { Log.e(tag, e.message, e) }
}

/**
 * Interactive PTY terminal. Until PRoot is installed, this intentionally starts the app's
 * unprivileged Android host shell, not Debian.
 */
@Composable
fun InteractiveTerminalScreen() {
    val context = LocalContext.current
    val sessionClient = remember(context) { AppSessionClient(context) }
    val viewClient = remember { AppViewClient() }
    val session = remember(context) {
        val home = context.filesDir.absolutePath
        TerminalSession(
            "/system/bin/sh",
            home,
            arrayOf("-i"),
            arrayOf("HOME=$home", "TERM=xterm-256color", "PATH=/system/bin:/system/xbin", "LANG=C.UTF-8"),
            2000,
            sessionClient
        )
    }

    fun sendKey(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        session.write(bytes, 0, bytes.size)
    }

    DisposableEffect(session) {
        onDispose { session.finishIfRunning() }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text("ANDROID HOST SHELL · UNPRIVILEGED", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
        Text(
            "PRoot is not connected yet; this is not the Debian environment.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp, top = 3.dp)
        )
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxSize(),
            factory = { viewContext ->
                TerminalView(viewContext).apply {
                    setTerminalViewClient(viewClient)
                    setTextSize(14)
                    setTypeface(Typeface.MONOSPACE)
                    setTerminalCursorBlinkerRate(600)
                    sessionClient.terminalView = this
                    attachSession(session)
                    requestFocus()
                }
            },
            update = { view ->
                if (view.mTermSession !== session) {
                    sessionClient.terminalView = view
                    view.attachSession(session)
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
            listOf("ESC" to "\\u001b", "TAB" to "\\t", "CTRL+C" to "\\u0003", "CTRL+D" to "\\u0004", "↑" to "\\u001b[A", "↓" to "\\u001b[B", "←" to "\\u001b[D", "→" to "\\u001b[C").forEach { (label, value) ->
                TextButton(onClick = { sendKey(value) }, modifier = Modifier.weight(1f)) { Text(label, fontSize = 10.sp, maxLines = 1) }
            }
        }
    }
}
