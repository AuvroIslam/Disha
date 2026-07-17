package com.example.gemmachat.ui.gis

import android.Manifest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertTriangle
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Maximize2
import compose.icons.feathericons.X
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.example.gemmachat.R
import com.example.gemmachat.core.Gis
import com.example.gemmachat.data.PublicShelterHit
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
private val USER = Color(0xFF1B1030)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GisScreen(viewModel: GisViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    val locationPerm = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) {
        viewModel.findNearestShelter()
    }

    // Frame the map on what matters — you, the route and the shelters — not the whole road
    // extract, so the default view is readable. Pinch-zoom lets the user go closer.
    val bbox = remember(ui.userLat, ui.userLon, ui.shelters, ui.detailed, ui.nearbyPublic, ui.route) {
        val lats = ArrayList<Double>(); val lons = ArrayList<Double>()
        ui.route?.polyline?.forEach { lats.add(it[0]); lons.add(it[1]) }
        ui.floodPolys.forEach { r -> r.forEach { lats.add(it[1]); lons.add(it[0]) } }
        val pts = if (ui.detailed) ui.shelters.map { it.lat to it.lon }
        else ui.nearbyPublic.map { it.shelter.lat to it.shelter.lon }
        pts.forEach { lats.add(it.first); lons.add(it.second) }
        lats.add(ui.userLat); lons.add(ui.userLon)
        if (lats.size < 2) { lats.add(ui.userLat + 0.02); lons.add(ui.userLon + 0.02) }
        val padLat = (lats.max() - lats.min()).coerceAtLeast(0.004) * 0.12
        val padLon = (lons.max() - lons.min()).coerceAtLeast(0.004) * 0.12
        doubleArrayOf(lats.min() - padLat, lats.max() + padLat, lons.min() - padLon, lons.max() + padLon)
    }
    var showFullMap by remember { mutableStateOf(false) }

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

            if (ui.detailed) {
                Spacer(Modifier.height(12.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(FeatherIcons.AlertTriangle, null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr("The flood zone below is a sample scenario for practicing routes — " +
                                "not live flood data. The route avoids that sample zone, not confirmed " +
                                "real floodwater. Check local conditions before you travel.",
                                "নিচের বন্যা এলাকা পথ অনুশীলনের জন্য একটি নমুনা দৃশ্য — লাইভ বন্যা তথ্য নয়। " +
                                    "পথটি সেই নমুনা এলাকা এড়ায়, প্রকৃত বন্যার পানি নিশ্চিত নয়। যাত্রার আগে স্থানীয় " +
                                    "পরিস্থিতি যাচাই করুন।"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // ---- mini map — static preview; tap opens a full-screen zoomable map ---- //
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().height(260.dp).clipToBounds()
                        .clickable { showFullMap = true },
                ) {
                    Canvas(Modifier.fillMaxSize().padding(10.dp)) { drawShelterMap(ui, bbox) }
                    Row(
                        Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .clip(RoundedCornerShape(8.dp)).background(Color(0xCC1B1030))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(FeatherIcons.Maximize2, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(tr("Tap to enlarge", "বড় করতে চাপ দিন"),
                            color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (showFullMap) FullMapDialog(ui, bbox, onClose = { showFullMap = false })
            Spacer(Modifier.height(4.dp))
            MapLegend(ui.detailed)
            if (ui.detailed) {
                Text(
                    tr("Roads: © OpenStreetMap contributors.", "রাস্তা: © OpenStreetMap contributors।"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp))
            }

            // ---- results ---- //
            // Detailed mode: flood-avoiding walking route card.
            ui.route?.let { r ->
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        val top = ui.ranked.firstOrNull { it.shelterId == ui.selectedShelterId }
                            ?: ui.ranked.firstOrNull()
                        Text("→ ${top?.name ?: tr("Shelter", "আশ্রয়")}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${tr("Walking route", "হাঁটার পথ")}: ${fmtDist(r.distM)}",
                            style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (r.crossesFlood)
                                tr("⚠ No route avoiding the sample flood zone — this path crosses it.",
                                    "⚠ নমুনা বন্যা এলাকা এড়ানো কোনো পথ নেই — এই পথ সেটি পার হয়।")
                            else tr("✓ Route avoids the sample flood zone", "✓ পথটি নমুনা বন্যা এলাকা এড়ায়") +
                                if (ui.naiveCrossesFlood)
                                    tr(" (the direct line would have crossed it).",
                                        " (সরাসরি পথ সেটি পার হতো)।") else ".",
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

            if (ui.detailed && ui.ranked.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(tr("Ranked shelters", "সাজানো আশ্রয়কেন্দ্র"),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ui.ranked.forEach { s ->
                    ShelterRow(s, selected = s.shelterId == ui.selectedShelterId,
                        onClick = { viewModel.selectShelter(s) })
                }
            } else if (!ui.detailed && ui.nearbyPublic.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(tr("Nearest shelters (schools & colleges)", "নিকটতম আশ্রয় (স্কুল ও কলেজ)"),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(tr("Public schools and colleges — commonly used as flood shelters.",
                    "সরকারি স্কুল ও কলেজ — সাধারণত বন্যা আশ্রয় হিসেবে ব্যবহৃত হয়।"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ui.nearbyPublic.forEach { h -> PublicShelterRow(h) }
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
private fun ShelterRow(s: Gis.RankedShelter, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)
            .let {
                if (selected) it.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else it
            },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                if (s.onHighGround) {
                    Spacer(Modifier.width(8.dp))
                    HighGroundBadge()
                }
            }
            Text("${fmtDist(s.distM)} · ${s.capacityLeft} ${tr("spaces free", "জায়গা খালি")} · " +
                "${tr("score", "স্কোর")} ${"%.3f".format(s.score)}",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HighGroundBadge() {
    Text(
        tr("HIGH GROUND", "উঁচু জায়গা"),
        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun PublicShelterRow(h: PublicShelterHit) {
    val typeLabel = when (h.shelter.type) {
        "c" -> tr("College", "কলেজ")
        "u" -> tr("University", "বিশ্ববিদ্যালয়")
        else -> tr("School", "স্কুল")
    }
    val place = listOf(h.shelter.upazila, h.shelter.district)
        .filter { it.isNotBlank() }.joinToString(", ")
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(h.shelter.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text("$typeLabel · ${fmtDist(h.distM)}" + if (place.isNotBlank()) " · $place" else "",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun fmtDist(m: Int): String = if (m >= 1000) "%.1f km".format(m / 1000.0) else "$m m"

@Composable
private fun MapLegend(detailed: Boolean) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(USER, ring = true)
            Spacer(Modifier.width(4.dp))
            Text(tr("you", "আপনি"), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(14.dp))
            LegendDot(SHELTER)
            Spacer(Modifier.width(4.dp))
            Text(if (detailed) tr("shelter", "আশ্রয়") else tr("nearest shelter", "নিকটতম আশ্রয়"),
                style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (detailed) {
                LegendLine(ROUTE)
                Spacer(Modifier.width(4.dp))
                Text(tr("safe route", "নিরাপদ পথ"), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(14.dp))
                LegendLine(FLOODED_ROAD, dashed = true)
                Spacer(Modifier.width(4.dp))
                Text(tr("flooded road", "বন্যা রাস্তা"), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(14.dp))
                LegendSwatch(FLOOD)
                Spacer(Modifier.width(4.dp))
                Text(tr("sample flood zone", "নমুনা বন্যা এলাকা"), style = MaterialTheme.typography.labelSmall)
            } else {
                LegendLine(ROUTE, dashed = true)
                Spacer(Modifier.width(4.dp))
                Text(tr("direction", "দিক"), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, ring: Boolean = false) {
    Box(
        Modifier.size(10.dp).clip(CircleShape)
            .background(if (ring) Color.White else color)
            .let { if (ring) it.border(1.5.dp, color, CircleShape) else it },
    )
}

@Composable
private fun LegendLine(color: Color, dashed: Boolean = false) {
    if (dashed) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).height(3.dp).background(color))
            Spacer(Modifier.width(2.dp))
            Box(Modifier.width(6.dp).height(3.dp).background(color))
        }
    } else {
        Box(Modifier.width(18.dp).height(3.dp).background(color))
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(Modifier.size(10.dp).background(color.copy(alpha = 0.35f)))
}

@Composable
private fun FullMapDialog(ui: GisUiState, bbox: DoubleArray, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var pan by remember { mutableStateOf(Offset.Zero) }
        Box(Modifier.fillMaxSize().background(Color(0xFF0E0A1A))) {
            Canvas(
                Modifier.fillMaxSize().padding(16.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, panChange, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 8f)
                            pan += panChange
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        translationX = pan.x; translationY = pan.y
                    },
            ) { drawShelterMap(ui, bbox) }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(FeatherIcons.X, contentDescription = "Close", tint = Color.White)
            }
            Text(tr("Pinch to zoom · drag to pan", "জুম করতে দুই আঙুল · সরাতে টানুন"),
                color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

/** Shared renderer for the shelter mini-map, used by both the inline preview and the dialog. */
private fun DrawScope.drawShelterMap(ui: GisUiState, bbox: DoubleArray) {
    val w = size.width; val h = size.height; val p = 14f
    val minLat = bbox[0]; val maxLat = bbox[1]; val minLon = bbox[2]; val maxLon = bbox[3]
    val dLat = (maxLat - minLat).let { if (it == 0.0) 1e-6 else it }
    val dLon = (maxLon - minLon).let { if (it == 0.0) 1e-6 else it }
    fun ox(lon: Double) = (((lon - minLon) / dLon).toFloat()) * (w - 2 * p) + p
    fun oy(lat: Double) = (((maxLat - lat) / dLat).toFloat()) * (h - 2 * p) + p

    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
    ui.floodPolys.forEach { ring ->
        val path = Path()
        ring.forEachIndexed { i, pt ->
            val x = ox(pt[0]); val y = oy(pt[1])
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, FLOOD, alpha = 0.28f)
    }
    ui.graph?.let { g ->
        g.edges.forEach { (u, v) ->
            val a = g.nodes.getValue(u); val b = g.nodes.getValue(v)
            val flooded = Gis.segmentCrossesFlood(a, b, ui.floodPolys)
            drawLine(
                color = if (flooded) FLOODED_ROAD else ROAD.copy(alpha = 0.35f),
                start = Offset(ox(a[1]), oy(a[0])), end = Offset(ox(b[1]), oy(b[0])),
                strokeWidth = if (flooded) 2.5f else 1.6f,
                pathEffect = if (flooded) dash else null,
            )
        }
    }
    val poly = ui.route?.polyline
    if (poly != null) {
        for (i in 0 until poly.size - 1) {
            drawLine(ROUTE, Offset(ox(poly[i][1]), oy(poly[i][0])),
                Offset(ox(poly[i + 1][1]), oy(poly[i + 1][0])), strokeWidth = 9f, cap = StrokeCap.Round)
        }
    } else ui.nearbyPublic.firstOrNull()?.let { top ->
        drawLine(ROUTE, Offset(ox(ui.userLon), oy(ui.userLat)),
            Offset(ox(top.shelter.lon), oy(top.shelter.lat)),
            strokeWidth = 5f, pathEffect = dash, cap = StrokeCap.Round)
    }
    val shelterPts = if (ui.detailed) ui.shelters.map { it.id to (it.lon to it.lat) }
    else ui.nearbyPublic.map { it.shelter.name to (it.shelter.lon to it.shelter.lat) }
    shelterPts.forEach { (id, ll) ->
        val (lon, lat) = ll
        val selected = ui.detailed && id == ui.selectedShelterId
        val c = Offset(ox(lon), oy(lat))
        if (selected) {
            drawCircle(Color.White, 15f, c); drawCircle(ROUTE, 11f, c)
        } else {
            drawCircle(Color.White, 12f, c); drawCircle(SHELTER, 8.5f, c)
        }
    }
    val uc = Offset(ox(ui.userLon), oy(ui.userLat))
    drawCircle(Color.White, 14f, uc); drawCircle(USER, 10f, uc)
}
