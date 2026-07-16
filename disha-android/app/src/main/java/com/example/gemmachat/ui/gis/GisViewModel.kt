package com.example.gemmachat.ui.gis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmachat.GemmaChatApplication
import com.example.gemmachat.core.Gis
import com.example.gemmachat.core.Shelter
import com.example.gemmachat.data.BdGeo
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
    val districtEn: String = "",
    val districtBn: String = "",
    val detailed: Boolean = false,          // true = full pack with flood-avoiding routing
    val shelters: List<Shelter> = emptyList(),
    val floodPolys: List<List<DoubleArray>> = emptyList(),
    val graph: Gis.PedGraph? = null,
    val ranked: List<Gis.RankedShelter> = emptyList(),
    val route: Gis.Route? = null,
    val naiveCrossesFlood: Boolean = false,
    val elderly: Boolean = false,
    val computed: Boolean = false,
)

class GisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GemmaChatApplication
    private val ctx = application

    private val _ui = MutableStateFlow(GisUiState())
    val ui: StateFlow<GisUiState> = _ui

    init {
        // Seed the map on the manually-selected region until a GPS fix arrives.
        val r = Regions.byId(app.prefs.activeRegion.value)
        _ui.value = _ui.value.copy(userLat = r.centerLat, userLon = r.centerLon)
    }

    fun setElderly(v: Boolean) {
        _ui.value = _ui.value.copy(elderly = v)
    }

    fun findNearestShelter() {
        if (_ui.value.locating) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(locating = true)
            val loc = app.locationProvider.current()
            val usedGps = loc != null
            val fallback = Regions.byId(app.prefs.activeRegion.value)
            val lat = loc?.first ?: fallback.centerLat
            val lon = loc?.second ?: fallback.centerLon

            val district = BdGeo.nearestDistrict(ctx, lat, lon)
            val (pack, distToPack) = BdGeo.nearestPack(lat, lon)
            val profile = if (_ui.value.elderly) listOf("elderly") else emptyList()
            val detailed = distToPack <= DETAIL_RADIUS_M

            if (detailed) {
                val shelters = RegionAssets.loadShelters(ctx, pack.id)
                val flood = RegionAssets.loadFloodPolys(ctx, pack.id)
                val graph = RegionAssets.loadGraph(ctx, pack.id)
                val ranked = Gis.findNearestShelter(lat, lon, shelters, profile)
                val top = ranked.firstOrNull()
                val route = top?.let { Gis.safeRoute(lat, lon, it.lat, it.lon, graph, flood) }
                val naive = top?.let {
                    Gis.segmentCrossesFlood(doubleArrayOf(lat, lon), doubleArrayOf(it.lat, it.lon), flood)
                } ?: false
                _ui.value = _ui.value.copy(
                    userLat = lat, userLon = lon, usingGps = usedGps, locating = false,
                    districtEn = district.name, districtBn = district.bn, detailed = true,
                    shelters = shelters, floodPolys = flood, graph = graph,
                    ranked = ranked, route = route, naiveCrossesFlood = naive, computed = true,
                )
            } else {
                // No detailed map here yet — nationwide fallback on the union of known shelters.
                val shelters = BdGeo.allShelters(ctx)
                val ranked = Gis.findNearestShelter(lat, lon, shelters, profile)
                _ui.value = _ui.value.copy(
                    userLat = lat, userLon = lon, usingGps = usedGps, locating = false,
                    districtEn = district.name, districtBn = district.bn, detailed = false,
                    shelters = shelters, floodPolys = emptyList(), graph = null,
                    ranked = ranked, route = null, naiveCrossesFlood = false, computed = true,
                )
            }
        }
    }

    companion object {
        // Within this distance of a pack centre we use its detailed routing map.
        const val DETAIL_RADIUS_M = 45_000.0
    }
}
