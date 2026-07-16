package com.example.gemmachat.ui.firstaid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmachat.GemmaChatApplication
import com.example.gemmachat.core.Rag
import com.example.gemmachat.data.RegionAssets
import com.example.gemmachat.data.download.HfDownloadRepository
import com.example.gemmachat.inference.GemmaLlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FirstAidUiState(
    val engineLoading: Boolean = true,
    val engineReady: Boolean = false,
    val busy: Boolean = false,
    val answer: String? = null,
    val citations: List<Rag.Citation> = emptyList(),
    val redFlag: Boolean = false,
    val error: String? = null,
)

class FirstAidViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GemmaChatApplication
    private val engine by lazy { GemmaLlmEngine(app.engineHolder) }
    private val retriever by lazy { Rag.KeywordRetriever(RegionAssets.loadFirstAid(getApplication())) }

    private val _ui = MutableStateFlow(FirstAidUiState())
    val ui: StateFlow<FirstAidUiState> = _ui

    init {
        viewModelScope.launch {
            if (!app.engineHolder.isReady()) {
                val model = HfDownloadRepository.modelFile(getApplication())
                if (model.exists()) app.engineHolder.loadModel(model)
            }
            _ui.value = _ui.value.copy(engineLoading = false, engineReady = app.engineHolder.isReady())
        }
    }

    fun ask(query: String) {
        val q = query.trim()
        if (q.isEmpty() || _ui.value.busy) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, error = null, answer = null, citations = emptyList())
            try {
                val useGemma = app.engineHolder.isReady()
                app.engineHolder.respondInBangla = app.prefs.isBangla
                val ans = withContext(Dispatchers.Default) {
                    Rag.firstAidAnswer(q, retriever, if (useGemma) engine else null, k = 4)
                }
                _ui.value = _ui.value.copy(
                    busy = false, answer = ans.answer, citations = ans.citations, redFlag = ans.redFlag)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(busy = false, error = e.message)
            }
        }
    }
}
