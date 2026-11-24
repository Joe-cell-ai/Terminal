package dev.linuxterminal.runtime

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DebianInstallViewModel(application: Application) : AndroidViewModel(application) {
    private val installer = DebianInstaller(application)
    private val prootInstaller = ProotRuntimeInstaller(application)
    private val _progress = MutableStateFlow(
        if (installer.isInstalled()) DebianInstaller.Progress("Debian Bookworm ARM64 installed", 100, true)
        else DebianInstaller.Progress("Debian is not installed", 0)
    )
    val progress: StateFlow<DebianInstaller.Progress> = _progress.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _runtimeProgress = MutableStateFlow(
        ProotRuntimeInstaller.Progress(
            if (prootInstaller.isInstalled()) "PRoot runtime installed" else "PRoot runtime not installed",
            if (prootInstaller.isInstalled()) 100 else 0,
            prootInstaller.isInstalled()
        )
    )
    val runtimeProgress: StateFlow<ProotRuntimeInstaller.Progress> = _runtimeProgress.asStateFlow()
    private val _runtimeBusy = MutableStateFlow(false)
    val runtimeBusy: StateFlow<Boolean> = _runtimeBusy.asStateFlow()
    private val _runtimeError = MutableStateFlow<String?>(null)
    val runtimeError: StateFlow<String?> = _runtimeError.asStateFlow()

    fun install() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                installer.install { _progress.value = it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                _error.value = t.message ?: "Debian installation failed."
            } finally {
                _busy.value = false
            }
        }
    }

    fun installRuntime() {
        if (_runtimeBusy.value) return
        viewModelScope.launch {
            _runtimeBusy.value = true
            _runtimeError.value = null
            try {
                prootInstaller.install { _runtimeProgress.value = it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                _runtimeError.value = t.message ?: "PRoot runtime installation failed."
            } finally {
                _runtimeBusy.value = false
            }
        }
    }
}
