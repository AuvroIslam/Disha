package com.example.gemmachat.ui.triage

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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

private val EXAMPLES = listOf(
    "Pregnant woman trapped on the rooftop, water is still rising fast, no food since morning.",
    "My father is not breathing after we pulled him out of the floodwater.",
    "Elderly man has heavy bleeding from a deep leg cut, blood soaking the cloth.",
    "We are four people safe on the second floor but out of drinking water.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageScreen(viewModel: TriageViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    var text by remember { mutableStateOf("") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.setImageFromUri(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disha · Rescue Triage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize(),
        ) {
            HeroBanner(R.drawable.hero_triage, 118.dp,
                title = "Rescue Triage", subtitle = "Prioritise SOS by urgency")
            Spacer(Modifier.height(12.dp))
            // engine status line
            when {
                ui.engineLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Loading on-device Gemma 4…", style = MaterialTheme.typography.bodySmall)
                }
                ui.engineReady -> Text(
                    "● On-device Gemma 4 ready — triage runs offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                else -> Text(
                    "Model not loaded — using rule-based fallback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Describe the SOS") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            Spacer(Modifier.height(8.dp))
            Text("Try an example:", style = MaterialTheme.typography.labelMedium)
            EXAMPLES.forEach { ex ->
                AssistChip(
                    onClick = { text = ex },
                    label = { Text(ex.take(42) + "…", maxLines = 1) },
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("📷 Attach photo") }
                if (ui.imagePath != null) {
                    Spacer(Modifier.width(8.dp))
                    Text("attached ✓", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { viewModel.clearImage() }) { Text("remove") }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row {
                Button(
                    onClick = { viewModel.triage(text) },
                    enabled = !ui.busy && (text.isNotBlank() || ui.imagePath != null),
                ) {
                    if (ui.busy) {
                        CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Triaging on-device…")
                    } else {
                        Text("Run Triage")
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (ui.queue.isNotEmpty()) {
                    OutlinedButton(onClick = { viewModel.clearQueue() }, enabled = !ui.busy) {
                        Text("Clear")
                    }
                }
            }

            ui.error?.let {
                Spacer(Modifier.height(8.dp))
                Text("Error: $it", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "Prioritized queue (${ui.queue.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.queue) { item -> TriageCard(item) }
            }
        }
    }
}

@Composable
private fun TriageCard(item: TriagedItem) {
    val r = item.result
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.color)
                Spacer(Modifier.width(8.dp))
                Text(r.priority.uppercase(), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("score ${"%.2f".format(r.urgencyScore)}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (r.producedBy == "gemma") "Gemma" else "fallback",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(item.text, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Why: ${r.rationale}", style = MaterialTheme.typography.bodySmall)
            Text("Action: ${r.recommendedAction}", style = MaterialTheme.typography.bodySmall)
            if (r.riskSignals.isNotEmpty()) {
                Text("Signals: ${r.riskSignals.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall)
            }
            if (r.needsHumanReview) {
                Text("⚠ Needs human review", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
