package dev.linuxterminal.runtime

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DebianInstallViewModel(application: Application) : AndroidViewModel(application) {
    private val installer = DebianInstaller(application)
    private val _progress = MutableStateFlow(DebianInstaller.Progress("Debian is not installed", 0))
    val progress: StateFlow<DebianInstaller.Progress> = _progress.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun install() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                installer.install { _progress.value = it }
            } catch (t: Throwable) {
                _error.value = t.message ?: "Debian installation failed."
            } finally {
                _busy.value = false
            }
        }
    }
}
