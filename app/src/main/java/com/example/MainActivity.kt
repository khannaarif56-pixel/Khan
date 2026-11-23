package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.attemptAutoSignIn
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.ui.components.JarvisHudBackground
import com.example.ui.components.JarvisTopHudBar
import com.example.ui.navigation.JarvisCenter
import com.example.ui.screens.AiBrainScreen
import com.example.ui.screens.AiDevCenterScreen
import com.example.ui.screens.AiToolsScreen
import com.example.ui.screens.AiVisionScreen
import com.example.ui.screens.EmergencyLockScreen
import com.example.ui.screens.GoogleIntegrationsScreen
import com.example.ui.screens.HighSecurityScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.JarvisAuthScreen
import com.example.ui.screens.MobileControlScreen
import com.example.ui.screens.ProductivityScreen
import com.example.ui.screens.UpdateCenterScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.screens.VoiceSelectionScreen
import com.example.ui.screens.WorldwideSearchScreen
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextSecondaryMuted
import com.example.viewmodel.JarvisViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE Compose UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            JarvisTheme {
                JarvisRootApp()
            }
        }
    }
}

@Composable
private fun JarvisRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var autoSignInChecked by remember { mutableStateOf(auth.currentUser != null) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            delay(250L)
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { autoSignInChecked = true },
                onUnauthenticated = { autoSignInChecked = true },
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        JarvisAuthScreen(
            onAuthSuccess = { autoSignInChecked = true }
        )
    } else {
        val uid = user.uid
        val email = user.email.orEmpty()
        key(uid) {
            JarvisAuthenticatedShell(
                currentUserId = uid,
                currentUserEmail = email
            )
        }
    }
}

@Composable
private fun JarvisAuthenticatedShell(
    currentUserId: String,
    currentUserEmail: String
) {
    val viewModel: JarvisViewModel = viewModel(
        key = currentUserId,
        factory = JarvisViewModel.provideFactory(currentUserId, currentUserEmail)
    )

    val settings by viewModel.profileSettings.collectAsStateWithLifecycle()
    val sessionUnlocked by viewModel.sessionUnlocked.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val firestoreError by viewModel.firestoreError.collectAsStateWithLifecycle()

    if (settings.emergencyLocked || !sessionUnlocked) {
        EmergencyLockScreen(
            securityVault = viewModel.securityVault,
            ownerEmail = currentUserEmail.ifBlank { currentUserId },
            onUnlocked = { viewModel.unlockSession() }
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentCenter = JarvisCenter.fromRoute(backStackEntry?.destination?.route)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    LaunchedEffect(firestoreError) {
        firestoreError?.let { err ->
            snackbarHostState.showSnackbar(err)
        }
    }

    fun navigateToCenter(center: JarvisCenter) {
        navController.navigate(center.route) {
            launchSingleTop = true
            if (center == JarvisCenter.HOME) {
                popUpTo(JarvisCenter.HOME.route) { inclusive = false }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = DeepNavy,
                        contentColor = SolarGold
                    ) {
                        JarvisCenter.primaryTabs.forEach { tab ->
                            val selected = currentCenter == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToCenter(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.shortLabel,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianBlack,
                                    selectedTextColor = SolarGold,
                                    indicatorColor = SolarGold,
                                    unselectedIconColor = TextSecondaryMuted,
                                    unselectedTextColor = TextSecondaryMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            JarvisHudBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = DeepNavy,
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                        ) {
                            JarvisCenter.primaryTabs.forEach { tab ->
                                val selected = currentCenter == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToCenter(tab) },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.shortLabel) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ObsidianBlack,
                                        selectedTextColor = SolarGold,
                                        indicatorColor = SolarGold,
                                        unselectedIconColor = TextSecondaryMuted,
                                        unselectedTextColor = TextSecondaryMuted
                                    )
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        JarvisTopHudBar(
                            title = currentCenter.title,
                            subtitle = currentCenter.subtitle,
                            ownerDisplayName = settings.ownerDisplayName,
                            primaryLanguage = settings.primaryLanguage,
                            onToggleLanguage = {
                                val next = if (settings.primaryLanguage == "hi") "en" else "hi"
                                viewModel.setLanguage(next)
                            },
                            onBackClick = if (currentCenter != JarvisCenter.HOME) {
                                { navController.popBackStack() }
                            } else {
                                null
                            }
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            NavHost(
                                navController = navController,
                                startDestination = JarvisCenter.HOME.route
                            ) {
                                composable(JarvisCenter.HOME.route) {
                                    HomeDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateCenter = ::navigateToCenter
                                    )
                                }
                                composable(JarvisCenter.AI_BRAIN.route) {
                                    AiBrainScreen(viewModel = viewModel)
                                }
                                composable(JarvisCenter.VOICE_ASSISTANT.route) {
                                    VoiceAssistantScreen(
                                        viewModel = viewModel,
                                        onOpenVoiceSelection = { navigateToCenter(JarvisCenter.VOICE_SELECTION) }
                                    )
                                }
                                composable(JarvisCenter.VOICE_SELECTION.route) {
                                    VoiceSelectionScreen(viewModel = viewModel)
                                }
                                composable(JarvisCenter.AI_VISION.route) {
                                    AiVisionScreen(viewModel = viewModel)
                                }
                                composable(JarvisCenter.MOBILE_CONTROL.route) {
                                    MobileControlScreen(viewModel = viewModel)
                                }
                                composable(JarvisCenter.AI_TOOLS.route) {
                                    AiToolsScreen(
                                        viewModel = viewModel,
                                        onOpenVisionCenter = { navigateToCenter(JarvisCenter.AI_VISION) }
                                    )
                                }
                                composable(JarvisCenter.PRODUCTIVITY.route) {
                                    ProductivityScreen(
                                        viewModel = viewModel,
                                        onOpenChatHistory = { navigateToCenter(JarvisCenter.AI_BRAIN) }
                                    )
                                }
                                composable(JarvisCenter.HIGH_SECURITY.route) {
                                    HighSecurityScreen(viewModel = viewModel)
                                }
                                composable(JarvisCenter.WORLDWIDE_SEARCH.route) {
                                    WorldwideSearchScreen(viewModel = viewModel)
                                }
                                composable(JarvisCenter.GOOGLE_INTEGRATIONS.route) {
                                    GoogleIntegrationsScreen(
                                        viewModel = viewModel,
                                        onOpenWorldwideSearch = { navigateToCenter(JarvisCenter.WORLDWIDE_SEARCH) }
                                    )
                                }
                                composable(JarvisCenter.AI_DEV.route) {
                                    AiDevCenterScreen(viewModel = viewModel)
                                }
                                composable(JarvisCenter.UPDATE_CENTER.route) {
                                    UpdateCenterScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
