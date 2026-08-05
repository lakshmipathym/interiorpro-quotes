package com.example.ui.history

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.pdf.PdfGenerator
import com.example.ui.components.*
import com.example.ui.customer.CustomerViewModel
import com.example.ui.theme.*
import com.example.utils.ShareManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuotationHistoryScreen(
    historyViewModel: HistoryViewModel,
    quotationViewModel: com.example.ui.quotation.QuotationViewModel,
    companyViewModel: com.example.ui.company.CompanyViewModel,
    customerViewModel: CustomerViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quotations by historyViewModel.allQuotations.collectAsStateWithLifecycle()
    val companyProfile by companyViewModel.companyProfile.collectAsStateWithLifecycle()
    val customersList by customerViewModel.customers.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedSortOption by remember { mutableStateOf("Newest First") }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Search, Filter, and Sort logic combined reactively
    val filteredAndSortedQuotes = remember(quotations, searchQuery, selectedStatusFilter, selectedSortOption) {
        quotations
            .filter { quote ->
                // Search filter (Quotation Number or Customer Name)
                val queryMatches = quote.customerName.contains(searchQuery, ignoreCase = true) ||
                        quote.quotationNumber.contains(searchQuery, ignoreCase = true)
                
                // Status filter
                val statusMatches = if (selectedStatusFilter == "All") {
                    true
                } else {
                    quote.status.equals(selectedStatusFilter, ignoreCase = true)
                }
                
                queryMatches && statusMatches
            }
            .sortedWith { q1, q2 ->
                when (selectedSortOption) {
                    "Newest First" -> q2.date.compareTo(q1.date) // descending
                    "Oldest First" -> q1.date.compareTo(q2.date) // ascending
                    "Customer Name" -> q1.customerName.compareTo(q2.customerName, ignoreCase = true)
                    else -> 0
                }
            }
    }

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val companyName = remember(companyProfile) {
        companyProfile?.companyName?.ifBlank { null } 
            ?: companyProfile?.ownerName?.ifBlank { null } 
            ?: "Contractor Partner"
    }

    val isSearchingOrFiltering = remember(searchQuery, selectedStatusFilter) {
        searchQuery.isNotEmpty() || selectedStatusFilter != "All"
    }

    val recentDraft = remember(quotations) {
        quotations.firstOrNull { it.status.lowercase(Locale.getDefault()) == "draft" }
    }

    val recentCustomers = remember(customersList) {
        customersList.take(5)
    }

    val recentQuotes = remember(quotations) {
        quotations.take(5)
    }

    var selectedQuoteForOptions by remember { mutableStateOf<Quotation?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Quotation?>(null) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    quotationViewModel.startNewQuotation()
                    onNavigateToCreate()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_new_quote"),
                text = { Text("New Quote", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Add, contentDescription = "New Quotation") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(400))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.L)
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.L),
                    contentPadding = PaddingValues(top = Spacing.L, bottom = Spacing.Section)
                ) {
                    // 1. App Bar Header & Greeting
                    item {
                        GreetingHeader(
                            greeting = greeting,
                            companyName = companyName
                        )
                    }

                    // 2. Search and Filtering Section
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            PremiumOutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = "Search Quotations",
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(Spacing.S))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.S),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val filters = listOf("All", "Draft", "Final", "Cancelled")
                                filters.forEach { filter ->
                                    val selected = selectedStatusFilter == filter
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedStatusFilter = filter },
                                        label = { Text(filter, style = MaterialTheme.typography.labelMedium) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.testTag("filter_chip_${filter.lowercase()}")
                                    )
                                }
                            }

                            if (isSearchingOrFiltering) {
                                Spacer(modifier = Modifier.height(Spacing.XS))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Sort,
                                            contentDescription = "Sort Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(IconSize.Small)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.XS))
                                        Text(
                                            text = "Sort by:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Box {
                                        AssistChip(
                                            onClick = { isSortMenuExpanded = true },
                                            label = { Text(selectedSortOption, style = MaterialTheme.typography.labelSmall) },
                                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(IconSize.Small)) },
                                            modifier = Modifier.testTag("sort_chip")
                                        )
                                        DropdownMenu(
                                            expanded = isSortMenuExpanded,
                                            onDismissRequest = { isSortMenuExpanded = false }
                                        ) {
                                            listOf("Newest First", "Oldest First", "Customer Name").forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        selectedSortOption = option
                                                        isSortMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Work Hub Content (If not active search/filter)
                    if (!isSearchingOrFiltering) {
                        if (quotations.isEmpty()) {
                            item {
                                EmptyHistoryState(
                                    onCreateQuote = {
                                        quotationViewModel.startNewQuotation()
                                        onNavigateToCreate()
                                    }
                                )
                            }
                        } else {
                            // --- TODAY'S SUMMARY SECTION ---
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    SectionHeader(title = "Today's Summary")
                                    Spacer(modifier = Modifier.height(Spacing.XS))
                                    
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().testTag("summary_row"),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.M),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.M),
                                        maxItemsInEachRow = 3
                                    ) {
                                        val stats = listOf(
                                            Triple("Total Quotes", quotations.size.toString(), Icons.AutoMirrored.Filled.List),
                                            Triple("Draft Quotes", quotations.count { it.status.lowercase() == "draft" }.toString(), Icons.Filled.EditNote),
                                            Triple("Total Clients", customersList.size.toString(), Icons.Filled.People)
                                        )
                                        
                                        stats.forEachIndexed { index, (label, count, icon) ->
                                            MetricCard(
                                                title = label,
                                                value = count,
                                                icon = icon,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("summary_card_$index")
                                            )
                                        }
                                    }
                                }
                            }

                            // --- QUICK ACTIONS SECTION ---
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    SectionHeader(title = "Quick Actions")
                                    Spacer(modifier = Modifier.height(Spacing.XS))

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.M),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.M),
                                        maxItemsInEachRow = 2
                                    ) {
                                        QuickActionCard(
                                            title = "New Quote",
                                            subtitle = "Create proposal",
                                            icon = Icons.Filled.Add,
                                            onClick = {
                                                quotationViewModel.startNewQuotation()
                                                onNavigateToCreate()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        QuickActionCard(
                                            title = "Filter Drafts",
                                            subtitle = "Show in-progress",
                                            icon = Icons.Filled.FilterList,
                                            onClick = { selectedStatusFilter = "Draft" },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // --- HERO SECTION ---
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("hero_section_card"),
                                    shape = RoundedCornerShape(CornerRadius.Large),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(Spacing.L)
                                    ) {
                                        Text(
                                            text = "Ready to start a project?",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(Spacing.XXS))
                                        Text(
                                            text = "Create a customized quotation, calculate GST dynamically, and generate instant PDF summaries offline.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                        Spacer(modifier = Modifier.height(Spacing.M))
                                        PremiumPrimaryButton(
                                            onClick = {
                                                quotationViewModel.startNewQuotation()
                                                onNavigateToCreate()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("hero_new_quote_button")
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(Spacing.S))
                                            Text("New Quotation", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // --- CONTINUE DRAFT SECTION ---
                            recentDraft?.let { draft ->
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        SectionHeader(title = "Continue Draft")
                                        Spacer(modifier = Modifier.height(Spacing.XS))
                                        
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    quotationViewModel.loadQuotationToEdit(draft)
                                                    onNavigateToEdit()
                                                }
                                                .testTag("continue_draft_card"),
                                            shape = RoundedCornerShape(CornerRadius.Large),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(Spacing.L),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        PremiumBadge(
                                                            text = "DRAFT",
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                        Text(
                                                            text = draft.quotationNumber,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(Spacing.XXS))
                                                    Text(
                                                        text = draft.customerName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    if (draft.projectType.isNotEmpty()) {
                                                        Text(
                                                            text = "${draft.projectType} (${draft.category})",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                                
                                                Column(
                                                    horizontalAlignment = Alignment.End,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "Rs. ${com.example.utils.CurrencyFormatter.formatIndianCurrency(draft.grandTotal)}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(Spacing.S))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.PlayArrow,
                                                            contentDescription = "Resume Draft",
                                                            tint = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.size(IconSize.Small)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // --- RECENT CUSTOMERS SECTION ---
                            if (recentCustomers.isNotEmpty()) {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        SectionHeader(title = "Recent Customers")
                                        Spacer(modifier = Modifier.height(Spacing.XS))
                                        
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.S)
                                        ) {
                                            recentCustomers.forEach { customer ->
                                                ActivityTile(
                                                    title = customer.customerName,
                                                    subtitle = if (customer.city.isNotBlank()) customer.city else "Client Record",
                                                    time = "Tap to Quote",
                                                    icon = Icons.Filled.Person,
                                                    onClick = {
                                                        quotationViewModel.startNewQuotation()
                                                        quotationViewModel.selectCustomer(customer)
                                                        onNavigateToCreate()
                                                    },
                                                    modifier = Modifier.testTag("customer_chip_${customer.customerId}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // --- RECENT QUOTATIONS TITLE ---
                            item {
                                SectionHeader(title = "Recent Quotations")
                            }

                            val countToShow = minOf(recentQuotes.size, 5)
                            if (countToShow == 0) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("empty_quotations_card"),
                                        shape = RoundedCornerShape(CornerRadius.Medium),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(Spacing.XL),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No quotations created yet.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(recentQuotes) { quote ->
                                    QuotationRowItem(
                                        quote = quote,
                                        modifier = Modifier.animateItem(),
                                        onSelect = { selectedQuoteForOptions = quote },
                                        onViewPdf = {
                                            scope.launch {
                                                try {
                                                    isGeneratingPdf = true
                                                    val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                                    ShareManager.openOrViewPdf(context, file)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to view PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isGeneratingPdf = false
                                                }
                                            }
                                        },
                                        onSharePdf = {
                                            scope.launch {
                                                try {
                                                    isGeneratingPdf = true
                                                    val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                                    ShareManager.shareQuotation(context, file, quote.quotationNumber)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isGeneratingPdf = false
                                                }
                                            }
                                        },
                                        onEdit = {
                                            quotationViewModel.loadQuotationToEdit(quote)
                                            onNavigateToEdit()
                                        },
                                        onDuplicate = {
                                            historyViewModel.duplicateQuotation(quote.id) { newNum ->
                                                scope.launch {
                                                    Toast.makeText(context, "Duplicated as $newNum", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onDelete = {
                                            showDeleteConfirm = quote
                                        },
                                        onStatusClick = {
                                            selectedQuoteForOptions = quote
                                        }
                                    )
                                }
                            }
                        } // End of else block for quotations.isEmpty() check

                    } else {
                        // 4. Searching/Filtering results List
                        if (filteredAndSortedQuotes.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "No matching quotations found",
                                    message = "Try a different search query or filter by a different status.",
                                    icon = Icons.Filled.SearchOff,
                                    actionLabel = "Reset Search",
                                    onActionClick = {
                                        searchQuery = ""
                                        selectedStatusFilter = "All"
                                    }
                                )
                            }
                        } else {
                            items(filteredAndSortedQuotes) { quote ->
                                QuotationRowItem(
                                    quote = quote,
                                    modifier = Modifier.animateItem(),
                                    onSelect = { selectedQuoteForOptions = quote },
                                    onViewPdf = {
                                        scope.launch {
                                            try {
                                                isGeneratingPdf = true
                                                val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                                ShareManager.openOrViewPdf(context, file)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed to view PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isGeneratingPdf = false
                                            }
                                        }
                                    },
                                    onSharePdf = {
                                        scope.launch {
                                            try {
                                                isGeneratingPdf = true
                                                val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                                ShareManager.shareQuotation(context, file, quote.quotationNumber)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed to share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isGeneratingPdf = false
                                            }
                                        }
                                    },
                                    onEdit = {
                                        quotationViewModel.loadQuotationToEdit(quote)
                                        onNavigateToEdit()
                                    },
                                    onDuplicate = {
                                        historyViewModel.duplicateQuotation(quote.id) { newNum ->
                                            scope.launch {
                                                Toast.makeText(context, "Duplicated as $newNum", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onDelete = {
                                        showDeleteConfirm = quote
                                    },
                                    onStatusClick = {
                                        selectedQuoteForOptions = quote
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG FOR OPTIONS ---
    selectedQuoteForOptions?.let { quote ->
        Dialog(onDismissRequest = { selectedQuoteForOptions = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CornerRadius.ExtraLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Medium)
            ) {
                Column(modifier = Modifier.padding(Spacing.XXL)) {
                    Text(
                        text = "Quotation: ${quote.quotationNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = quote.customerName, 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.L)
                    )
                    
                    ListItem(
                        headlineContent = { Text("Generate PDF & Share", style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = { Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        modifier = Modifier.clickable {
                            scope.launch {
                                try {
                                    isGeneratingPdf = true
                                    val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                    ShareManager.shareQuotation(context, file, quote.quotationNumber)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isGeneratingPdf = false
                                    selectedQuoteForOptions = null
                                }
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("View / Print PDF", style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            scope.launch {
                                try {
                                    isGeneratingPdf = true
                                    val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                    ShareManager.openOrViewPdf(context, file)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to view PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isGeneratingPdf = false
                                    selectedQuoteForOptions = null
                                }
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Edit Quotation", style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        modifier = Modifier.clickable {
                            quotationViewModel.loadQuotationToEdit(quote)
                            selectedQuoteForOptions = null
                            onNavigateToEdit()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Duplicate Quotation", style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                        modifier = Modifier.clickable {
                            historyViewModel.duplicateQuotation(quote.id) { newNum ->
                                scope.launch {
                                    Toast.makeText(context, "Duplicated as $newNum", Toast.LENGTH_SHORT).show()
                                }
                            }
                            selectedQuoteForOptions = null
                        }
                    )
                    
                    // Update Status Item with Dropdown inside Dialog
                    ListItem(
                        headlineContent = { Text("Change Status", style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = { Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                        trailingContent = {
                            var isStatusMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { isStatusMenuExpanded = true }) {
                                    Text(quote.status)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = isStatusMenuExpanded,
                                    onDismissRequest = { isStatusMenuExpanded = false }
                                ) {
                                    listOf("Draft", "Final", "Cancelled").forEach { statusOption ->
                                        DropdownMenuItem(
                                            text = { Text(statusOption) },
                                            onClick = {
                                                historyViewModel.updateQuotationStatus(quote.id, statusOption)
                                                isStatusMenuExpanded = false
                                                selectedQuoteForOptions = null
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text("Delete Quotation", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            showDeleteConfirm = quote
                            selectedQuoteForOptions = null
                        }
                    )
                }
            }
        }
    }

    // --- DELETE CONFIRM ---
    showDeleteConfirm?.let { quote ->
        PremiumDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = "Delete Quotation",
            actions = {
                PremiumTextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(Spacing.S))
                PremiumPrimaryButton(
                    onClick = {
                        historyViewModel.deleteQuotation(quote.id)
                        showDeleteConfirm = null
                        Toast.makeText(context, "Quotation deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete")
                }
            }
        ) {
            Text(
                text = "Are you sure you want to permanently delete quotation ${quote.quotationNumber}? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // --- PDF GENERATION PROGRESS DIALOG ---
    if (isGeneratingPdf) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(CornerRadius.ExtraLarge),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Large),
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.XXL),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(Spacing.XL))
                    Text(
                        text = "Generating PDF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.S))
                    Text(
                        text = "Rendering vector elements and styling document page...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GreetingHeader(
    greeting: String,
    companyName: String
) {
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
                    text = "$greeting,",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = companyName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
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

@Composable
fun EmptyHistoryState(
    onCreateQuote: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.M)
            .testTag("empty_history_card"),
        shape = RoundedCornerShape(CornerRadius.ExtraLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.XXL, vertical = Spacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(CornerRadius.ExtraLarge)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.XL))
            
            Text(
                text = "Create Your First Quotation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(Spacing.S))
            
            Text(
                text = "Manage your interior design proposals professionally. Create, view, duplicate, or export high-quality PDF quotations offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(Spacing.XXL))
            
            PremiumPrimaryButton(
                onClick = onCreateQuote,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_state_create_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.Medium)
                )
                Spacer(modifier = Modifier.width(Spacing.S))
                Text(
                    text = "New Quotation",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuotationRowItem(
    quote: Quotation,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onViewPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onStatusClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()) }
    val formattedDate = remember(quote.date) { sdf.format(Date(quote.date)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("quotation_card_${quote.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(CornerRadius.Large),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Small)
    ) {
        Column(modifier = Modifier.padding(Spacing.L)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quote.quotationNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.S))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quote.customerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                val statusColor = when (quote.status.lowercase(Locale.getDefault())) {
                    "final" -> MaterialTheme.colorScheme.primaryContainer
                    "cancelled" -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
                val statusTextColor = when (quote.status.lowercase(Locale.getDefault())) {
                    "final" -> MaterialTheme.colorScheme.onPrimaryContainer
                    "cancelled" -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(CornerRadius.Small))
                        .background(statusColor)
                        .clickable { onStatusClick() }
                        .padding(horizontal = Spacing.S, vertical = Spacing.XXS)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.XXS)
                    ) {
                        Text(
                            text = quote.status.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Change Status",
                            tint = statusTextColor,
                            modifier = Modifier.size(IconSize.Small)
                        )
                    }
                }
            }
            
            if (quote.projectType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.XXS))
                Text(
                    text = "${quote.projectType} (${quote.category})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Column {
                    Text(
                        text = "Grand Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(quote.grandTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.XXS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onViewPdf,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "View PDF",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(IconSize.Small)
                        )
                    }
                    
                    IconButton(
                        onClick = onSharePdf,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share PDF",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(IconSize.Small)
                        )
                    }
                    
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(IconSize.Small)
                        )
                    }
                    
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.Small)
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(IconSize.Small)
                        )
                    }
                }
            }
        }
    }
}
