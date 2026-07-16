package com.example.gemmachat.ui.gis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gemmachat.R
import com.example.gemmachat.core.Gis
import com.example.gemmachat.ui.components.HeroBanner

private val FLOOD = Color(0xFF2196F3)
private val ROAD = Color(0xFF9E9E9E)
private val FLOODED_ROAD = Color(0xFFE53935)
private val ROUTE = Color(0xFFFF9800)
private val SHELTER = Color(0xFF43A047)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GisScreen(viewModel: GisViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsState()

    // bounding box over all geodata (for the mini-map projection)
    val bbox = remember {
        val lats = ArrayList<Double>(); val lons = ArrayList<Double>()
        viewModel.graph.nodes.values.forEach { lats.add(it[0]); lons.add(it[1]) }
        viewModel.shelters.forEach { lats.add(it.lat); lons.add(it.lon) }
        viewModel.floodPolys.forEach { r -> r.forEach { lats.add(it[1]); lons.add(it[0]) } }
        lats.add(viewModel.userLat); lons.add(viewModel.userLon)
        doubleArrayOf(lats.min(), lats.max(), lons.min(), lons.max())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disha · Safe Shelter & Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            HeroBanner(R.drawable.hero_shelter, 128.dp,
                title = "Safe Shelter & Route", subtitle = "Nearest shelter, flood-safe route")
            Spacer(Modifier.height(12.dp))
            Text("Offline map · Chattogram demo pack. Your location: Halishahar (near flooding).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = ui.elderly, onCheckedChange = { viewModel.setElderly(it) })
                Spacer(Modifier.width(8.dp))
                Text("Elderly / needs accessible shelter", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.findNearestShelter() }) {
                Text("Find nearest safe shelter")
            }

            // ---- mini map ---- //
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Canvas(Modifier.fillMaxWidth().height(280.dp).padding(8.dp)) {
                    val w = size.width; val h = size.height; val p = 20f
                    val minLat = bbox[0]; val maxLat = bbox[1]; val minLon = bbox[2]; val maxLon = bbox[3]
                    val dLat = (maxLat - minLat).let { if (it == 0.0) 1e-6 else it }
                    val dLon = (maxLon - minLon).let { if (it == 0.0) 1e-6 else it }
                    fun ox(lon: Double) = (((lon - minLon) / dLon).toFloat()) * (w - 2 * p) + p
                    fun oy(lat: Double) = (((maxLat - lat) / dLat).toFloat()) * (h - 2 * p) + p

                    // flood polygon
                    viewModel.floodPolys.forEach { ring ->
                        val path = Path()
                        ring.forEachIndexed { i, pt ->
                            val x = ox(pt[0]); val y = oy(pt[1])
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, FLOOD, alpha = 0.22f)
                    }
                    // roads (red dashed = flooded)
                    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    viewModel.graph.edges.forEach { (u, v) ->
                        val a = viewModel.graph.nodes.getValue(u)
                        val b = viewModel.graph.nodes.getValue(v)
                        val flooded = Gis.segmentCrossesFlood(a, b, viewModel.floodPolys)
                        drawLine(
                            color = if (flooded) FLOODED_ROAD else ROAD,
                            start = Offset(ox(a[1]), oy(a[0])), end = Offset(ox(b[1]), oy(b[0])),
                            strokeWidth = 3f, pathEffect = if (flooded) dash else null,
                        )
                    }
                    // route (orange)
                    ui.route?.polyline?.let { poly ->
                        for (i in 0 until poly.size - 1) {
                            drawLine(ROUTE,
                                Offset(ox(poly[i][1]), oy(poly[i][0])),
                                Offset(ox(poly[i + 1][1]), oy(poly[i + 1][0])), strokeWidth = 8f)
                        }
                    }
                    // shelters (green) + user (black)
                    viewModel.shelters.forEach { s ->
                        drawCircle(SHELTER, radius = 12f, center = Offset(ox(s.lon), oy(s.lat)))
                    }
                    drawCircle(Color.Black, radius = 12f,
                        center = Offset(ox(viewModel.userLon), oy(viewModel.userLat)))
                }
            }
            Text("● you   ● shelter   ▬ safe route   ┈ flooded road   ▨ flood zone",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp))

            // ---- results ---- //
            ui.route?.let { r ->
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        val top = ui.ranked.firstOrNull()
                        Text("→ ${top?.name ?: "Shelter"}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Walking route: ${r.distM} m", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (r.crossesFlood) "⚠ No fully dry route — this path crosses floodwater."
                            else "✓ Route avoids flooded roads" +
                                if (ui.naiveCrossesFlood) " (the direct line would have crossed flooding)." else ".",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (r.crossesFlood) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (ui.ranked.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Ranked shelters", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                ui.ranked.forEach { s -> ShelterRow(s) }
            } else if (!ui.computed) {
                Spacer(Modifier.height(12.dp))
                Text("Tap “Find nearest safe shelter” to rank shelters and draw a flood-avoiding route.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ShelterRow(s: Gis.RankedShelter) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (s.onHighGround) {
                    Spacer(Modifier.width(8.dp))
                    Text("HIGH-GROUND", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("${s.distM} m · ${s.capacityLeft} spaces free · score ${"%.3f".format(s.score)}",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
