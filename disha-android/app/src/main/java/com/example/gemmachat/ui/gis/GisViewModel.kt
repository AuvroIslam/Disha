package com.example.gemmachat.ui.gis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmachat.GemmaChatApplication
import com.example.gemmachat.core.Facility
import com.example.gemmachat.core.Gis
import com.example.gemmachat.core.Shelter
import com.example.gemmachat.data.RegionAssets
import com.example.gemmachat.data.Regions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GisUiState(
    val userLat: Double = 0.0,
    val userLon: Double = 0.0,
    val usingGps: Boolean = false,
    val locating: Boolean = false,
    val ranked: List<Gis.RankedShelter> = emptyList(),
    val route: Gis.Route? = null,
    val naiveCrossesFlood: Boolean = false,
    val elderly: Boolean = false,
    val computed: Boolean = false,
)

class GisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GemmaChatApplication
    private val region = Regions.byId(app.prefs.activeRegion.value)
    val regionName: String = if (app.prefs.isBangla) region.nameBn else region.nameEn

    val shelters: List<Shelter> by lazy { RegionAssets.loadShelters(application, region.id) }
    val facilities: List<Facility> by lazy { RegionAssets.loadFacilities(application, region.id) }
    val floodPolys: List<List<DoubleArray>> by lazy { RegionAssets.loadFloodPolys(application, region.id) }
    val graph: Gis.PedGraph by lazy { RegionAssets.loadGraph(application, region.id) }

    private val _ui = MutableStateFlow(GisUiState(userLat = region.centerLat, userLon = region.centerLon))
    val ui: StateFlow<GisUiState> = _ui

    fun setElderly(v: Boolean) {
        _ui.value = _ui.value.copy(elderly = v)
    }

    fun findNearestShelter() {
        if (_ui.value.locating) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(locating = true)
            val loc = app.locationProvider.current()
            val lat = loc?.first ?: region.centerLat
            val lon = loc?.second ?: region.centerLon
            val profile = if (_ui.value.elderly) listOf("elderly") else emptyList()
            val ranked = Gis.findNearestShelter(lat, lon, shelters, profile)
            val top = ranked.firstOrNull()
            val route = top?.let { Gis.safeRoute(lat, lon, it.lat, it.lon, graph, floodPolys) }
            val naive = top?.let {
                Gis.segmentCrossesFlood(doubleArrayOf(lat, lon), doubleArrayOf(it.lat, it.lon), floodPolys)
            } ?: false
            _ui.value = _ui.value.copy(
                ranked = ranked, route = route, naiveCrossesFlood = naive, computed = true,
                userLat = lat, userLon = lon, usingGps = loc != null, locating = false,
            )
        }
    }
}
