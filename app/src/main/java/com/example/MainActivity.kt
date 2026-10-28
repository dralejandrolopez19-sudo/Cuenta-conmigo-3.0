package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.example.data.repository.CuentaConmigoRepository
import com.example.ui.components.CuentaConmigoBottomBar
import com.example.ui.components.CuentaConmigoTopBar
import com.example.ui.components.PrivacyShieldBanner
import com.example.ui.components.ProactiveSupportCard
import com.example.ui.components.StreetPrivacySettingsDialog
import com.example.ui.components.attemptAutoSignIn
import com.example.ui.components.findActivity
import com.example.ui.components.signOutUser
import com.example.ui.screens.AgendaScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CrisisSupportDialog
import com.example.ui.screens.DiaryScreen
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MedicationScreen
import com.example.ui.screens.MoodScreen
import com.example.ui.screens.PersistentMedicationAlarmDialog
import com.example.ui.screens.ProfileDialog
import com.example.ui.theme.CuentaConmigoTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.AppViewModel
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicializar App Check antes de setContent según lineamientos de Firebase AI Logic
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            CuentaConmigoTheme {
                MainAppRoot()
            }
        }
    }
}

@Composable
fun MainAppRoot() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isGuestMode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null && !isGuestMode) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = coroutineScope
            )
        }
    }

    val user = currentUser
    if (user != null) {
        AppMainContent(
            userId = user.uid,
            userEmail = user.email ?: "",
            isGuest = false,
            initialStreetMode = false,
            onSignOut = {
                signOutUser(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {
                        currentUser = null
                        isGuestMode = false
                    },
                    scope = coroutineScope
                )
            }
        )
    } else if (isGuestMode) {
        AppMainContent(
            userId = "guest_explorer",
            userEmail = "invitado@cuentaconmigo.app",
            isGuest = true,
            initialStreetMode = false,
            onSignOut = {
                isGuestMode = false
                com.example.data.repository.InMemoryGuestStore.reset()
            }
        )
    } else {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    }
}

@Composable
fun AppMainContent(
    userId: String,
    userEmail: String,
    isGuest: Boolean = false,
    initialStreetMode: Boolean = false,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(userId) { CuentaConmigoRepository(context, userId) }
    val viewModel = remember(userId) {
        AppViewModel(
            application = (context.applicationContext as android.app.Application),
            repository = repository,
            currentUserId = userId,
            initialStreetMode = initialStreetMode
        )
    }

    val currentTab by viewModel.currentTab.collectAsState()
    val profile by viewModel.profile.collectAsState()

    val isStreetModeActive by viewModel.isStreetModeActive.collectAsState()
    val streetModeTriggerReason by viewModel.streetModeTriggerReason.collectAsState()
    val autoMotionPrivacyEnabled by viewModel.autoMotionPrivacyEnabled.collectAsState()
    val isAiDisguiseEnabled by viewModel.isAiDisguiseEnabled.collectAsState()
    val isGeneratingAiCamouflage by viewModel.isGeneratingAiCamouflage.collectAsState()
    val aiPrivacyStatusMessage by viewModel.aiPrivacyStatusMessage.collectAsState()
    val showStreetPrivacyDialog by viewModel.showStreetPrivacyDialog.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Activar o desactivar FLAG_SECURE dinámicamente cuando Modo Calle está activo
    LaunchedEffect(isStreetModeActive) {
        val window = context.findActivity()?.window
        if (window != null) {
            if (isStreetModeActive) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Permiso procesado */ }
    )

    // Solicitud de permiso de notificación en Android 13+
    LaunchedEffect(userId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val showCrisisSupport by viewModel.showCrisisSupportDialog.collectAsState()
    val proactiveSupport by viewModel.activeProactiveSupport.collectAsState()

    // Manejo de botón atrás para volver a Inicio si está en otra pestaña
    BackHandler(enabled = currentTab != AppTab.INICIO) {
        viewModel.selectTab(AppTab.INICIO)
    }

    Scaffold(
        topBar = {
            Column {
                CuentaConmigoTopBar(
                    onEditProfileClicked = { showEditProfileDialog = true },
                    onSignOutClicked = onSignOut,
                    onSosClicked = { viewModel.openCrisisSupport() },
                    showSosButton = (currentTab == AppTab.INICIO),
                    isGuest = isGuest,
                    isStreetModeActive = isStreetModeActive,
                    onToggleStreetModeQuick = { viewModel.toggleStreetModeQuick() },
                    onOpenStreetPrivacySettings = { viewModel.openStreetPrivacyDialog() }
                )
                PrivacyShieldBanner(
                    isStreetModeActive = isStreetModeActive,
                    triggerReason = streetModeTriggerReason,
                    isAiDisguiseEnabled = isAiDisguiseEnabled,
                    isGeneratingAi = isGeneratingAiCamouflage,
                    onOpenSettings = { viewModel.openStreetPrivacyDialog() },
                    onDisableStreetMode = { viewModel.setStreetMode(false) }
                )
                ProactiveSupportCard(
                    visible = proactiveSupport?.hasImbalance == true,
                    reasonMessage = proactiveSupport?.triggerReason?.ifBlank {
                        "Notamos que estás atravesando un momento difícil. No tienes que pasarlo a solas."
                    } ?: "Notamos que estás atravesando un momento difícil. No tienes que pasarlo a solas.",
                    onDismiss = { viewModel.dismissProactiveSupport() },
                    onOpenHelplines = {
                        viewModel.dismissProactiveSupport()
                        viewModel.openCrisisSupport()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        },
        bottomBar = {
            CuentaConmigoBottomBar(
                selectedTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.INICIO -> HomeScreen(viewModel = viewModel)
                AppTab.DIARIO -> DiaryScreen(viewModel = viewModel)
                AppTab.ANIMO -> MoodScreen(viewModel = viewModel)
                AppTab.PASTILLAS -> MedicationScreen(viewModel = viewModel)
                AppTab.HABITOS -> HabitsScreen(viewModel = viewModel)
                AppTab.AGENDA -> AgendaScreen(viewModel = viewModel)
            }
        }
    }

    val activeAlarmDoses by viewModel.activeAlarmDoses.collectAsState()

    if (showStreetPrivacyDialog) {
        StreetPrivacySettingsDialog(
            isStreetModeActive = isStreetModeActive,
            autoMotionEnabled = autoMotionPrivacyEnabled,
            isAiDisguiseEnabled = isAiDisguiseEnabled,
            isSensorAvailable = viewModel.isMotionSensorAvailable,
            isGeneratingAi = isGeneratingAiCamouflage,
            aiStatusMessage = aiPrivacyStatusMessage,
            onToggleStreetMode = { enabled ->
                viewModel.setStreetMode(
                    enabled = enabled,
                    reason = if (enabled) "Modo Calle activado desde el panel de privacidad" else ""
                )
            },
            onToggleAutoMotion = { viewModel.setAutoMotionPrivacy(it) },
            onToggleAiDisguise = { viewModel.setAiDisguiseEnabled(it) },
            onGenerateAiCamouflageNow = { viewModel.generateAiCamouflageAliases() },
            onSimulateStreetMotion = { viewModel.simulateStreetMotionTrigger() },
            onDismiss = { viewModel.closeStreetPrivacyDialog() }
        )
    }

    if (showEditProfileDialog) {
        ProfileDialog(
            currentName = profile?.displayName?.trim()?.ifBlank { null }
                ?: Firebase.auth.currentUser?.displayName?.trim()?.ifBlank { null }
                ?: Firebase.auth.currentUser?.email?.substringBefore("@")?.replace(".", " ")?.replace("_", " ")?.split(" ")?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                ?: "Mi Nombre",
            onSaveName = { newName ->
                viewModel.updateProfileName(newName, userEmail)
                showEditProfileDialog = false
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    // DIÁLOGO DE CRISIS Y LÍNEAS DE AYUDA
    if (showCrisisSupport) {
        CrisisSupportDialog(
            onDismiss = { viewModel.closeCrisisSupport() }
        )
    }

    // ALARMA PERSISTENTE DE MEDICACIÓN
    if (activeAlarmDoses.isNotEmpty()) {
        PersistentMedicationAlarmDialog(
            pendingDoses = activeAlarmDoses,
            onConfirmDose = { dose ->
                viewModel.confirmDoseInAlarm(dose)
            },
            onConfirmAllDoses = {
                viewModel.confirmAllAlarmDoses()
            },
            onSnoozeAlarm = {
                viewModel.snoozeMedicationAlarm(5)
            }
        )
    }
}
