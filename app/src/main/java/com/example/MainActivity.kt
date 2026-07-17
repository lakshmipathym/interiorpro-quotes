package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
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
import com.example.data.ClientRepository
import com.example.ui.client.ClientViewModel
import com.example.ui.client.ClientViewModelFactory
import com.example.ui.client.ClientsScreen
import com.example.ui.client.AddEditClientScreen
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
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.settings.SettingsViewModelFactory
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.material.icons.filled.CloudDownload
import com.example.core.backup.BackupMetadata

class MainActivity : ComponentActivity() {

    private lateinit var companyViewModel: CompanyViewModel
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var quotationViewModel: QuotationViewModel
    private lateinit var masterViewModel: MasterViewModel
    private lateinit var clientViewModel: ClientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize offline database & repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = QuotesRepository(database)
        val masterRepository = MasterRepository(database)
        val clientRepository = ClientRepository(database)

        // Initialize Core Architectural Dependencies
        val signInManager = com.example.core.drive.GoogleSignInManagerImpl(applicationContext)
        val driveService = com.example.core.drive.GoogleDriveServiceImpl(applicationContext, signInManager)
        val encryptionManager = com.example.core.security.EncryptionManagerImpl()
        val checksumManager = com.example.core.security.ChecksumManagerImpl()
        val integrityValidator = com.example.core.security.IntegrityValidatorImpl(checksumManager)
        val restoreManager = com.example.core.backup.RestoreManagerImpl(applicationContext, database, repository, encryptionManager, checksumManager, integrityValidator)
        val deviceManager = com.example.core.device.DeviceManagerImpl(applicationContext)
        val backupManager = com.example.core.backup.BackupManagerImpl(database, repository, encryptionManager, checksumManager, deviceManager)
        val syncCoordinator = com.example.core.sync.SyncCoordinatorImpl()
        val syncManager = com.example.core.sync.SyncManagerImpl(applicationContext, driveService, backupManager, restoreManager, deviceManager, syncCoordinator)
        val workspaceManager = com.example.core.backup.WorkspaceManagerImpl(applicationContext, database, repository, encryptionManager, checksumManager)

        // 2. Instantiate Split ViewModels
        companyViewModel = ViewModelProvider(this, CompanyViewModelFactory(application, repository))[CompanyViewModel::class.java]
        customerViewModel = ViewModelProvider(this, CustomerViewModelFactory(application, repository))[CustomerViewModel::class.java]
        historyViewModel = ViewModelProvider(this, HistoryViewModelFactory(application, repository))[HistoryViewModel::class.java]
        settingsViewModel = ViewModelProvider(this, SettingsViewModelFactory(application, repository, syncManager, workspaceManager, deviceManager, signInManager))[SettingsViewModel::class.java]
        quotationViewModel = ViewModelProvider(this, QuotationViewModelFactory(application, repository, syncManager))[QuotationViewModel::class.java]
        masterViewModel = ViewModelProvider(this, MasterViewModelFactory(application, masterRepository))[MasterViewModel::class.java]
        clientViewModel = ViewModelProvider(this, ClientViewModelFactory(application, clientRepository))[ClientViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainDashboard(
                    companyViewModel,
                    customerViewModel,
                    historyViewModel,
                    settingsViewModel,
                    quotationViewModel,
                    masterViewModel,
                    clientViewModel
                )
            }
        }
    }
}

@Composable
fun MainDashboard(
    companyViewModel: CompanyViewModel,
    customerViewModel: CustomerViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    quotationViewModel: QuotationViewModel,
    masterViewModel: MasterViewModel,
    clientViewModel: ClientViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "history"
    val context = LocalContext.current

    var newerBackupMetadata by remember { mutableStateOf<BackupMetadata?>(null) }
    var isRestoringCloudBackup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.checkForNewerBackup { metadata ->
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
                        settingsViewModel.resolveConflicts(preferCloud = true) { result ->
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentRoute == "history",
                    onClick = {
                        if (currentRoute != "history") {
                            navController.navigate("history") {
                                popUpTo("history") { inclusive = true }
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Receipt, contentDescription = "History") },
                    label = { Text("History", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentRoute == "new_quote",
                    onClick = {
                        quotationViewModel.startNewQuotation()
                        if (currentRoute != "new_quote") {
                            navController.navigate("new_quote") {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.AddCircle, contentDescription = "Create") },
                    label = { Text("New Quote", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentRoute == "clients",
                    onClick = {
                        if (currentRoute != "clients") {
                            navController.navigate("clients") {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.People, contentDescription = "Clients") },
                    label = { Text("Clients", fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = {
                        if (currentRoute != "settings") {
                            navController.navigate("settings") {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController = navController, startDestination = "history") {
                composable("history") {
                    QuotationHistoryScreen(
                        historyViewModel = historyViewModel,
                        quotationViewModel = quotationViewModel,
                        companyViewModel = companyViewModel,
                        customerViewModel = customerViewModel,
                        onNavigateToCreate = {
                            quotationViewModel.startNewQuotation()
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
                        quotationViewModel = quotationViewModel,
                        customerViewModel = customerViewModel,
                        onSuccessReturn = {
                            navController.navigate("history") {
                                popUpTo("history") { inclusive = true }
                            }
                        }
                    )
                }
                composable("clients") {
                    ClientsScreen(
                        clientViewModel = clientViewModel,
                        onNavigateToAddClient = {
                            navController.navigate("add_edit_client?clientId=0") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToEditClient = { clientId ->
                            navController.navigate("add_edit_client?clientId=$clientId") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = "add_edit_client?clientId={clientId}",
                    arguments = listOf(
                        navArgument("clientId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) { backStackEntry ->
                    val clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L
                    AddEditClientScreen(
                        clientId = clientId,
                        clientViewModel = clientViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("masters") {
                    MasterDataScreen(masterViewModel = masterViewModel)
                }
                composable("settings") {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        companyViewModel = companyViewModel,
                        onNavigateToMasters = {
                            navController.navigate("masters") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}
