package com.example.ui.customer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.data.CustomerEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomersScreen(
    customerViewModel: CustomerViewModel,
    onSelect: ((CustomerEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State collection from ViewModel
    val customers by customerViewModel.customers.collectAsState()
    val recentCustomers by customerViewModel.recentCustomers.collectAsState()
    val searchQuery by customerViewModel.searchQuery.collectAsState()
    val sortBy by customerViewModel.sortBy.collectAsState()
    val showInactive by customerViewModel.showInactive.collectAsState()

    // Dialog & Form states
    var isFormOpen by remember { mutableStateOf<CustomerEntity?>(null) } // null = closed, customerId=0 = new, else = edit
    var viewDetailsCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CustomerEntity?>(null) }
    var showDuplicateWarning by remember { mutableStateOf<CustomerEntity?>(null) }
    var pendingSaveCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isFormOpen = CustomerEntity() },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Customer")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
                .animateContentSize()
        ) {
            // --- HEADER ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Customer Directory",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Manage your clients, project sites, and contact details.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // --- SEARCH, SORT & FILTER BAR ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Row
                com.example.ui.components.PremiumOutlinedTextField(
    value = searchQuery,
    onValueChange = {customerViewModel.searchQuery.value = it},
    label = "",
    placeholder = "Search by name, mobile, site location...",
    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)

                // Sort & Filters Row
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Sorting Options Chips
                    SortOption.values().forEach { option ->
                        val selected = sortBy == option
                        FilterChip(
                            selected = selected,
                            onClick = { customerViewModel.sortBy.value = option },
                            label = {
                                Text(
                                    text = when (option) {
                                        SortOption.RECENTLY_ADDED -> "Recently Added"
                                        SortOption.NAME_AZ -> "Name A-Z"
                                        SortOption.NAME_ZA -> "Name Z-A"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    // Inactive Toggle Chip
                    FilterChip(
                        selected = showInactive,
                        onClick = { customerViewModel.showInactive.value = !showInactive },
                        label = {
                            Text(
                                text = "Show Inactive",
                                fontSize = 12.sp,
                                fontWeight = if (showInactive) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            if (showInactive) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            // --- RECENT CUSTOMERS HORIZONTAL LIST ---
            if (recentCustomers.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Recent Contacts",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentCustomers) { customer ->
                            ElevatedCard(
                                onClick = { viewDetailsCustomer = customer },
                                modifier = Modifier
                                    .animateItem()
                                    .width(130.dp)
                                    .height(90.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val initials = customer.customerName.take(2).uppercase()
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.secondary
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = customer.customerName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    if (customer.siteLocation.isNotEmpty()) {
                                        Text(
                                            text = customer.siteLocation,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- CUSTOMERS DIRECTORY LIST ---
            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 40.dp)
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
                                    imageVector = if (searchQuery.isEmpty()) Icons.Filled.PersonAdd else Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "No Customers Yet" else "No Results Match Your Search",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) {
                                    "Your customer directory is currently empty. Tap the '+' button below to register your first customer."
                                } else {
                                    "Try adjusting your search criteria or toggling 'Show Inactive'."
                                },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(280.dp),
                                lineHeight = 20.sp
                            )
                            if (searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { customerViewModel.searchQuery.value = "" },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reset Search")
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customers, key = { it.customerId }) { customer ->
                        ElevatedCard(
                            onClick = {
                                if (onSelect != null) {
                                    onSelect(customer)
                                } else {
                                    viewDetailsCustomer = customer
                                }
                            },
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .testTag("customer_card_${customer.customerId}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (customer.isActive) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            ),
                            elevation = CardDefaults.elevatedCardElevation(
                                defaultElevation = if (customer.isActive) 2.dp else 0.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar Circle
                                val initials = customer.customerName.take(1).uppercase()
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (customer.isActive) {
                                                MaterialTheme.colorScheme.primaryContainer
                                             } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                             }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        color = if (customer.isActive) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Customer Details Column
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = customer.customerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (customer.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        // Status badge
                                        if (!customer.isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Inactive",
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "Phone: ${customer.mobileNumber}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )

                                    if (customer.city.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.LocationCity,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = customer.city,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    } else if (customer.siteLocation.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = customer.siteLocation,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Quick Action Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (onSelect != null) {
                                        IconButton(
                                            onClick = { onSelect(customer) },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "Select Client",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    // Details Button
                                    IconButton(
                                        onClick = { viewDetailsCustomer = customer },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Info,
                                            contentDescription = "View Details",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Edit Button
                                    IconButton(
                                        onClick = { isFormOpen = customer },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = "Edit Profile",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Toggle/Deactivate Button
                                    IconButton(
                                        onClick = {
                                            if (customer.isActive) {
                                                showDeleteConfirm = customer
                                            } else {
                                                customerViewModel.toggleCustomerActive(customer)
                                                Toast.makeText(context, "Client re-activated", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (customer.isActive) Icons.Filled.Delete else Icons.Filled.Refresh,
                                            contentDescription = if (customer.isActive) "Soft Delete" else "Restore Client",
                                            tint = if (customer.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- VIEW DETAILS DIALOG ---
    viewDetailsCustomer?.let { customer ->
        Dialog(onDismissRequest = { viewDetailsCustomer = null }) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Client Profile",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { viewDetailsCustomer = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close sheet")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Initial Tag & Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        val initials = customer.customerName.take(2).uppercase()
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(customer.customerName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(
                                text = if (customer.isActive) "Status: Active Client" else "Status: Inactive Client",
                                fontSize = 12.sp,
                                color = if (customer.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Contact Section
                    DetailHeading(title = "Contact Information")
                    DetailRow(icon = Icons.Filled.Phone, label = "Mobile Number", value = customer.mobileNumber)
                    if (customer.whatsappNumber.isNotEmpty()) {
                        DetailRow(icon = Icons.Filled.Share, label = "WhatsApp Number", value = customer.whatsappNumber, isWhatsapp = true, number = customer.whatsappNumber)
                    }
                    if (customer.email.isNotEmpty()) {
                        DetailRow(icon = Icons.Filled.Email, label = "Email Address", value = customer.email)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Address/Site Location Section
                    DetailHeading(title = "Site & Billing details")
                    if (customer.siteLocation.isNotEmpty()) {
                        DetailRow(icon = Icons.Filled.LocationOn, label = "Project Site Location", value = customer.siteLocation)
                    }
                    if (customer.address.isNotEmpty()) {
                        DetailRow(icon = Icons.Filled.Home, label = "Billing Address", value = customer.address)
                    }

                    // City State ZIP etc.
                    val cityStateZip = buildString {
                        if (customer.city.isNotEmpty()) append(customer.city).append(", ")
                        if (customer.district.isNotEmpty()) append(customer.district).append(", ")
                        if (customer.state.isNotEmpty()) append(customer.state).append(" ")
                        if (customer.pincode.isNotEmpty()) append("- ").append(customer.pincode)
                    }.trim().trimEnd(',')
                    
                    if (cityStateZip.isNotEmpty()) {
                        DetailRow(icon = Icons.Filled.Map, label = "Location Hierarchy", value = cityStateZip)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notes & Activity Log
                    if (customer.notes.isNotEmpty()) {
                        DetailHeading(title = "Client & Project Notes")
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = customer.notes,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Metadata dates
                    DetailHeading(title = "System Registry Logs")
                    val df = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    val createdStr = df.format(Date(customer.createdDate))
                    val modifiedStr = df.format(Date(customer.modifiedDate))
                    
                    Text("Registered On: $createdStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Last Modified: $modifiedStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${customer.mobileNumber}")
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Call Customer", tint = MaterialTheme.colorScheme.primary)
                        }

                        if (customer.whatsappNumber.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val formattedNum = customer.whatsappNumber.replace(Regex("[^0-9]"), "")
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNum")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                            }
                        }

                        Button(
                            onClick = { viewDetailsCustomer = null },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }

    // --- FORM DIALOG (ADD / EDIT) ---
    isFormOpen?.let { customer ->
        var name by remember(customer.customerId) { mutableStateOf(customer.customerName) }
        var mobile by remember(customer.customerId) { mutableStateOf(customer.mobileNumber) }
        var whatsapp by remember(customer.customerId) { mutableStateOf(customer.whatsappNumber) }
        var email by remember(customer.customerId) { mutableStateOf(customer.email) }
        var address by remember(customer.customerId) { mutableStateOf(customer.address) }
        var siteLoc by remember(customer.customerId) { mutableStateOf(customer.siteLocation) }
        var city by remember(customer.customerId) { mutableStateOf(customer.city) }
        var district by remember(customer.customerId) { mutableStateOf(customer.district) }
        var state by remember(customer.customerId) { mutableStateOf(customer.state) }
        var pincode by remember(customer.customerId) { mutableStateOf(customer.pincode) }
        var notes by remember(customer.customerId) { mutableStateOf(customer.notes) }
        var isActive by remember(customer.customerId) { mutableStateOf(customer.isActive) }

        var companyName by remember(customer.customerId) { mutableStateOf(customer.companyName) }
        var contactPerson by remember(customer.customerId) { mutableStateOf(customer.contactPerson) }
        var gstin by remember(customer.customerId) { mutableStateOf(customer.gstin) }
        var siteAddress by remember(customer.customerId) { mutableStateOf(customer.siteAddress) }
        var country by remember(customer.customerId) { mutableStateOf(customer.country) }

        // Validation Error States
        var nameError by remember(customer.customerId) { mutableStateOf<String?>(null) }
        var mobileError by remember(customer.customerId) { mutableStateOf<String?>(null) }
        var emailError by remember(customer.customerId) { mutableStateOf<String?>(null) }
        var gstinError by remember(customer.customerId) { mutableStateOf<String?>(null) }

        val focusRequester = remember(customer.customerId) { FocusRequester() }

        LaunchedEffect(customer) {
            kotlinx.coroutines.delay(150)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore if not attached
            }
        }

        // Duplicate Check Warning Sheet trigger
        var checkingDuplicateByMobile by remember { mutableStateOf(false) }

                com.example.ui.components.PremiumDialog(
            onDismissRequest = { isFormOpen = null },
            title = if (customer.customerId == 0L) "Add Client Profile" else "Edit Client Profile",
            actions = {
                com.example.ui.components.PremiumTextButton(onClick = { isFormOpen = null }) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                com.example.ui.components.PremiumPrimaryButton(
                    onClick = {
                        val trimmedName = name.trim()
                        val trimmedMobile = mobile.trim()
                        val trimmedEmail = email.trim()
                        
                        val hasNameError = trimmedName.isEmpty()
                        val hasMobileError = trimmedMobile.isEmpty() || !com.example.utils.ValidationManager.isValidPhone(trimmedMobile)
                        val hasEmailError = trimmedEmail.isNotEmpty() && !com.example.utils.ValidationManager.isValidEmail(trimmedEmail)
                        val trimmedGstin = gstin.trim().uppercase()
                        val hasGstinError = trimmedGstin.isNotEmpty() && !com.example.utils.ValidationManager.isValidGstin(trimmedGstin)

                        nameError = if (trimmedName.isEmpty()) "Customer Name is required." else null
                        mobileError = if (trimmedMobile.isEmpty()) {
                            "Mobile Number is required."
                        } else if (!com.example.utils.ValidationManager.isValidPhone(trimmedMobile)) {
                            "Please enter a valid 10-15 digit Mobile Number."
                        } else null
                        emailError = if (trimmedEmail.isNotEmpty() && !com.example.utils.ValidationManager.isValidEmail(trimmedEmail)) {
                            "Please enter a valid Email Address."
                        } else null
                        gstinError = if (hasGstinError) "Please enter a valid 15-character GSTIN." else null

                        if (!hasNameError && !hasMobileError && !hasEmailError && !hasGstinError) {
                            scope.launch {
                                val existing = customerViewModel.getCustomerByMobile(trimmedMobile)
                                val savedCustomer = CustomerEntity(
                                    customerId = customer.customerId,
                                    customerName = trimmedName,
                                    mobileNumber = trimmedMobile,
                                    whatsappNumber = whatsapp.trim(),
                                    email = trimmedEmail,
                                    address = address.trim(),
                                    siteLocation = siteLoc.trim(),
                                    city = city.trim(),
                                    district = district.trim(),
                                    state = state.trim(),
                                    pincode = pincode.trim(),
                                    notes = notes.trim(),
                                    createdDate = if (customer.customerId == 0L) System.currentTimeMillis() else customer.createdDate,
                                    modifiedDate = System.currentTimeMillis(),
                                    isActive = isActive,
                                    companyName = companyName.trim(),
                                    contactPerson = contactPerson.trim(),
                                    gstin = gstin.trim().uppercase(),
                                    siteAddress = siteAddress.trim(),
                                    country = country.trim()
                                )
                                // If existing customer found, and we are either:
                                // 1. In Add Mode (customerId == 0)
                                // 2. Editing (but different customer ID)
                                if (existing != null && (customer.customerId == 0L || existing.customerId != customer.customerId)) {
                                    showDuplicateWarning = existing
                                    pendingSaveCustomer = savedCustomer
                                } else {
                                    // Save directly, no duplicates
                                    if (customer.customerId == 0L) {
                                        customerViewModel.saveCustomer(savedCustomer)
                                    } else {
                                        customerViewModel.updateCustomer(savedCustomer)
                                    }
                                    isFormOpen = null
                                    Toast.makeText(context, "Client registered successfully.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("form_save_button")
                ) { Text("Save") }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                    com.example.ui.components.PremiumOutlinedTextField(
    value = name,
    onValueChange = {name = it 
                            if (it.trim().isNotEmpty()) {
                                nameError = null
                            }},
    label = "Customer Name *",
    placeholder = "Eg: Ramesh Kumar",
    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
    isError = nameError != null,
    errorMessage = nameError,
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                    

                    com.example.ui.components.PremiumOutlinedTextField(
    value = contactPerson,
    onValueChange = {contactPerson = it},
    label = "Contact Person (Optional)",
    placeholder = "Eg: Mr. Ramesh",
    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                    

                    com.example.ui.components.PremiumOutlinedTextField(
    value = mobile,
    onValueChange = {mobile = it 
                            if (it.trim().isNotEmpty()) {
                                mobileError = null
                            }},
    label = "Mobile Number *",
    placeholder = "Eg: 9876543210",
    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    isError = mobileError != null,
    errorMessage = mobileError,
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                    

                    com.example.ui.components.PremiumOutlinedTextField(
    value = whatsapp,
    onValueChange = {whatsapp = it},
    label = "WhatsApp Number",
    placeholder = "Eg: 9876543210",
    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                    

                    com.example.ui.components.PremiumOutlinedTextField(
    value = email,
    onValueChange = {email = it 
                            emailError = null},
    label = "Email Address",
    placeholder = "Eg: ramesh@gmail.com",
    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
    isError = emailError != null,
    errorMessage = emailError,
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                    

                    com.example.ui.components.PremiumOutlinedTextField(
    value = companyName,
    onValueChange = {companyName = it},
    label = "Company Name (Optional)",
    placeholder = "Eg: ABC Builders",
    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                    

                    com.example.ui.components.PremiumOutlinedTextField(
    value = gstin,
    isError = gstinError != null,
    errorMessage = gstinError,
    onValueChange = {gstin = it
        if (it.trim().isNotEmpty()) {
            gstinError = null
        }
    },
    label = "Client GSTIN (Optional)",
    placeholder = "Eg: 33ABCDE1234F1Z5",
    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                    

                    com.example.ui.components.PremiumOutlinedTextField(
    value = siteLoc,
    onValueChange = {siteLoc = it},
    label = "Project Site Location",
    placeholder = "Eg: Green Villa",
    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                    

                    com.example.ui.components.PremiumOutlinedTextField(
    value = address,
    onValueChange = {address = it},
    label = "Billing Address",
    placeholder = "Eg: 123, ABC Street, Near Landmark, City",
    leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                    

                    com.example.ui.components.PremiumOutlinedTextField(
                        value = siteAddress,
                        onValueChange = {siteAddress = it},
                        label = "Site Address (Optional)",
                        placeholder = "Eg: 123, ABC Street, Near Landmark, City",
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = country,
                        onValueChange = {country = it},
                        label = "Country",
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    

                    // City / District Row
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = city,
                        onValueChange = {city = it},
                        label = "City",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = district,
                        onValueChange = {district = it},
                        label = "District",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    

                    // State / PIN Row
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = state,
                        onValueChange = {state = it},
                        label = "State",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = pincode,
                        onValueChange = {pincode = it},
                        label = "Pincode",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    

                    com.example.ui.components.PremiumOutlinedTextField(
    value = notes,
    onValueChange = {notes = it},
    label = "Notes / Requirements",
    placeholder = "Eg: Needs completion by next month",
    modifier = Modifier.fillMaxWidth()
)
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    // Active Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }

                    if (customer.customerId != 0L) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val df = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        Text(
                            text = "Last Modified: ${df.format(Date(customer.modifiedDate))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
            }
    }
    }


    // --- DUPLICATE WARNING MODAL ---
    showDuplicateWarning?.let { existingCustomer ->
        com.example.ui.components.PremiumDialog(
            onDismissRequest = { 
                showDuplicateWarning = null
                pendingSaveCustomer = null
            },
            title = "Duplicate Client Detected",
            actions = {
                com.example.ui.components.PremiumTextButton(
                    onClick = {
                        // Overwrite and save anyway
                        pendingSaveCustomer?.let { toSave ->
                            if (toSave.customerId == 0L) {
                                customerViewModel.saveCustomer(toSave)
                            } else {
                                customerViewModel.updateCustomer(toSave)
                            }
                            isFormOpen = null
                            showDuplicateWarning = null
                            pendingSaveCustomer = null
                            Toast.makeText(context, "Client profile saved.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Proceed Anyway")
                }
                Spacer(modifier = Modifier.width(8.dp))
                com.example.ui.components.PremiumPrimaryButton(
                    onClick = {
                        // Open Existing Customer in Form!
                        isFormOpen = existingCustomer
                        showDuplicateWarning = null
                        pendingSaveCustomer = null
                    }
                ) {
                    Text("Open Existing")
                }
            }
        ) {
            Text(
                text = "A client named '${existingCustomer.customerName}' is already registered with mobile number ${existingCustomer.mobileNumber}.\n\nWould you like to open the existing profile to view/edit instead of saving a duplicate record?"
            )
        }
    }

    // --- SOFT DELETE CONFIRMATION DIALOG ---
    showDeleteConfirm?.let { customer ->
        com.example.ui.components.PremiumDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = "Deactivate Client?",
            actions = {
                com.example.ui.components.PremiumTextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                com.example.ui.components.PremiumPrimaryButton(
                    onClick = {
                        customerViewModel.deleteCustomer(customer)
                        showDeleteConfirm = null
                        Toast.makeText(context, "Client profile deactivated.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Deactivate")
                }
            }
        ) {
            Text("Are you sure you want to soft delete / deactivate '${customer.customerName}'?\n\nThis will hide them from active selections. Past quotations referencing this client will remain completely intact.")
        }
    }
}

// --- SUB COMPONENT HELPER ---
@Composable
fun DetailHeading(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isWhatsapp: Boolean = false,
    number: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// --- REWRITTEN QUICK ADD CUSTOMER INLINE DIALOG FOR SELECTIONS ---
@Composable
fun QuickAddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var siteName by remember { mutableStateOf("") }
    val context = LocalContext.current

    com.example.ui.components.PremiumDialog(
        onDismissRequest = onDismiss,
        title = "Quick Add Client",
        actions = {
            com.example.ui.components.PremiumTextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            com.example.ui.components.PremiumPrimaryButton(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedPhone = phone.trim()
                    
                    if (trimmedName.isEmpty()) {
                        Toast.makeText(context, "Client Name is required", Toast.LENGTH_SHORT).show()
                    } else if (trimmedPhone.isEmpty()) {
                        Toast.makeText(context, "Phone Number is required", Toast.LENGTH_SHORT).show()
                    } else if (!com.example.utils.ValidationManager.isValidPhone(trimmedPhone)) {
                        Toast.makeText(context, "Please enter a valid 10-15 digit phone number.", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(
                            CustomerEntity(
                                customerName = trimmedName,
                                mobileNumber = trimmedPhone,
                                siteLocation = siteName.trim(),
                                address = address.trim(),
                                isActive = true
                            )
                        )
                    }
                }
            ) { Text("Save & Select") }
        }
    ) {
                
                com.example.ui.components.PremiumOutlinedTextField(
    value = name,
    onValueChange = {name = it},
    label = "Client Name *",
    placeholder = "Eg: Ramesh Kumar",
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                

                com.example.ui.components.PremiumOutlinedTextField(
    value = phone,
    onValueChange = {phone = it},
    label = "Phone Number *",
    placeholder = "Eg: 9876543210",
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                

                com.example.ui.components.PremiumOutlinedTextField(
    value = siteName,
    onValueChange = {siteName = it},
    label = "Site Name (e.g., Block B, Apt 402)",
    placeholder = "Eg: Green Villa",
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
                

                com.example.ui.components.PremiumOutlinedTextField(
    value = address,
    onValueChange = {address = it},
    label = "Site / Billing Address",
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)

                Spacer(modifier = Modifier.height(20.dp))

    }
}
