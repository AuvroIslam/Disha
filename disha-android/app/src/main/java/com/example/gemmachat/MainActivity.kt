package com.example.gemmachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gemmachat.core.Triage
import com.example.gemmachat.data.RegionAssets
import com.example.gemmachat.data.SosEntry
import com.example.gemmachat.data.download.HfDownloadRepository
import com.example.gemmachat.ui.i18n.LocalBangla
import com.example.gemmachat.ui.chat.ChatScreen
import com.example.gemmachat.ui.chat.ChatViewModel
import com.example.gemmachat.ui.community.CommunityScreen
import com.example.gemmachat.ui.community.CommunityViewModel
import com.example.gemmachat.ui.emergency.EmergencyScreen
import com.example.gemmachat.ui.family.FamilyScreen
import com.example.gemmachat.ui.family.FamilyViewModel
import com.example.gemmachat.ui.firstaid.FirstAidScreen
import com.example.gemmachat.ui.firstaid.FirstAidViewModel
import com.example.gemmachat.ui.gis.GisScreen
import com.example.gemmachat.ui.gis.GisViewModel
import com.example.gemmachat.ui.guide.GuideScreen
import com.example.gemmachat.ui.home.DishaHomeScreen
import com.example.gemmachat.ui.mesh.MeshScreen
import com.example.gemmachat.ui.mesh.MeshViewModel
import com.example.gemmachat.ui.onboarding.OnboardingScreen
import com.example.gemmachat.ui.onboarding.OnboardingViewModel
import com.example.gemmachat.ui.settings.SettingsScreen
import com.example.gemmachat.ui.settings.SettingsViewModel
import com.example.gemmachat.ui.splash.BrandSplashScreen
import com.example.gemmachat.ui.summary.SummaryScreen
import com.example.gemmachat.ui.summary.SummaryViewModel
import com.example.gemmachat.ui.theme.GemmaChatTheme
import com.example.gemmachat.ui.triage.TriageScreen
import com.example.gemmachat.ui.triage.TriageViewModel

private object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TRIAGE = "triage"
    const val FIRSTAID = "firstaid"
    const val GIS = "gis"
    const val SUMMARY = "summary"
    const val MESH = "mesh"
    const val GUIDE = "guide"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val EMERGENCY = "emergency"
    const val COMMUNITY = "community"
    const val FAMILY = "family"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GemmaChatTheme {
                DishaNavHost()
            }
        }
    }
}

@Composable
private fun DishaNavHost() {
    val context = LocalContext.current
    val app = context.applicationContext as GemmaChatApplication
    val language by app.prefs.language.collectAsState()
    app.engineHolder.respondInBangla = language == "bn"
    val start =
        if (HfDownloadRepository.modelFile(context).exists()) Routes.HOME else Routes.ONBOARDING
    val navController = rememberNavController()

    fun toStartFromSplash() {
        navController.navigate(start) {
            popUpTo(Routes.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun appFactory() =
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application,
        )

    CompositionLocalProvider(LocalBangla provides (language == "bn")) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            BrandSplashScreen()
            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1300)
                toStartFromSplash()
            }
        }
        composable(Routes.ONBOARDING) {
            val vm: OnboardingViewModel = viewModel(factory = appFactory())
            val toHome: () -> Unit = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                    launchSingleTop = true
                }
            }
            OnboardingScreen(viewModel = vm, onFinished = toHome, onSkip = toHome)
        }
        composable(Routes.HOME) {
            val coachSeen by app.prefs.coachSeen.collectAsState()
            DishaHomeScreen(
                modelReady = HfDownloadRepository.modelFile(context).exists(),
                showCoach = !coachSeen,
                onCoachDismiss = { app.prefs.markCoachSeen() },
                onTriage = { navController.navigate(Routes.TRIAGE) },
                onFirstAid = { navController.navigate(Routes.FIRSTAID) },
                onGis = { navController.navigate(Routes.GIS) },
                onSummary = { navController.navigate(Routes.SUMMARY) },
                onMesh = { navController.navigate(Routes.MESH) },
                onChat = { navController.navigate(Routes.CHAT) },
                onGuide = { navController.navigate(Routes.GUIDE) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onEmergency = { navController.navigate(Routes.EMERGENCY) },
                onCommunity = { navController.navigate(Routes.COMMUNITY) },
                onFamily = { navController.navigate(Routes.FAMILY) },
                onSeedDemo = {
                    if (app.sosRepository.entries.value.none { it.source == "drill" }) {
                        RegionAssets.loadScenarios(context).forEach {
                            app.sosRepository.add(SosEntry(it, Triage.fallbackTriage(it), source = "drill"))
                        }
                    }
                },
            )
        }
        composable(Routes.TRIAGE) {
            val vm: TriageViewModel = viewModel(factory = appFactory())
            TriageScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.FIRSTAID) {
            val vm: FirstAidViewModel = viewModel(factory = appFactory())
            FirstAidScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.GIS) {
            val vm: GisViewModel = viewModel(factory = appFactory())
            GisScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.SUMMARY) {
            val vm: SummaryViewModel = viewModel(factory = appFactory())
            SummaryScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.MESH) {
            val vm: MeshViewModel = viewModel(factory = appFactory())
            MeshScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.GUIDE) {
            GuideScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.EMERGENCY) {
            EmergencyScreen(
                onBack = { navController.popBackStack() },
                onMesh = { navController.navigate(Routes.MESH) },
            )
        }
        composable(Routes.COMMUNITY) {
            val vm: CommunityViewModel = viewModel(factory = appFactory())
            CommunityScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.FAMILY) {
            val vm: FamilyViewModel = viewModel(factory = appFactory())
            FamilyScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.CHAT) {
            val vm: ChatViewModel = viewModel(factory = appFactory())
            ChatScreen(
                viewModel = vm,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onNeedModel = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.CHAT) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = appFactory())
            SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
    }
}
