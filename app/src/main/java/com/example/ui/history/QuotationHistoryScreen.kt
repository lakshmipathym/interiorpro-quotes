package com.example.ui.history

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.pdf.PdfGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.utils.ShareManager
import com.example.ui.customer.CustomerViewModel
import androidx.compose.ui.platform.testTag

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
    val quotations by historyViewModel.allQuotations.collectAsState()
    val companyProfile by companyViewModel.companyProfile.collectAsState()
    val customersList by customerViewModel.customers.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedSortOption by remember { mutableStateOf("Newest First") }

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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    quotationViewModel.startNewQuotation()
                    onNavigateToCreate()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_new_quote")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Quotation")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                // 1. App Bar Header & Greeting
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "InteriorPro Quotes",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$greeting,",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = companyName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Home,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Search and Filtering Section
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search Quotations") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("search_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val filters = listOf("All", "Draft", "Final", "Cancelled")
                            filters.forEach { filter ->
                                val selected = selectedStatusFilter == filter
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedStatusFilter = filter },
                                    label = { Text(filter) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("filter_chip_${filter.lowercase()}")
                                )
                            }
                        }

                        if (isSearchingOrFiltering) {
                            Spacer(modifier = Modifier.height(4.dp))
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sort by:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Box {
                                    AssistChip(
                                        onClick = { isSortMenuExpanded = true },
                                        label = { Text(selectedSortOption, fontSize = 11.sp) },
                                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp)) },
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
                        // --- HERO SECTION ---
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("hero_section_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "Ready to start a project?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Create a customized quotation, calculate GST dynamically, and generate instant PDF summaries offline.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        quotationViewModel.startNewQuotation()
                                        onNavigateToCreate()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("hero_new_quote_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("New Quotation", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // --- CONTINUE DRAFT SECTION ---
                    recentDraft?.let { draft ->
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Continue Draft",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            quotationViewModel.loadQuotationToEdit(draft)
                                            onNavigateToEdit()
                                        }
                                        .testTag("continue_draft_card"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "DRAFT",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = draft.quotationNumber,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = draft.customerName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            if (draft.projectType.isNotEmpty()) {
                                                Text(
                                                    text = "${draft.projectType} (${draft.category})",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "Rs. ${com.example.utils.CurrencyFormatter.formatIndianCurrency(draft.grandTotal)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = "Resume Draft",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(20.dp)
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
                                Text(
                                    text = "Recent Customers",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recentCustomers.forEach { customer ->
                                        val initials = customer.customerName.split(" ")
                                            .filter { it.isNotEmpty() }
                                            .take(2)
                                            .joinToString("") { it.take(1).uppercase() }
                                            .ifEmpty { "C" }
                                            
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    quotationViewModel.startNewQuotation()
                                                    quotationViewModel.selectCustomer(customer)
                                                    onNavigateToCreate()
                                                }
                                                .testTag("customer_chip_${customer.customerId}"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = initials,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = customer.customerName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = if (customer.city.isNotBlank()) customer.city else "Contract",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Filled.Add,
                                                    contentDescription = "New Quotation",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- TODAY'S SUMMARY SECTION ---
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Today's Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().testTag("summary_row"),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val stats = listOf(
                                    Triple("Total Quotes", quotations.size.toString(), MaterialTheme.colorScheme.primaryContainer),
                                    Triple("Draft Quotes", quotations.count { it.status.lowercase() == "draft" }.toString(), MaterialTheme.colorScheme.secondaryContainer),
                                    Triple("Total Clients", customersList.size.toString(), MaterialTheme.colorScheme.tertiaryContainer)
                                )
                                
                                stats.forEachIndexed { index, (label, count, color) ->
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .testTag("summary_card_$index"),
                                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = count,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- RECENT QUOTATIONS TITLE ---
                    item {
                        Text(
                            text = "Recent Quotations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    val countToShow = minOf(recentQuotes.size, 5)
                    if (countToShow == 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("empty_quotations_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No quotations created yet.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(recentQuotes) { quote ->
                            QuotationRowItem(
                                quote = quote,
                                onSelect = { selectedQuoteForOptions = quote },
                                onViewPdf = {
                                    scope.launch {
                                        val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                        ShareManager.openOrViewPdf(context, file)
                                    }
                                },
                                onSharePdf = {
                                    scope.launch {
                                        val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                        ShareManager.shareQuotation(context, file, quote.quotationNumber)
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No matching quotations found",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Try a different search query or filter by a different status.",
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            searchQuery = ""
                                            selectedStatusFilter = "All"
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Reset Filters")
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredAndSortedQuotes) { quote ->
                            QuotationRowItem(
                                quote = quote,
                                onSelect = { selectedQuoteForOptions = quote },
                                onViewPdf = {
                                    scope.launch {
                                        val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                        ShareManager.openOrViewPdf(context, file)
                                    }
                                },
                                onSharePdf = {
                                    scope.launch {
                                        val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                        ShareManager.shareQuotation(context, file, quote.quotationNumber)
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

    // --- DIALOG FOR OPTIONS ---
    selectedQuoteForOptions?.let { quote ->
        Dialog(onDismissRequest = { selectedQuoteForOptions = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Quotation: ${quote.quotationNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = quote.customerName, 
                        fontSize = 14.sp, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    ListItem(
                        headlineContent = { Text("Generate PDF & Share") },
                        leadingContent = { Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        modifier = Modifier.clickable {
                            scope.launch {
                                val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                ShareManager.shareQuotation(context, file, quote.quotationNumber)
                                selectedQuoteForOptions = null
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("View / Print PDF") },
                        leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            scope.launch {
                                val file = ShareManager.generateQuotationPdf(context, historyViewModel.repository, quote.id)
                                ShareManager.openOrViewPdf(context, file)
                                selectedQuoteForOptions = null
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Edit Quotation") },
                        leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        modifier = Modifier.clickable {
                            quotationViewModel.loadQuotationToEdit(quote)
                            selectedQuoteForOptions = null
                            onNavigateToEdit()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Duplicate Quotation") },
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
                        headlineContent = { Text("Change Status") },
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
                        headlineContent = { Text("Delete Quotation", color = MaterialTheme.colorScheme.error) },
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Quotation") },
            text = { Text("Are you sure you want to permanently delete quotation ${quote.quotationNumber}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        historyViewModel.deleteQuotation(quote.id)
                        showDeleteConfirm = null
                        Toast.makeText(context, "Quotation deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyHistoryState(
    onCreateQuote: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("empty_history_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Create Your First Quotation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Manage your interior design proposals professionally. Create, view, duplicate, or export high-quality PDF quotations offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onCreateQuote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("empty_state_create_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Quotation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuotationRowItem(
    quote: Quotation,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("quotation_card_${quote.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
            
            Spacer(modifier = Modifier.height(10.dp))
            
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor)
                        .clickable { onStatusClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = quote.status.uppercase(Locale.getDefault()),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Change Status",
                            tint = statusTextColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            
            if (quote.projectType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${quote.projectType} (${quote.category})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                            modifier = Modifier.size(20.dp)
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
                            modifier = Modifier.size(20.dp)
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
                            modifier = Modifier.size(20.dp)
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
                            modifier = Modifier.size(20.dp)
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

