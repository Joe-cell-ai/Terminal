package dev.linuxterminal.runtime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.graphics.Typeface
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
 * Starts Debian Bash only when an app-private ARM64 PRoot runtime and verified Debian rootfs
 * are present. Otherwise, the UI clearly labels the Android host-shell fallback.
 */
@Composable
fun InteractiveTerminalScreen(fontSize: Float = 14f, scrollback: Int = 2000, sessionKey: Int = 0, startupCommand: String = "", terminalBackground: Color = Color(0xFF1E2229), transparency: Float = 0f, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sessionClient = remember(context) { AppSessionClient(context) }
    val viewClient = remember { AppViewClient() }
    // Keep one PTY per tab while this terminal screen remains in composition.
    // Switching tabs therefore preserves each shell's working directory and variables.
    val sessions = remember(context) { mutableMapOf<Int, TerminalSession>() }
    val launch = remember(context) { ProotRuntime(context).resolve() }
    val session = remember(context, sessionKey, scrollback, launch) {
        sessions.getOrPut(sessionKey) {
            val home = context.filesDir.absolutePath
            val command = launch ?: TerminalLaunch(
                executable = "/system/bin/sh",
                workingDirectory = home,
                arguments = arrayOf("-i"),
                environment = arrayOf(
                    "HOME=$home",
                    "TERM=xterm-256color",
                    "PATH=/system/bin:/system/xbin",
                    "LANG=C.UTF-8"
                ),
                isDebian = false
            )
            TerminalSession(
                command.executable,
                command.workingDirectory,
                command.arguments,
                command.environment,
                scrollback,
                sessionClient
            ).also { newSession ->
                if (launch?.isDebian == true) {
                    val kaliSetup = """
                        export TERM=xterm-256color
                        export COLORTERM=truecolor
                        export CLICOLOR=1
                        export LS_COLORS='di=1;34:ln=1;36:ex=1;32:*.zip=1;31:*.gz=1;31:*.tar=1;31:*.xz=1;31:*.7z=1;31:*.deb=1;35:*.pdf=0;35'
                        export GREP_COLORS='ms=01;31:mc=01;31:sl=:cx=:fn=01;36:ln=01;32:bn=01;32:se=36'
                        export PS1='\\[\\e[1;34m\\]kali㉿kali\\[\\e[0m\\]:\\[\\e[1;36m\\]\\w\\[\\e[0m\\]\\$ '
                        alias ls='ls --color=auto'
                        alias ll='ls -alF --color=auto'
                        alias grep='grep --color=auto'
                        alias ip='ip --color=auto'
                        """.trimIndent() + "\n"
                    val startup = (kaliSetup + if (startupCommand.isNotBlank()) startupCommand.trim() + "\n" else "").toByteArray(Charsets.UTF_8)
                    newSession.write(startup, 0, startup.size)
                } else if (startupCommand.isNotBlank()) {
                    val startup = (startupCommand.trim() + "\n").toByteArray(Charsets.UTF_8)
                    newSession.write(startup, 0, startup.size)
                }
            }        }
    }

    fun sendKey(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        session.write(bytes, 0, bytes.size)
    }

    DisposableEffect(context) {
        onDispose {
            sessions.values.forEach { it.finishIfRunning() }
            sessions.clear()
            sessionClient.terminalView = null
        }
    }

    val alpha = 1f - transparency.coerceIn(0f, 0.75f)
    val terminalBg = terminalBackground.copy(alpha = alpha)
    Column(modifier.fillMaxSize().background(terminalBg).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            (if (launch?.isDebian == true) "DEBIAN BASH" else "ANDROID HOST SHELL") + " · TAB ${sessionKey + 1}",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp
        )
        Text(
            if (launch?.isDebian == true) "Debian Bookworm ARM64 · PRoot" else
                "Debian shell is unavailable. Install the verified ARM64 PRoot runtime to launch Debian Bash.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp, top = 3.dp)
        )
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxSize().background(terminalBg),
            factory = { viewContext ->
                TerminalView(viewContext, null).apply {
                    setTerminalViewClient(viewClient)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setTextSize(fontSize.toInt())
                    setTypeface(Typeface.MONOSPACE)
                    setBackgroundColor(android.graphics.Color.argb((alpha * 255).toInt(), android.graphics.Color.red(terminalBackground.toArgb()), android.graphics.Color.green(terminalBackground.toArgb()), android.graphics.Color.blue(terminalBackground.toArgb())))
                    setTerminalCursorBlinkerRate(600)
                    sessionClient.terminalView = this
                    attachSession(session)
                    requestFocus()
                }
            },
            update = { view ->
                view.setTextSize(fontSize.toInt())
                view.setBackgroundColor(android.graphics.Color.argb((alpha * 255).toInt(), android.graphics.Color.red(terminalBackground.toArgb()), android.graphics.Color.green(terminalBackground.toArgb()), android.graphics.Color.blue(terminalBackground.toArgb())))
                if (view.mTermSession !== session) {
                    sessionClient.terminalView = view
                    view.attachSession(session)
                }
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
            Button(onClick = {
                sessionClient.terminalView?.let { terminalView ->
                    terminalView.isFocusableInTouchMode = true
                    terminalView.requestFocus()
                    terminalView.post {
                        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }, modifier = Modifier.weight(1.3f)) { Text("Keyboard", fontSize = 10.sp, maxLines = 1) }
            listOf("ESC" to "\u001b", "TAB" to "\t", "CTRL+C" to "\u0003", "CTRL+D" to "\u0004", "↑" to "\u001b[A", "↓" to "\u001b[B", "←" to "\u001b[D", "→" to "\u001b[C").forEach { (label, value) ->
                TextButton(onClick = { sendKey(value) }, modifier = Modifier.weight(1f)) { Text(label, fontSize = 10.sp, maxLines = 1) }
            }
        }
    }
}
