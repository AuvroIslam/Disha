package com.example.gemmachat.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemmachat.R
import com.example.gemmachat.ui.theme.AccentPurple
import com.example.gemmachat.ui.theme.BgCard
import com.example.gemmachat.ui.theme.TextPrimary
import com.example.gemmachat.ui.theme.TextSecondary
import com.example.gemmachat.ui.theme.TileAidBg
import com.example.gemmachat.ui.theme.TileAidFg
import com.example.gemmachat.ui.theme.TileChatBg
import com.example.gemmachat.ui.theme.TileChatFg
import com.example.gemmachat.ui.theme.TileMeshBg
import com.example.gemmachat.ui.theme.TileMeshFg
import com.example.gemmachat.ui.theme.TileShelterBg
import com.example.gemmachat.ui.theme.TileShelterFg
import com.example.gemmachat.ui.theme.TileSummaryBg
import com.example.gemmachat.ui.theme.TileSummaryFg
import com.example.gemmachat.ui.theme.TileTriageBg
import com.example.gemmachat.ui.theme.TileTriageFg

@Composable
fun DishaHomeScreen(
    onTriage: () -> Unit,
    onFirstAid: () -> Unit,
    onGis: () -> Unit,
    onSummary: () -> Unit,
    onMesh: () -> Unit,
    onChat: () -> Unit,
    onGuide: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        // ---- Header: logo + brand + actions ----
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(painterResource(R.drawable.disha_logo), contentDescription = null,
                modifier = Modifier.size(46.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("দিশা", color = AccentPurple, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                Text("DISHA", color = AccentPurple, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }
            Spacer(Modifier.weight(1f))
            CircleIconButton(Icons.Filled.HelpOutline, onClick = onGuide)
            Spacer(Modifier.width(10.dp))
            CircleIconButton(Icons.Filled.Settings, onClick = onSettings)
        }

        Spacer(Modifier.height(18.dp))
        Text("Assalamu Alaikum 👋", color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(2.dp))
        Text("কিভাবে সাহায্য করতে পারি?", color = TextPrimary,
            fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp)
        Spacer(Modifier.height(6.dp))
        Text("Your offline AI assistant for flood emergencies",
            color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

        // ---- Hero illustration ----
        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(R.drawable.hero_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(24.dp)),
        )

        // ---- Offline mode status card ----
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(AccentPurple.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Shield, null, tint = AccentPurple, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("You are in ", color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Offline Mode", color = AccentPurple, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(" ●", color = Color(0xFF22A565),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("All systems ready. AI is running on this device.",
                        color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ---- Feature grid (2 x 3) ----
        Spacer(Modifier.height(18.dp))
        FeatureRow(
            left = { FeatureCard(TileTriageBg, TileTriageFg, Icons.Filled.Warning, "Rescue Triage",
                "Prioritize SOS and urgent cases", onTriage, it) },
            right = { FeatureCard(TileAidBg, TileAidFg, Icons.Filled.MedicalServices, "First Aid",
                "Step-by-step medical guidance", onFirstAid, it) },
        )
        Spacer(Modifier.height(14.dp))
        FeatureRow(
            left = { FeatureCard(TileShelterBg, TileShelterFg, Icons.Filled.Place, "Safe Shelter & Route",
                "Find safe places and best routes", onGis, it) },
            right = { FeatureCard(TileSummaryBg, TileSummaryFg, Icons.Filled.BarChart, "Coordinator Summary",
                "Overview, reports and insights", onSummary, it) },
        )
        Spacer(Modifier.height(14.dp))
        FeatureRow(
            left = { FeatureCard(TileMeshBg, TileMeshFg, Icons.Filled.Sensors, "Mesh SOS",
                "Send & receive SOS offline via mesh", onMesh, it) },
            right = { FeatureCard(TileChatBg, TileChatFg, Icons.AutoMirrored.Filled.Chat, "AI Assistant",
                "Ask anything about flood safety & first aid", onChat, it) },
        )
    }
}

@Composable
private fun FeatureRow(
    left: @Composable (Modifier) -> Unit,
    right: @Composable (Modifier) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        left(Modifier.weight(1f))
        Spacer(Modifier.width(14.dp))
        right(Modifier.weight(1f))
    }
}

@Composable
private fun FeatureCard(
    bg: Color, fg: Color, icon: ImageVector, title: String, subtitle: String,
    onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(196.dp).clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(fg.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(BgCard).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
    }
}
