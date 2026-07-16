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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import compose.icons.FeatherIcons
import compose.icons.feathericons.Activity
import compose.icons.feathericons.AlertTriangle
import compose.icons.feathericons.BarChart2
import compose.icons.feathericons.HelpCircle
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.MessageCircle
import compose.icons.feathericons.Play
import compose.icons.feathericons.Radio
import compose.icons.feathericons.Settings
import compose.icons.feathericons.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.gemmachat.ui.components.HeroBanner
import com.example.gemmachat.ui.demo.Actions
import com.example.gemmachat.ui.demo.DemoDialog
import com.example.gemmachat.ui.i18n.tr
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
    onSeedDemo: () -> Unit = {},
) {
    var showDemo by remember { mutableStateOf(false) }
    if (showDemo) {
        DemoDialog(
            onDismiss = { showDemo = false },
            onSeed = onSeedDemo,
            actions = Actions(
                triage = onTriage, firstAid = onFirstAid, shelter = onGis,
                mesh = onMesh, summary = onSummary,
            ),
        )
    }
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
            Image(painterResource(R.drawable.disha_app_icon), contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("দিশা", color = AccentPurple, fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp, lineHeight = 26.sp)
                Text("DISHA", color = AccentPurple, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                    modifier = Modifier.offset(y = (-5).dp))
            }
            Spacer(Modifier.weight(1f))
            CircleIconButton(FeatherIcons.HelpCircle, onClick = onGuide)
            Spacer(Modifier.width(10.dp))
            CircleIconButton(FeatherIcons.Settings, onClick = onSettings)
        }

        Spacer(Modifier.height(18.dp))
        Text(tr("Assalamu Alaikum", "আসসালামু আলাইকুম"), color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(2.dp))
        Text("কিভাবে সাহায্য করতে পারি?", color = TextPrimary,
            fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp)
        Spacer(Modifier.height(6.dp))
        Text(tr("Your offline AI assistant for flood emergencies",
            "বন্যা জরুরি অবস্থার জন্য আপনার অফলাইন এআই সহায়ক"),
            color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

        // ---- Flood drill launcher ----
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(AccentPurple.copy(alpha = 0.10f))
                .clickable { showDemo = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(AccentPurple),
                contentAlignment = Alignment.Center) {
                Icon(FeatherIcons.Play, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Try a flood drill", "একটি বন্যা মহড়া করুন"), color = TextPrimary,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(tr("A guided walkthrough of every tool", "প্রতিটি টুলের ধাপে ধাপে পরিচিতি"),
                    color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }

        // ---- Hero illustration ----
        Spacer(Modifier.height(16.dp))
        HeroBanner(
            R.drawable.hero_home,
            title = tr("We're here to help", "আমরা পাশে আছি"),
            subtitle = tr("Offline AI, right on your device", "অফলাইন এআই, আপনার ডিভাইসেই"),
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
                    Icon(FeatherIcons.Shield, null, tint = AccentPurple, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("You are in ", "আপনি এখন "), color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(tr("Offline Mode", "অফলাইন মোডে"), color = AccentPurple,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(" ●", color = Color(0xFF22A565),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(tr("All systems ready. AI is running on this device.",
                        "সব প্রস্তুত। এআই এই ডিভাইসেই চলছে।"),
                        color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ---- Feature grid (2 x 3) ----
        Spacer(Modifier.height(18.dp))
        FeatureRow(
            left = { FeatureCard(TileTriageBg, TileTriageFg, FeatherIcons.AlertTriangle,
                tr("Rescue Triage", "উদ্ধার ট্রায়াজ"),
                tr("Prioritize SOS and urgent cases", "জরুরি এসওএস অগ্রাধিকার দিন"), onTriage, it) },
            right = { FeatureCard(TileAidBg, TileAidFg, FeatherIcons.Activity,
                tr("First Aid", "প্রাথমিক চিকিৎসা"),
                tr("Simple, trusted medical guidance", "সহজ, নির্ভরযোগ্য চিকিৎসা পরামর্শ"), onFirstAid, it) },
        )
        Spacer(Modifier.height(14.dp))
        FeatureRow(
            left = { FeatureCard(TileShelterBg, TileShelterFg, FeatherIcons.MapPin,
                tr("Safe Shelter & Route", "নিরাপদ আশ্রয় ও পথ"),
                tr("Find safe places and best routes", "নিরাপদ জায়গা ও সেরা পথ খুঁজুন"), onGis, it) },
            right = { FeatureCard(TileSummaryBg, TileSummaryFg, FeatherIcons.BarChart2,
                tr("Coordinator Summary", "সমন্বয়কারী সারাংশ"),
                tr("Overview, reports and insights", "সারচিত্র, রিপোর্ট ও বিশ্লেষণ"), onSummary, it) },
        )
        Spacer(Modifier.height(14.dp))
        FeatureRow(
            left = { FeatureCard(TileMeshBg, TileMeshFg, FeatherIcons.Radio,
                tr("Mesh SOS", "মেশ এসওএস"),
                tr("Send & receive SOS offline via mesh", "মেশে অফলাইনে এসওএস পাঠান ও নিন"), onMesh, it) },
            right = { FeatureCard(TileChatBg, TileChatFg, FeatherIcons.MessageCircle,
                tr("AI Assistant", "এআই সহকারী"),
                tr("Ask anything about flood safety & first aid", "বন্যা ও প্রাথমিক চিকিৎসা নিয়ে জিজ্ঞেস করুন"), onChat, it) },
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
