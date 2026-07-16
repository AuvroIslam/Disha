package com.example.gemmachat.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DishaHomeScreen(
    onTriage: () -> Unit,
    onFirstAid: () -> Unit,
    onGis: () -> Unit,
    onSummary: () -> Unit,
    onMesh: () -> Unit,
    onChat: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Disha", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "দিশা — offline AI disaster-response companion, powered by on-device Gemma 4.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        HomeTile(
            emoji = "🚑",
            title = "Rescue Triage",
            subtitle = "Score & prioritise SOS reports with on-device Gemma",
            onClick = onTriage,
        )
        Spacer(Modifier.height(12.dp))
        HomeTile(
            emoji = "🩹",
            title = "First Aid",
            subtitle = "Grounded, cited steps from offline WHO/IFRC packs",
            onClick = onFirstAid,
        )
        Spacer(Modifier.height(12.dp))
        HomeTile(
            emoji = "🗺️",
            title = "Safe Shelter & Route",
            subtitle = "Nearest shelter + a flood-avoiding route on an offline map",
            onClick = onGis,
        )
        Spacer(Modifier.height(12.dp))
        HomeTile(
            emoji = "📋",
            title = "Coordinator Summary",
            subtitle = "Gemma briefing over triaged field reports",
            onClick = onSummary,
        )
        Spacer(Modifier.height(12.dp))
        HomeTile(
            emoji = "📡",
            title = "Mesh SOS",
            subtitle = "Send SOS phone-to-phone, no internet (signed + relayed)",
            onClick = onMesh,
        )
        Spacer(Modifier.height(12.dp))
        HomeTile(
            emoji = "💬",
            title = "AI Assistant",
            subtitle = "Ask for flood safety & first-aid guidance (offline)",
            onClick = onChat,
        )
    }
}

@Composable
private fun HomeTile(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
