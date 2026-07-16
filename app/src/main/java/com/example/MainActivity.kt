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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.settings.SettingsViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var companyViewModel: CompanyViewModel
    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var quotationViewModel: QuotationViewModel
    private lateinit var masterViewModel: MasterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize offline database & repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = QuotesRepository(database)
        val masterRepository = MasterRepository(database)

        // 2. Instantiate Split ViewModels
        companyViewModel = ViewModelProvider(this, CompanyViewModelFactory(application, repository))[CompanyViewModel::class.java]
        customerViewModel = ViewModelProvider(this, CustomerViewModelFactory(application, repository))[CustomerViewModel::class.java]
        historyViewModel = ViewModelProvider(this, HistoryViewModelFactory(application, repository))[HistoryViewModel::class.java]
        settingsViewModel = ViewModelProvider(this, SettingsViewModelFactory(application, repository))[SettingsViewModel::class.java]
        quotationViewModel = ViewModelProvider(this, QuotationViewModelFactory(application, repository))[QuotationViewModel::class.java]
        masterViewModel = ViewModelProvider(this, MasterViewModelFactory(application, masterRepository))[MasterViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainDashboard(
                    companyViewModel,
                    customerViewModel,
                    historyViewModel,
                    settingsViewModel,
                    quotationViewModel,
                    masterViewModel
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
    masterViewModel: MasterViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "history"

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
                    CustomersScreen(customerViewModel = customerViewModel)
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
