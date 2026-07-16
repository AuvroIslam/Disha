package com.example.gemmachat.ui.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmachat.GemmaChatApplication
import com.example.gemmachat.core.Gis
import com.example.gemmachat.core.Summary
import com.example.gemmachat.core.Triage
import com.example.gemmachat.data.RegionAssets
import com.example.gemmachat.data.download.HfDownloadRepository
import com.example.gemmachat.inference.GemmaLlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SummaryUiState(
    val engineLoading: Boolean = true,
    val engineReady: Boolean = false,
    val busy: Boolean = false,
    val briefing: String? = null,
    val stats: Summary.Stats? = null,
    val error: String? = null,
)

class SummaryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GemmaChatApplication
    private val engine by lazy { GemmaLlmEngine(app.engineHolder) }

    private val _ui = MutableStateFlow(SummaryUiState())
    val ui: StateFlow<SummaryUiState> = _ui

    init {
        viewModelScope.launch {
            if (!app.engineHolder.isReady()) {
                val model = HfDownloadRepository.modelFile(getApplication())
                if (model.exists()) app.engineHolder.loadModel(model)
            }
            _ui.value = _ui.value.copy(engineLoading = false, engineReady = app.engineHolder.isReady())
        }
    }

    fun generate() {
        if (_ui.value.busy) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, error = null)
            try {
                val ctx = getApplication<Application>()
                val scenarios = RegionAssets.loadScenarios(ctx)
                val shelters = RegionAssets.loadShelters(ctx)
                val graph = RegionAssets.loadGraph(ctx)
                val flood = RegionAssets.loadFloodPolys(ctx)
                val blocked = graph.edges.mapIndexedNotNull { i, (u, v) ->
                    if (Gis.segmentCrossesFlood(graph.nodes.getValue(u), graph.nodes.getValue(v), flood))
                        "seg_$i" else null
                }
                // Batch-triage the field reports deterministically (fast); Gemma writes the briefing.
                val results = scenarios.map { Triage.fallbackTriage(it) }
                val ready = app.engineHolder.isReady()
                val out = withContext(Dispatchers.Default) {
                    Summary.disasterSummary(
                        scenarios, results, shelters = shelters, blockedRoads = blocked,
                        newSince = scenarios.size, gemma = if (ready) engine else null)
                }
                _ui.value = _ui.value.copy(busy = false, briefing = out.briefing, stats = out.stats)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(busy = false, error = e.message)
            }
        }
    }
}
