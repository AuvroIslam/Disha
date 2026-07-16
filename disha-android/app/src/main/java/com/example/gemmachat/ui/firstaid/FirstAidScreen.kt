package com.example.gemmachat.ui.firstaid

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gemmachat.R
import com.example.gemmachat.ui.components.HeroBanner

private val EXAMPLES = listOf(
    "Someone is bleeding heavily from a deep cut on the leg.",
    "We pulled someone from the floodwater and they are not breathing.",
    "A snake bit my brother's foot near the water.",
    "A child swallowed floodwater and is vomiting.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstAidScreen(viewModel: FirstAidViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disha · First Aid") },
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
            HeroBanner(R.drawable.hero_firstaid,
                title = "First Aid", subtitle = "Trusted, cited guidance")
            Spacer(Modifier.height(12.dp))
            when {
                ui.engineLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Loading on-device Gemma 4…", style = MaterialTheme.typography.bodySmall)
                }
                ui.engineReady -> Text(
                    "● Grounded in offline first aid guidance (WHO, IFRC, Red Cross), answered by Gemma 4.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                else -> Text("Model not loaded — showing the source passages directly.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("Describe the injury / situation") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
            )
            Spacer(Modifier.height(8.dp))
            Text("Try an example:", style = MaterialTheme.typography.labelMedium)
            EXAMPLES.forEach { ex ->
                AssistChip(onClick = { text = ex },
                    label = { Text(ex.take(40) + "…", maxLines = 1) },
                    modifier = Modifier.padding(vertical = 2.dp))
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.ask(text) }, enabled = !ui.busy && text.isNotBlank()) {
                if (ui.busy) {
                    CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Thinking on-device…")
                } else {
                    Text("Get First Aid Steps")
                }
            }

            ui.error?.let {
                Spacer(Modifier.height(8.dp))
                Text("Error: $it", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            if (ui.redFlag) {
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D))) {
                    Text(
                        "🚨 জীবন-সংকটজনক — এখনই সাহায্য নিন / LIFE-THREATENING: seek emergency help NOW.",
                        Modifier.padding(12.dp), color = Color.White,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }

            ui.answer?.let { ans ->
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(ans, style = MaterialTheme.typography.bodyMedium)
                        if (ui.citations.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("Sources:", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold)
                            ui.citations.forEach { c ->
                                Text("[${c.n}] ${c.source} · ${c.pack}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
