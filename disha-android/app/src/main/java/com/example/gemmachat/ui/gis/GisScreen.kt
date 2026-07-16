package com.example.gemmachat.ui.gis

import android.Manifest
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
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.gemmachat.ui.i18n.LocalBangla
import com.example.gemmachat.ui.i18n.tr
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

private val FLOOD = Color(0xFF2196F3)
private val ROAD = Color(0xFF9E9E9E)
private val FLOODED_ROAD = Color(0xFFE53935)
private val ROUTE = Color(0xFFFF9800)
private val SHELTER = Color(0xFF43A047)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GisScreen(viewModel: GisViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    val locationPerm = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) {
        viewModel.findNearestShelter()
    }

    // bounding box over the current geodata (for the mini-map projection)
    val bbox = remember(ui.userLat, ui.userLon, ui.shelters, ui.detailed, ui.ranked) {
        val lats = ArrayList<Double>(); val lons = ArrayList<Double>()
        ui.graph?.nodes?.values?.forEach { lats.add(it[0]); lons.add(it[1]) }
        ui.floodPolys.forEach { r -> r.forEach { lats.add(it[1]); lons.add(it[0]) } }
        // In the nationwide view only the nearest few shelters matter for framing.
        val pts = if (ui.detailed) ui.shelters.map { it.lat to it.lon }
        else ui.ranked.map { it.lat to it.lon }
        pts.forEach { lats.add(it.first); lons.add(it.second) }
        lats.add(ui.userLat); lons.add(ui.userLon)
        if (lats.size < 2) { lats.add(ui.userLat + 0.02); lons.add(ui.userLon + 0.02) }
        doubleArrayOf(lats.min(), lats.max(), lons.min(), lons.max())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Disha · Safe Shelter & Route", "দিশা · নিরাপদ আশ্রয় ও পথ")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            HeroBanner(R.drawable.hero_shelter,
                title = tr("Safe Shelter", "নিরাপদ আশ্রয়"),
                subtitle = tr("Nearest shelter, safest way there", "নিকটতম আশ্রয়, নিরাপদতম পথ"))
            Spacer(Modifier.height(12.dp))
            val district = if (LocalBangla.current) ui.districtBn else ui.districtEn
            Text(
                when {
                    ui.locating -> tr("Locating you via GPS…", "জিপিএস দিয়ে অবস্থান নেওয়া হচ্ছে…")
                    !ui.computed -> tr("Works anywhere in Bangladesh. Grant location for a precise result.",
                        "বাংলাদেশের যেকোনো জায়গায় কাজ করে। নির্ভুল ফলাফলের জন্য লোকেশন অনুমতি দিন।")
                    ui.detailed -> tr("You are in $district. Detailed offline map available.",
                        "আপনি $district-এ আছেন। বিস্তারিত অফলাইন মানচিত্র রয়েছে।") +
                        (if (ui.usingGps) "" else tr(" (GPS off — using region centre.)", " (জিপিএস বন্ধ — এলাকার কেন্দ্র।)"))
                    else -> tr("You are in $district. No detailed map here yet — showing safe-direction guidance.",
                        "আপনি $district-এ আছেন। এখানে বিস্তারিত মানচিত্র নেই — নিরাপদ দিকনির্দেশনা দেখানো হচ্ছে।")
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = ui.elderly,
                    onCheckedChange = { viewModel.setElderly(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFC9C2E8),
                        uncheckedBorderColor = Color(0xFFC9C2E8),
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text(tr("Elderly / needs accessible shelter", "বয়স্ক / সুবিধাজনক আশ্রয় প্রয়োজন"),
                    style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                if (locationPerm.status.isGranted) viewModel.findNearestShelter()
                else locationPerm.launchPermissionRequest()
            }) {
                Text(if (ui.locating) tr("Locating…", "অবস্থান নেওয়া হচ্ছে…")
                    else tr("Find nearest safe shelter", "নিকটতম নিরাপদ আশ্রয় খুঁজুন"))
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

                    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    // flood polygon
                    ui.floodPolys.forEach { ring ->
                        val path = Path()
                        ring.forEachIndexed { i, pt ->
                            val x = ox(pt[0]); val y = oy(pt[1])
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, FLOOD, alpha = 0.22f)
                    }
                    // roads (red dashed = flooded) — only in detailed mode
                    ui.graph?.let { g ->
                        g.edges.forEach { (u, v) ->
                            val a = g.nodes.getValue(u); val b = g.nodes.getValue(v)
                            val flooded = Gis.segmentCrossesFlood(a, b, ui.floodPolys)
                            drawLine(
                                color = if (flooded) FLOODED_ROAD else ROAD,
                                start = Offset(ox(a[1]), oy(a[0])), end = Offset(ox(b[1]), oy(b[0])),
                                strokeWidth = 3f, pathEffect = if (flooded) dash else null,
                            )
                        }
                    }
                    // detailed route (orange), else a dashed direction line to the nearest shelter
                    val poly = ui.route?.polyline
                    if (poly != null) {
                        for (i in 0 until poly.size - 1) {
                            drawLine(ROUTE,
                                Offset(ox(poly[i][1]), oy(poly[i][0])),
                                Offset(ox(poly[i + 1][1]), oy(poly[i + 1][0])), strokeWidth = 8f)
                        }
                    } else ui.ranked.firstOrNull()?.let { top ->
                        drawLine(ROUTE,
                            Offset(ox(ui.userLon), oy(ui.userLat)),
                            Offset(ox(top.lon), oy(top.lat)), strokeWidth = 5f, pathEffect = dash)
                    }
                    // shelters (green) + user (black)
                    val shownShelters = if (ui.detailed) ui.shelters
                    else ui.shelters.filter { s -> ui.ranked.any { it.lat == s.lat && it.lon == s.lon } }
                    shownShelters.forEach { s ->
                        drawCircle(SHELTER, radius = 12f, center = Offset(ox(s.lon), oy(s.lat)))
                    }
                    drawCircle(Color.Black, radius = 12f,
                        center = Offset(ox(ui.userLon), oy(ui.userLat)))
                }
            }
            Text(
                if (ui.detailed)
                    tr("● you   ● shelter   ▬ safe route   ┈ flooded road   ▨ flood zone",
                        "● আপনি   ● আশ্রয়   ▬ নিরাপদ পথ   ┈ বন্যা রাস্তা   ▨ বন্যা এলাকা")
                else tr("● you   ● nearest shelter   ┈ direction",
                    "● আপনি   ● নিকটতম আশ্রয়   ┈ দিক"),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp))

            // ---- results ---- //
            // Detailed mode: flood-avoiding walking route card.
            ui.route?.let { r ->
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        val top = ui.ranked.firstOrNull()
                        Text("→ ${top?.name ?: tr("Shelter", "আশ্রয়")}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${tr("Walking route", "হাঁটার পথ")}: ${fmtDist(r.distM)}",
                            style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (r.crossesFlood)
                                tr("⚠ No fully dry route — this path crosses floodwater.",
                                    "⚠ সম্পূর্ণ শুকনো পথ নেই — এই পথ বন্যার পানি পার হয়।")
                            else tr("✓ Route avoids flooded roads", "✓ পথটি বন্যা কবলিত রাস্তা এড়ায়") +
                                if (ui.naiveCrossesFlood)
                                    tr(" (the direct line would have crossed flooding).",
                                        " (সরাসরি পথ বন্যা পার হতো)।") else ".",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (r.crossesFlood) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Nationwide fallback: safe-direction guidance (no detailed pack for this district).
            if (ui.computed && !ui.detailed) {
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(tr("Move to safety", "নিরাপদে সরে যান"),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tr("Head to the nearest high ground or a strong multi-storey building — a school, " +
                                "college or Union Parishad often serves as a flood shelter. Do not cross " +
                                "fast-moving water; even knee-deep flow can sweep you away.",
                                "নিকটতম উঁচু জায়গা বা মজবুত বহুতল ভবনে যান — স্কুল, কলেজ বা ইউনিয়ন পরিষদ প্রায়ই " +
                                    "বন্যা আশ্রয় হিসেবে ব্যবহৃত হয়। দ্রুত বয়ে চলা পানি পার হবেন না; হাঁটু-সমান স্রোতও " +
                                    "আপনাকে ভাসিয়ে নিতে পারে।"),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (ui.ranked.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (ui.detailed) tr("Ranked shelters", "সাজানো আশ্রয়কেন্দ্র")
                    else tr("Nearest known shelters", "নিকটতম পরিচিত আশ্রয়"),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ui.ranked.forEach { s -> ShelterRow(s) }
            } else if (!ui.computed) {
                Spacer(Modifier.height(12.dp))
                Text(tr("Tap “Find nearest safe shelter”. It uses your GPS and works anywhere in Bangladesh.",
                    "“নিকটতম নিরাপদ আশ্রয় খুঁজুন”-এ চাপ দিন। এটি আপনার জিপিএস ব্যবহার করে, বাংলাদেশের যেকোনো জায়গায় কাজ করে।"),
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
                    Text(tr("HIGH GROUND", "উঁচু জায়গা"), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("${fmtDist(s.distM)} · ${s.capacityLeft} ${tr("spaces free", "জায়গা খালি")} · " +
                "${tr("score", "স্কোর")} ${"%.3f".format(s.score)}",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun fmtDist(m: Int): String = if (m >= 1000) "%.1f km".format(m / 1000.0) else "$m m"
