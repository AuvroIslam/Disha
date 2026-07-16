package com.example.gemmachat.ui.guide

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import compose.icons.FeatherIcons
import compose.icons.feathericons.Activity
import compose.icons.feathericons.AlertTriangle
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.BarChart2
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.MessageCircle
import compose.icons.feathericons.Radio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

private data class Feature(
    val icon: ImageVector, val bg: Color, val fg: Color, val title: String, val desc: String,
)

private val FEATURES = listOf(
    Feature(FeatherIcons.AlertTriangle, TileTriageBg, TileTriageFg, "Rescue Triage",
        "Type or photograph an SOS and I rank its urgency, so the worst cases get help first."),
    Feature(FeatherIcons.Activity, TileAidBg, TileAidFg, "First Aid",
        "Describe an injury and I give clear, cited first aid in Bangla and English."),
    Feature(FeatherIcons.MapPin, TileShelterBg, TileShelterFg, "Safe Shelter",
        "I find the nearest safe shelter on high ground, and a way there that avoids flooding."),
    Feature(FeatherIcons.BarChart2, TileSummaryBg, TileSummaryFg, "Coordinator Summary",
        "I turn all the field reports into one clear briefing with counts, top cases and shortages."),
    Feature(FeatherIcons.Radio, TileMeshBg, TileMeshFg, "Mesh SOS",
        "Send an SOS from phone to phone with no internet. Nearby phones pass it onward."),
    Feature(FeatherIcons.MessageCircle, TileChatBg, TileChatFg, "AI Assistant",
        "Ask me anything about flood safety or first aid, answered right on your phone."),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to use Disha") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
        ) {
            // Intro with mascot
            Image(painterResource(R.drawable.mascot_1), null,
                modifier = Modifier.size(150.dp).align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Fit)
            Text("Hi, I'm Disha! 🦉", color = TextPrimary, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(6.dp))
            Text("Your offline flood helper. Everything runs on this phone, with no internet.",
                color = TextSecondary, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(22.dp))
            SectionTitle("What each tool does")
            Spacer(Modifier.height(10.dp))
            FEATURES.forEach { f ->
                FeatureExplainer(f)
                Spacer(Modifier.height(10.dp))
            }

            // How to navigate
            Spacer(Modifier.height(12.dp))
            MascotNote(
                mascot = R.drawable.mascot_2,
                title = "How to get around",
                lines = listOf(
                    "1.  Tap any card on the Home screen to open a tool.",
                    "2.  Use the  ←  back arrow at the top to return Home.",
                    "3.  In an emergency, start with Rescue Triage or Mesh SOS.",
                ),
            )

            Spacer(Modifier.height(16.dp))
            MascotNote(
                mascot = R.drawable.mascot_3,
                title = "Good to know",
                lines = listOf(
                    "• Everything works fully offline, even in airplane mode.",
                    "• First Aid answers are grounded in WHO and IFRC guidance, and cited.",
                    "• Mesh SOS needs Bluetooth and Wi-Fi on, plus a second phone nearby.",
                    "• AI guidance supports, and does not replace, professional help.",
                ),
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun FeatureExplainer(f: Feature) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(f.bg),
                contentAlignment = Alignment.Center) {
                Icon(f.icon, null, tint = f.fg, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(f.title, color = TextPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
                Text(f.desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MascotNote(@DrawableRes mascot: Int, title: String, lines: List<String>) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AccentPurple.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Image(painterResource(mascot), null, modifier = Modifier.size(74.dp),
                contentScale = ContentScale.Fit)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.padding(top = 6.dp)) {
                Text(title, color = AccentPurple, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                lines.forEach {
                    Text(it, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
