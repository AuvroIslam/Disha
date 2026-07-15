package com.example.gemmachat.ui.gis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.gemmachat.core.Facility
import com.example.gemmachat.core.Gis
import com.example.gemmachat.core.Shelter
import com.example.gemmachat.data.RegionAssets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GisUiState(
    val ranked: List<Gis.RankedShelter> = emptyList(),
    val route: Gis.Route? = null,
    val naiveCrossesFlood: Boolean = false,
    val elderly: Boolean = false,
    val computed: Boolean = false,
)

class GisViewModel(application: Application) : AndroidViewModel(application) {

    // Demo user location: a volunteer near flooded Halishahar, Chattogram.
    val userLat = 22.330
    val userLon = 91.820

    val shelters: List<Shelter> by lazy { RegionAssets.loadShelters(application) }
    val facilities: List<Facility> by lazy { RegionAssets.loadFacilities(application) }
    val floodPolys: List<List<DoubleArray>> by lazy { RegionAssets.loadFloodPolys(application) }
    val graph: Gis.PedGraph by lazy { RegionAssets.loadGraph(application) }

    private val _ui = MutableStateFlow(GisUiState())
    val ui: StateFlow<GisUiState> = _ui

    fun setElderly(v: Boolean) {
        _ui.value = _ui.value.copy(elderly = v)
    }

    fun findNearestShelter() {
        val profile = if (_ui.value.elderly) listOf("elderly") else emptyList()
        val ranked = Gis.findNearestShelter(userLat, userLon, shelters, profile)
        val top = ranked.firstOrNull()
        val route = top?.let { Gis.safeRoute(userLat, userLon, it.lat, it.lon, graph, floodPolys) }
        val naive = top?.let {
            Gis.segmentCrossesFlood(doubleArrayOf(userLat, userLon), doubleArrayOf(it.lat, it.lon), floodPolys)
        } ?: false
        _ui.value = _ui.value.copy(ranked = ranked, route = route, naiveCrossesFlood = naive, computed = true)
    }
}
