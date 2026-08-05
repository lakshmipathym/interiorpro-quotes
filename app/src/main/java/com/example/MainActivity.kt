package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.QuotesRepository
import com.example.data.MasterRepository
import com.example.ui.company.CompanyViewModel
import com.example.ui.company.CompanyViewModelFactory
import com.example.ui.company.MasterViewModel
import com.example.ui.company.MasterViewModelFactory
import com.example.ui.company.MasterDataScreen
import com.example.ui.customer.CustomerViewModel
import com.example.ui.customer.CustomerViewModelFactory
import com.example.ui.customer.CustomersScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.history.HistoryViewModelFactory
import com.example.ui.history.QuotationHistoryScreen
import com.example.ui.quotation.NewQuotationScreen
import com.example.ui.quotation.QuotationViewModel
import com.example.ui.quotation.QuotationViewModelFactory

import com.example.domain.usecases.CalculateQuotationUseCase
import com.example.domain.usecases.FinalizeQuotationUseCase
import com.example.domain.engine.ItemCalculationEngineImpl
import com.example.domain.engine.QuotationCalculationEngineImpl
import com.example.domain.engine.DimensionParserImpl
import com.example.domain.engine.AmountInWordsConverterImpl
import com.example.data.snapshot.QuotationSnapshotRepositoryImpl
import com.example.domain.engine.QuotationSnapshotFactoryImpl
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.settings.SettingsViewModelFactory
import com.example.core.sync.dashboard.DashboardRepositoryImpl
import com.example.core.sync.dashboard.DashboardStateManager
import com.example.core.sync.dashboard.SyncDashboardViewModel
import com.example.core.sync.dashboard.ui.SyncDashboardScreen
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.core.backup.BackupMetadata

class MainActivity : ComponentActivity() {

    // Databases & Repositories (Lazy initialized to prevent cold start bottlenecks)
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { QuotesRepository(database) }
    private val masterRepository by lazy { MasterRepository(database) }

    // Core Sync & Security Infrastructure (Lazy initialized on-demand)
    private val signInManager by lazy { com.example.core.drive.GoogleSignInManagerImpl(applicationContext) }
    private val driveService by lazy { com.example.core.drive.GoogleDriveServiceImpl(applicationContext, signInManager) }
    private val encryptionManager by lazy { com.example.core.security.EncryptionManagerImpl() }
    private val checksumManager by lazy { com.example.core.security.ChecksumManagerImpl() }
    private val integrityValidator by lazy { com.example.core.security.IntegrityValidatorImpl(checksumManager) }
    private val restoreManager by lazy { com.example.core.backup.RestoreManagerImpl(applicationContext, database, repository, encryptionManager, checksumManager, integrityValidator) }
    private val deviceManager by lazy { com.example.core.device.DeviceManagerImpl(applicationContext) }
    private val backupManager by lazy { com.example.core.backup.BackupManagerImpl(database, repository, encryptionManager, checksumManager, deviceManager) }
    private val syncCoordinator by lazy { com.example.core.sync.SyncCoordinatorImpl() }
    private val syncManager by lazy { com.example.core.sync.SyncManagerImpl(applicationContext, driveService, backupManager, restoreManager, deviceManager, syncCoordinator) }
    private val workspaceManager by lazy { com.example.core.backup.WorkspaceManagerImpl(applicationContext, database, repository, encryptionManager, checksumManager) }
    private val themeManager by lazy { com.example.ui.theme.ThemeManager(applicationContext) }
    private val appStartupManager by lazy { com.example.core.startup.AppStartupManager(applicationContext) }

    // ViewModels (Lazy initialized when required by the Compose graph/screen)
    private val companyViewModel: CompanyViewModel by lazy {
        ViewModelProvider(this, CompanyViewModelFactory(application, repository, masterRepository))[CompanyViewModel::class.java]
    }
    private val customerViewModel: CustomerViewModel by lazy {
        ViewModelProvider(this, CustomerViewModelFactory(application, repository))[CustomerViewModel::class.java]
    }
    private val historyViewModel: HistoryViewModel by lazy {
        val itemEngine = ItemCalculationEngineImpl(DimensionParserImpl())
        val calcEngine = QuotationCalculationEngineImpl(AmountInWordsConverterImpl())
        val calcUseCase = com.example.domain.usecases.CalculateQuotationUseCase(itemEngine, calcEngine)
        val snapFactory = QuotationSnapshotFactoryImpl()
        val snapRepo = com.example.data.snapshot.QuotationSnapshotRepositoryImpl(com.example.data.AppDatabase.getDatabase(applicationContext), repository)
        val assetCopier = com.example.data.BrandingAssetCopierImpl(applicationContext)
        val finalizeUseCase = com.example.domain.usecases.FinalizeQuotationUseCase(snapFactory, snapRepo, assetCopier)
        ViewModelProvider(this, HistoryViewModelFactory(application, repository, calcUseCase, finalizeUseCase))[HistoryViewModel::class.java]
    }
    private val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider(this, SettingsViewModelFactory(application, repository, syncManager, workspaceManager, deviceManager, signInManager))[SettingsViewModel::class.java]
    }
    private val quotationViewModel: QuotationViewModel by lazy {
        val itemEngine = ItemCalculationEngineImpl(DimensionParserImpl())
        val calcEngine = QuotationCalculationEngineImpl(AmountInWordsConverterImpl())
        val calcUseCase = CalculateQuotationUseCase(itemEngine, calcEngine)
        val snapFactory = QuotationSnapshotFactoryImpl()
        val snapRepo = QuotationSnapshotRepositoryImpl(com.example.data.AppDatabase.getDatabase(applicationContext), repository)
        val assetCopier = com.example.data.BrandingAssetCopierImpl(applicationContext)
        val finalizeUseCase = FinalizeQuotationUseCase(snapFactory, snapRepo, assetCopier)
        ViewModelProvider(this, QuotationViewModelFactory(application, repository, masterRepository, syncManager, calcUseCase, finalizeUseCase, snapRepo))[QuotationViewModel::class.java]
    }
    private val masterViewModel: MasterViewModel by lazy {
        ViewModelProvider(this, MasterViewModelFactory(application, masterRepository))[MasterViewModel::class.java]
    }

    private val syncDashboardViewModel: SyncDashboardViewModel by lazy {
        val repository = DashboardRepositoryImpl(com.example.data.AppDatabase.getDatabase(applicationContext))
        val stateManager = DashboardStateManager(repository)
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SyncDashboardViewModel(stateManager) as T
            }
        }
        ViewModelProvider(this, factory)[SyncDashboardViewModel::class.java]
    }

    private fun cleanOrphanedImages() {
        try {
            val filesDir = this.filesDir
            val tempFiles = filesDir.listFiles { _, name ->
                name.startsWith("temp_des_") || name.startsWith("temp_lam_")
            }
            tempFiles?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Clean orphaned temporary images from aborted quotation edits
        cleanOrphanedImages()

        setContent {
            val themeMode by themeManager.themeMode.collectAsState()
            val systemIsDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                com.example.ui.theme.ThemeMode.DARK -> true
                com.example.ui.theme.ThemeMode.LIGHT -> false
                com.example.ui.theme.ThemeMode.SYSTEM -> systemIsDark
            }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainDashboard(
                    companyViewModelProvider = { companyViewModel },
                    customerViewModelProvider = { customerViewModel },
                    historyViewModelProvider = { historyViewModel },
                    settingsViewModelProvider = { settingsViewModel },
                    quotationViewModelProvider = { quotationViewModel },
                    masterViewModelProvider = { masterViewModel },
                    themeManagerProvider = { themeManager },
                    syncDashboardViewModelProvider = { syncDashboardViewModel },
                    appStartupManagerProvider = { appStartupManager },
                    signInManagerProvider = { signInManager }
                )
            }
        }
    }
}

enum class StartupStage {
    SPLASH,
    WELCOME,
    GOOGLE_DRIVE,
    TRIAL_INFO,
    MAIN_DASHBOARD,
    LICENSE_BLOCKED
}

@Composable
fun MainDashboard(
    companyViewModelProvider: () -> CompanyViewModel,
    customerViewModelProvider: () -> CustomerViewModel,
    historyViewModelProvider: () -> HistoryViewModel,
    settingsViewModelProvider: () -> SettingsViewModel,
    quotationViewModelProvider: () -> QuotationViewModel,
    masterViewModelProvider: () -> MasterViewModel,
    themeManagerProvider: () -> com.example.ui.theme.ThemeManager,
    syncDashboardViewModelProvider: () -> com.example.core.sync.dashboard.SyncDashboardViewModel,
    appStartupManagerProvider: () -> com.example.core.startup.AppStartupManager,
    signInManagerProvider: () -> com.example.core.drive.GoogleSignInManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "history"
    val context = LocalContext.current

    var startupStage by remember { mutableStateOf(StartupStage.SPLASH) }

    var newerBackupMetadata by remember { mutableStateOf<BackupMetadata?>(null) }
    var isRestoringCloudBackup by remember { mutableStateOf(false) }

    when (startupStage) {
        StartupStage.SPLASH -> {
            com.example.ui.startup.SplashScreen(
                appStartupManager = appStartupManagerProvider(),
                onNavigateToOnboarding = {
                    startupStage = StartupStage.WELCOME
                },
                onNavigateToDashboard = {
                    startupStage = StartupStage.MAIN_DASHBOARD
                },
                onNavigateToPlaceholder = {
                    startupStage = StartupStage.LICENSE_BLOCKED
                }
            )
        }
        StartupStage.WELCOME -> {
            com.example.ui.startup.WelcomeScreen(
                onGetStarted = {
                    startupStage = StartupStage.GOOGLE_DRIVE
                }
            )
        }
        StartupStage.GOOGLE_DRIVE -> {
            com.example.ui.startup.GoogleDriveOnboardingScreen(
                signInManager = signInManagerProvider(),
                onContinue = {
                    startupStage = StartupStage.TRIAL_INFO
                }
            )
        }
        StartupStage.TRIAL_INFO -> {
            com.example.ui.startup.TrialInfoOnboardingScreen(
                context = context,
                onCompleteOnboarding = {
                    val prefs = context.getSharedPreferences("app_onboarding_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_completed_onboarding", true).apply()

                    val licenseManager = com.example.core.license.LicenseManager(context)
                    if (licenseManager.getLicenseState() == com.example.core.license.LicenseState.EXPIRED_TRIAL) {
                        startupStage = StartupStage.LICENSE_BLOCKED
                    } else {
                        startupStage = StartupStage.MAIN_DASHBOARD
                    }
                }
            )
        }
        StartupStage.LICENSE_BLOCKED -> {
            com.example.ui.startup.LicensePlaceholderScreen()
        }
        StartupStage.MAIN_DASHBOARD -> {
            LaunchedEffect(Unit) {
                // Defer cloud backup checks slightly to allow completely smooth startup transition
                kotlinx.coroutines.delay(2000)
                settingsViewModelProvider().checkForNewerBackup { metadata ->
                    if (metadata != null) {
                        newerBackupMetadata = metadata
                    }
                }
            }


    if (newerBackupMetadata != null) {
        val metadata = newerBackupMetadata!!
        val formattedDate = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(metadata.timestamp))
        
        AlertDialog(
            onDismissRequest = { newerBackupMetadata = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Backup Found", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("A newer cloud backup is available in your secure Google Drive folder.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Backup Date: $formattedDate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Device: ${metadata.deviceName}", fontSize = 12.sp)
                    Text("App Version: v${metadata.appVersion}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Would you like to restore this backup now? This will replace your current local data.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRestoringCloudBackup = true
                        settingsViewModelProvider().resolveConflicts(preferCloud = true) { result ->
                            isRestoringCloudBackup = false
                            newerBackupMetadata = null
                            if (result is com.example.core.sync.SyncResult.Success) {
                                android.widget.Toast.makeText(context, "Cloud backup restored successfully!", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "Failed to restore cloud backup.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isRestoringCloudBackup
                ) {
                    if (isRestoringCloudBackup) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Restore Now")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { newerBackupMetadata = null },
                    enabled = !isRestoringCloudBackup
                ) {
                    Text("Later")
                }
            }
        )
    }

    val showBottomBar = currentRoute in listOf("history", "new_quote", "customers", "settings", "masters", "sync_dashboard", "about")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp
                ) {
                    val isHistory = currentRoute == "history"
                    NavigationBarItem(
                        selected = isHistory,
                        onClick = {
                            if (!isHistory) {
                                navController.navigate("history") {
                                    popUpTo("history") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(if(isHistory) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Filled.List, contentDescription = "History") },
                        label = { Text("History", fontWeight = if(isHistory) FontWeight.Bold else FontWeight.Normal) },
                        alwaysShowLabel = true
                    )
                    
                    val isNewQuote = currentRoute == "new_quote"
                    NavigationBarItem(
                        selected = isNewQuote,
                        onClick = {
                            quotationViewModelProvider().startNewQuotation()
                            if (!isNewQuote) {
                                navController.navigate("new_quote") {
                                    popUpTo("history") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(if(isNewQuote) Icons.Filled.AddCircle else Icons.Filled.Add, contentDescription = "Create") },
                        label = { Text("New Quote", fontWeight = if(isNewQuote) FontWeight.Bold else FontWeight.Normal) },
                        alwaysShowLabel = true
                    )
                    
                    val isCustomers = currentRoute == "customers"
                    NavigationBarItem(
                        selected = isCustomers,
                        onClick = {
                            if (!isCustomers) {
                                navController.navigate("customers") {
                                    popUpTo("history") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(if(isCustomers) Icons.Filled.Person else Icons.Filled.Person, contentDescription = "Clients") },
                        label = { Text("Customers", fontWeight = if(isCustomers) FontWeight.Bold else FontWeight.Normal) },
                        alwaysShowLabel = true
                    )
                    
                    val isSettings = currentRoute == "settings"
                    NavigationBarItem(
                        selected = isSettings,
                        onClick = {
                            if (!isSettings) {
                                navController.navigate("settings") {
                                    popUpTo("history") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontWeight = if(isSettings) FontWeight.Bold else FontWeight.Normal) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = "history",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { 300 },
                        animationSpec = tween(250)
                    ) + fadeIn(animationSpec = tween(250))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -300 },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -300 },
                        animationSpec = tween(250)
                    ) + fadeIn(animationSpec = tween(250))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { 300 },
                        animationSpec = tween(250)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) {
                composable("license_placeholder") {
                    com.example.ui.startup.LicensePlaceholderScreen()
                }
                composable("history") {
                    QuotationHistoryScreen(
                        historyViewModel = historyViewModelProvider(),
                        quotationViewModel = quotationViewModelProvider(),
                        companyViewModel = companyViewModelProvider(),
                        customerViewModel = customerViewModelProvider(),
                        onNavigateToCreate = {
                            quotationViewModelProvider().startNewQuotation()
                            navController.navigate("new_quote") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToEdit = {
                            navController.navigate("new_quote") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("new_quote") {
                    NewQuotationScreen(
                        quotationViewModel = quotationViewModelProvider(),
                        customerViewModel = customerViewModelProvider(),
                        onSuccessReturn = {
                            navController.navigate("history") {
                                popUpTo("history") { inclusive = true }
                            }
                        }
                    )
                }
                composable("customers") {
                    CustomersScreen(
                        customerViewModel = customerViewModelProvider()
                    )
                }
                composable("masters") {
                    MasterDataScreen(masterViewModel = masterViewModelProvider())
                }
                composable("settings") {
                    SettingsScreen(
                        settingsViewModel = settingsViewModelProvider(),
                        companyViewModel = companyViewModelProvider(),
                        masterViewModel = masterViewModelProvider(),
                        themeManager = themeManagerProvider(),
                        onNavigateToMasters = {
                            navController.navigate("masters") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToAbout = {
                            navController.navigate("about") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToSyncDashboard = {
                            navController.navigate("sync_dashboard") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("sync_dashboard") {
                    SyncDashboardScreen(
                        viewModel = syncDashboardViewModelProvider(), 
                        onNavigateBack = { navController.popBackStack() },
                        onNewQuotation = { navController.navigate("new_quote") },
                        onAddCustomer = { navController.navigate("customers") },
                        onAddMaterial = { navController.navigate("masters") },
                        onCompanyProfile = { navController.navigate("settings") },
                        onBackup = { navController.navigate("settings") }
                    )
                }
                composable("about") {
                    com.example.ui.settings.AboutScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
}
}


