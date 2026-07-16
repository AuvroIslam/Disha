package com.example.gemmachat.ui.mesh

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gemmachat.R
import com.example.gemmachat.ui.components.HeroBanner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MeshScreen(viewModel: MeshViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    var text by remember { mutableStateOf("") }

    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
    }
    val permState = rememberMultiplePermissionsState(permissions)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disha · Mesh SOS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
            if (!permState.allPermissionsGranted) {
                Text("Offline mesh needs Bluetooth, Nearby-Wi-Fi and location permissions to " +
                    "find nearby phones (no internet is used).",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permState.launchMultiplePermissionRequest() }) {
                    Text("Grant mesh permissions")
                }
                return@Column
            }

            LaunchedEffect(Unit) { viewModel.start() }
            DisposableEffect(Unit) { onDispose { viewModel.stop() } }

            HeroBanner(R.drawable.hero_mesh, 116.dp,
                title = "Mesh SOS", subtitle = "Send SOS, no internet")
            Spacer(Modifier.height(12.dp))

            Text("● ${ui.status}", style = MaterialTheme.typography.bodyMedium,
                color = if (ui.peers > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant)
            Text("This device: ${viewModel.localName}  ·  peers: ${ui.peers}",
                style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = text, onValueChange = { text = it },
                label = { Text("SOS message") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            Spacer(Modifier.height(6.dp))
            Row {
                AssistChip(onClick = { text = "Trapped on rooftop, water rising, need boat rescue." },
                    label = { Text("rooftop SOS") })
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = { text = "Elderly man, heavy bleeding, need medic urgently." },
                    label = { Text("medical SOS") })
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.send(text); text = "" },
                enabled = text.isNotBlank()) {
                Text("📡 Broadcast SOS")
            }

            Spacer(Modifier.height(12.dp))
            Text("Messages (${ui.messages.size})", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.messages.reversed()) { m -> MeshRow(m) }
            }
        }
    }
}

@Composable
private fun MeshRow(m: MeshMsg) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.color)
                Spacer(Modifier.width(6.dp))
                Text(m.priority.uppercase(), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (m.mine) "you → broadcast"
                    else "from ${m.sender}" + (if (m.hops > 0) " · ${m.hops} hop(s)" else ""),
                    style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(8.dp))
                if (!m.mine) {
                    Text(if (m.verified) "✓ signed" else "✗ unverified",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (m.verified) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(m.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
