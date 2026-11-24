package dev.linuxterminal.runtime

import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase
import com.termux.shared.termux.terminal.TermuxTerminalViewClientBase
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

/**
 * Interactive PTY terminal. Until the separately installed PRoot runtime is available,
 * this deliberately starts the Android app's unprivileged host shell, not Debian.
 */
@Composable
fun InteractiveTerminalScreen() {
    val context = LocalContext.current
    val sessionClient = remember {
        object : TermuxTerminalSessionClientBase() {
            var terminalView: TerminalView? = null

            override fun onTextChanged(changedSession: TerminalSession) {
                terminalView?.post { terminalView?.onScreenUpdated() }
            }

            override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Terminal", text))
            }
        }
    }
    val viewClient = remember { object : TermuxTerminalViewClientBase() {} }
    val session = remember(context) {
        val home = context.filesDir.absolutePath
        TerminalSession(
            "/system/bin/sh",
            home,
            arrayOf("-i"),
            arrayOf(
                "HOME=$home",
                "TERM=xterm-256color",
                "PATH=/system/bin:/system/xbin",
                "LANG=C.UTF-8"
            ),
            2000,
            sessionClient
        )
    }

    DisposableEffect(session) {
        onDispose { session.finishIfRunning() }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            "ANDROID HOST SHELL · UNPRIVILEGED",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp
        )
        Text(
            "PRoot is not connected yet; this is not the Debian environment.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp, top = 3.dp)
        )
        AndroidView(
            modifier = Modifier.fillMaxSize(),
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
    }
}
