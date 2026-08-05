package com.example.core.sync.dashboard.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.sync.dashboard.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDashboardScreen(
    viewModel: SyncDashboardViewModel,
    onNavigateBack: () -> Unit = {},
    onNewQuotation: () -> Unit = {},
    onAddCustomer: () -> Unit = {},
    onAddMaterial: () -> Unit = {},
    onCompanyProfile: () -> Unit = {},
    onBackup: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Enterprise Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Refresh */ }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = uiState) {
                is DashboardState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is DashboardState.Error -> {
                    EmptyState(
                        title = "Dashboard Error",
                        message = state.message,
                        icon = Icons.Filled.Warning
                    )
                }
                is DashboardState.Success -> {
                    val isEmpty = state.syncSummary.quotationCount == 0 &&
                            state.syncSummary.customerCount == 0 &&
                            state.syncSummary.masterRecordsCount == 0
                    if (isEmpty) {
                        DashboardEmptyState(onNewQuotation = onNewQuotation)
                    } else {
                        DashboardContent(
                            summary = state.syncSummary,
                            health = state.workspaceHealth,
                            onNewQuotation = onNewQuotation,
                            onAddCustomer = onAddCustomer,
                            onAddMaterial = onAddMaterial,
                            onCompanyProfile = onCompanyProfile,
                            onBackup = onBackup,
                            onHistory = onNavigateBack
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardEmptyState(onNewQuotation: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.XXL),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.SpaceDashboard,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(Spacing.XL))
        Text(
            text = "Welcome to InteriorPro ERP",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.M))
        Text(
            text = "Your workspace is ready. Start by creating your first quotation or adding customers.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.XXL))
        PremiumPrimaryButton(
            onClick = onNewQuotation,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(Spacing.S))
            Text("Create Your First Quotation", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardContent(
    summary: SyncSummary,
    health: WorkspaceHealth,
    onNewQuotation: () -> Unit,
    onAddCustomer: () -> Unit,
    onAddMaterial: () -> Unit,
    onCompanyProfile: () -> Unit,
    onBackup: () -> Unit,
    onHistory: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(400))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.L)
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(top = Spacing.L, bottom = Spacing.Section),
            verticalArrangement = Arrangement.spacedBy(Spacing.XL)
        ) {
            // 1. Greeting Header
            item {
                GreetingHeaderSection()
            }

            // 2. Today's Metrics (4 metric cards)
            item {
                TodayMetricsSection(summary)
            }

            // 3. Quick Actions (New Quote, Customers, Backup, History)
            item {
                QuickActionsSection(
                    onNewQuotation = onNewQuotation,
                    onAddCustomer = onAddCustomer,
                    onBackup = onBackup,
                    onHistory = onHistory
                )
            }

            // 4. Recent Quotations Section
            item {
                RecentQuotationsSection(summary, onHistory)
            }

            // 5. Subscription Status Card
            item {
                SubscriptionStatusSection()
            }

            // 6. Google Drive Status Card
            item {
                GoogleDriveStatusSection(summary, onBackup)
            }

            // 7. Last Backup Information
            item {
                LastBackupInformationSection(summary, onBackup)
            }

            // 8. App Version Footer
            item {
                AppVersionFooterSection()
            }
        }
    }
}

@Composable
fun GreetingHeaderSection() {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val dateFormatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val currentDate = dateFormatter.format(Date())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.Large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.L),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "InteriorPro ERP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(Spacing.XXS))
                Text(
                    text = "$greeting, Admin",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.XXS))
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Dashboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(IconSize.Medium)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayMetricsSection(summary: SyncSummary) {
    Column {
        SectionHeader(title = "Today's Metrics")
        Spacer(modifier = Modifier.height(Spacing.S))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.M),
            verticalArrangement = Arrangement.spacedBy(Spacing.M),
            maxItemsInEachRow = 2
        ) {
            MetricCard(
                title = "Quotations",
                value = summary.quotationCount.toString(),
                subtitle = "Active records",
                icon = Icons.AutoMirrored.Filled.List,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Customers",
                value = summary.customerCount.toString(),
                subtitle = "Client profiles",
                icon = Icons.Filled.People,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Materials",
                value = summary.masterRecordsCount.toString(),
                subtitle = "Master inventory",
                icon = Icons.Filled.Category,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Database Schema",
                value = "v${summary.databaseVersion}",
                subtitle = "Room Storage",
                icon = Icons.Filled.Storage,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionsSection(
    onNewQuotation: () -> Unit,
    onAddCustomer: () -> Unit,
    onBackup: () -> Unit,
    onHistory: () -> Unit
) {
    Column {
        SectionHeader(title = "Quick Actions")
        Spacer(modifier = Modifier.height(Spacing.S))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.M),
            verticalArrangement = Arrangement.spacedBy(Spacing.M),
            maxItemsInEachRow = 2
        ) {
            QuickActionCard(
                title = "New Quote",
                subtitle = "Create quotation",
                icon = Icons.Filled.Add,
                onClick = onNewQuotation,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Customers",
                subtitle = "Manage clients",
                icon = Icons.Filled.PersonAdd,
                onClick = onAddCustomer,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Cloud Backup",
                subtitle = "Sync to Drive",
                icon = Icons.Filled.CloudUpload,
                onClick = onBackup,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Quote History",
                subtitle = "View past quotes",
                icon = Icons.Filled.History,
                onClick = onHistory,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RecentQuotationsSection(summary: SyncSummary, onHistory: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Recent Quotations")
            TextButton(onClick = onHistory) {
                Text("View History", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(Spacing.XS))

        if (summary.quotationCount == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CornerRadius.Medium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.L),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No quotations created yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
                ActivityTile(
                    title = "Quotations Active in Database",
                    subtitle = "${summary.quotationCount} Total Quotation Record(s)",
                    time = "Just now",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = onHistory
                )
                ActivityTile(
                    title = "Customer Profiles Active",
                    subtitle = "${summary.customerCount} Registered Customer Record(s)",
                    time = "Today",
                    icon = Icons.Filled.People,
                    onClick = onHistory
                )
            }
        }
    }
}

@Composable
fun SubscriptionStatusSection() {
    Column {
        SectionHeader(title = "Subscription & License")
        Spacer(modifier = Modifier.height(Spacing.S))

        SubscriptionCard(
            title = "INTERIORPRO ENTERPRISE",
            statusText = "Commercial License Active",
            details = listOf(
                "Full Offline PDF & Calculation Engine",
                "AES-256 Encrypted Cloud Workspace Backups",
                "Unlimited Quotations & Masters Storage"
            )
        )
    }
}

@Composable
fun GoogleDriveStatusSection(summary: SyncSummary, onBackup: () -> Unit) {
    Column {
        SectionHeader(title = "Google Drive Status")
        Spacer(modifier = Modifier.height(Spacing.S))

        InformationCard(
            title = if (summary.isAccountConnected) "Google Drive Connected" else "Google Drive Disconnected",
            description = if (summary.isAccountConnected)
                "Account: ${summary.connectedAccount ?: "Active"}\nSync Status: ${summary.syncStatus}"
            else
                "Connect your account in Settings to enable encrypted workspace cloud backups.",
            icon = if (summary.isAccountConnected) Icons.Filled.CloudDone else Icons.Filled.CloudOff
        )
    }
}

@Composable
fun LastBackupInformationSection(summary: SyncSummary, onBackup: () -> Unit) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val lastBackupStr = summary.lastBackupTime?.let { dateFormatter.format(Date(it)) } ?: "Never Backed Up"
    val sizeInKb = if (summary.lastBackupSize > 0) "${summary.lastBackupSize / 1024} KB" else "0 KB"

    Column {
        SectionHeader(title = "Last Backup Information")
        Spacer(modifier = Modifier.height(Spacing.S))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CornerRadius.Large),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Small)
        ) {
            Column(modifier = Modifier.padding(Spacing.L)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(IconSize.Medium)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.M))
                        Column {
                            Text(
                                text = "Last Cloud Snapshot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = lastBackupStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    PremiumBadge(
                        text = summary.lastBackupStatus,
                        containerColor = if (summary.lastBackupStatus.equals("SUCCESS", ignoreCase = true))
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (summary.lastBackupStatus.equals("SUCCESS", ignoreCase = true))
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.M))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(Spacing.M))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Snapshot Size: $sizeInKb",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onBackup) {
                        Text("Manage Backup", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AppVersionFooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.L),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "InteriorPro ERP v1.0.0",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.XXS))
        Text(
            text = "Production Build • Secure Local-First Architecture",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
