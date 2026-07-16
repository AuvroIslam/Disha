package com.example.gemmachat.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SummaryScreen(viewModel: SummaryViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disha · Coordinator Summary") },
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
            Text(
                "Aggregates incoming field reports. Counts are computed on-device (never hallucinated); " +
                    "Gemma 4 writes the briefing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(12.dp))
            Button(onClick = { viewModel.generate() }, enabled = !ui.busy) {
                if (ui.busy) {
                    CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Writing briefing…")
                } else {
                    Text("Generate coordinator briefing")
                }
            }

            ui.error?.let {
                Spacer(Modifier.height(8.dp))
                Text("Error: $it", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            ui.stats?.let { st ->
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("Total ${st.totalSos}") })
                    AssistChip(onClick = {}, label = { Text("🔴 ${st.critical}") })
                    AssistChip(onClick = {}, label = { Text("🟠 ${st.high}") })
                    AssistChip(onClick = {}, label = { Text("🟡 ${st.moderate}") })
                    AssistChip(onClick = {}, label = { Text("🟢 ${st.low}") })
                }
            }

            ui.briefing?.let { b ->
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    Text(b, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
