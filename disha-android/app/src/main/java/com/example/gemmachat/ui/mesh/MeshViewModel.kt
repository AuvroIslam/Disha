package com.example.gemmachat.ui.mesh

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.example.gemmachat.core.SosReport
import com.example.gemmachat.core.Triage
import com.example.gemmachat.mesh.MeshManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MeshMsg(
    val text: String,
    val sender: String,
    val verified: Boolean,
    val hops: Int,
    val priority: String,
    val color: String,
    val mine: Boolean,
)

data class MeshUiState(
    val started: Boolean = false,
    val status: String = "Idle",
    val peers: Int = 0,
    val messages: List<MeshMsg> = emptyList(),
)

class MeshViewModel(application: Application) : AndroidViewModel(application) {

    val localName = "Disha ${Build.MODEL}".take(48)
    private var mgr: MeshManager? = null

    private val _ui = MutableStateFlow(MeshUiState())
    val ui: StateFlow<MeshUiState> = _ui

    fun start() {
        if (mgr != null) return
        mgr = MeshManager(
            getApplication(), localName,
            onStatus = { s -> _ui.value = _ui.value.copy(status = s) },
            onPeersChanged = { p -> _ui.value = _ui.value.copy(peers = p) },
            onReceived = { env, ok, hops ->
                val text = env.payload["text"] as? String ?: ""
                val tr = Triage.fallbackTriage(SosReport(text = text))
                val msg = MeshMsg(text, env.sender, ok, hops, tr.priority, tr.color, mine = false)
                _ui.value = _ui.value.copy(messages = _ui.value.messages + msg)
            },
        ).also { it.start() }
        _ui.value = _ui.value.copy(started = true)
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val m = mgr ?: return
        m.sendSos(t, 22.330, 91.820)
        val tr = Triage.fallbackTriage(SosReport(text = t))
        val msg = MeshMsg(t, localName, true, 0, tr.priority, tr.color, mine = true)
        _ui.value = _ui.value.copy(messages = _ui.value.messages + msg)
    }

    fun stop() {
        mgr?.stop()
        mgr = null
        _ui.value = _ui.value.copy(started = false, peers = 0)
    }

    override fun onCleared() {
        stop()
    }
}
