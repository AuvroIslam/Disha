package com.example.gemmachat.ui.triage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmachat.GemmaChatApplication
import com.example.gemmachat.core.SosReport
import com.example.gemmachat.core.Triage
import com.example.gemmachat.core.TriageResult
import com.example.gemmachat.data.download.HfDownloadRepository
import com.example.gemmachat.inference.GemmaLlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TriagedItem(val text: String, val result: TriageResult)

data class TriageUiState(
    val engineLoading: Boolean = true,
    val engineReady: Boolean = false,
    val busy: Boolean = false,
    val queue: List<TriagedItem> = emptyList(),
    val error: String? = null,
)

class TriageViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GemmaChatApplication
    private val engine by lazy { GemmaLlmEngine(app.engineHolder) }
    private val rank = mapOf("critical" to 0, "high" to 1, "moderate" to 2, "low" to 3)

    private val _ui = MutableStateFlow(TriageUiState())
    val ui: StateFlow<TriageUiState> = _ui

    init {
        viewModelScope.launch {
            if (!app.engineHolder.isReady()) {
                val model = HfDownloadRepository.modelFile(getApplication())
                if (model.exists()) app.engineHolder.loadModel(model)
            }
            _ui.value = _ui.value.copy(engineLoading = false, engineReady = app.engineHolder.isReady())
        }
    }

    fun triage(text: String) {
        val t = text.trim()
        if (t.isEmpty() || _ui.value.busy) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, error = null)
            try {
                val useGemma = app.engineHolder.isReady()
                val result = withContext(Dispatchers.Default) {
                    Triage.triageSos(SosReport(text = t), if (useGemma) engine else null)
                }
                val queue = (_ui.value.queue + TriagedItem(t, result))
                    .sortedWith(compareBy({ rank[it.result.priority] }, { -it.result.urgencyScore }))
                _ui.value = _ui.value.copy(busy = false, queue = queue)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(busy = false, error = e.message)
            }
        }
    }

    fun clearQueue() {
        _ui.value = _ui.value.copy(queue = emptyList())
    }
}
