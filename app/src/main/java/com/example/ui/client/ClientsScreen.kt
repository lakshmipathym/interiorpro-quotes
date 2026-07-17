package com.example.ui.client

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.Client
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClientsScreen(
    clientViewModel: ClientViewModel,
    onNavigateToAddClient: (() -> Unit)? = null,
    onNavigateToEditClient: ((Long) -> Unit)? = null,
    onSelect: ((Client) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModel State
    val clients by clientViewModel.clients.collectAsState()
    val searchQuery by clientViewModel.searchQuery.collectAsState()
    val sortBy by clientViewModel.sortBy.collectAsState()
    val showInactive by clientViewModel.showInactive.collectAsState()

    // Dialog & Form states
    var isFormOpen by remember { mutableStateOf<Client?>(null) } // null = closed, Client() = new, populated = edit
    var viewDetailsClient by remember { mutableStateOf<Client?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Client?>(null) }
    var showDuplicateWarning by remember { mutableStateOf<Client?>(null) }
    var pendingSaveClient by remember { mutableStateOf<Client?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (onNavigateToAddClient != null) {
                        onNavigateToAddClient()
                    } else {
                        isFormOpen = Client()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_client_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Client")
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
        ) {
            // --- HEADER ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Client Directory",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Manage your database of professional clients and site locations.",
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
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { clientViewModel.searchQuery.value = it },
                    placeholder = { Text("Search by name, email, company, mobile...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { clientViewModel.searchQuery.value = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                // Filter & Sort chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    ClientSortOption.values().forEach { option ->
                        val selected = sortBy == option
                        FilterChip(
                            selected = selected,
                            onClick = { clientViewModel.sortBy.value = option },
                            label = {
                                Text(
                                    text = when (option) {
                                        ClientSortOption.RECENTLY_ADDED -> "Recently Added"
                                        ClientSortOption.NAME_AZ -> "Name A-Z"
                                        ClientSortOption.NAME_ZA -> "Name Z-A"
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

                    FilterChip(
                        selected = showInactive,
                        onClick = { clientViewModel.showInactive.value = !showInactive },
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

            Spacer(modifier = Modifier.height(8.dp))

            // --- CLIENTS LIST / EMPTY STATE ---
            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isEmpty()) Icons.Filled.PeopleOutline else Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No Clients Yet" else "No Results Match",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) {
                                "Your client directory is currently empty. Tap the '+' button below to register your first client."
                            } else {
                                "Try adjusting your search criteria or toggling 'Show Inactive'."
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(280.dp)
                        )
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
                    items(clients, key = { it.clientId }) { client ->
                        Card(
                            onClick = {
                                if (onSelect != null) {
                                    onSelect(client)
                                } else {
                                    viewDetailsClient = client
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("client_card_${client.clientId}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (client.isActive) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar Circle
                                val initials = if (client.clientName.isNotBlank()) {
                                    client.clientName.trim().split("\\s+".toRegex())
                                        .take(2)
                                        .map { it.firstOrNull()?.uppercase() ?: "" }
                                        .joinToString("")
                                } else {
                                    "C"
                                }

                                val avatarBg = if (client.isActive) {
                                    val colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                    colors[client.clientId.toInt() % colors.size]
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Main Client details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = client.clientName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (client.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        if (!client.isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "Inactive",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }

                                    if (client.companyName.isNotBlank()) {
                                        Text(
                                            text = client.companyName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Phone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = client.mobileNumber.ifBlank { "No Phone" },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    if (client.siteLocation.isNotBlank() || client.city.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            val loc = listOf(client.siteLocation, client.city).filter { it.isNotBlank() }.joinToString(", ")
                                            Text(
                                                text = loc,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Quick Actions Column
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (onNavigateToEditClient != null) {
                                                onNavigateToEditClient(client.clientId)
                                            } else {
                                                isFormOpen = client
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit Client",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showDeleteConfirm = client },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Client",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
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

    // --- DIALOGS ---

    // 1. ADD / EDIT CLIENT FORM DIALOG
    isFormOpen?.let { currentClient ->
        val isNew = currentClient.clientId == 0L

        var name by remember { mutableStateOf(currentClient.clientName) }
        var company by remember { mutableStateOf(currentClient.companyName) }
        var contactPerson by remember { mutableStateOf(currentClient.contactPerson) }
        var mobile by remember { mutableStateOf(currentClient.mobileNumber) }
        var whatsapp by remember { mutableStateOf(currentClient.whatsappNumber) }
        var email by remember { mutableStateOf(currentClient.email) }
        var address by remember { mutableStateOf(currentClient.address) }
        var siteLocation by remember { mutableStateOf(currentClient.siteLocation) }
        var city by remember { mutableStateOf(currentClient.city) }
        var district by remember { mutableStateOf(currentClient.district) }
        var state by remember { mutableStateOf(currentClient.state) }
        var pincode by remember { mutableStateOf(currentClient.pincode) }
        var gstin by remember { mutableStateOf(currentClient.gstin) }
        var notes by remember { mutableStateOf(currentClient.notes) }
        var isActive by remember { mutableStateOf(currentClient.isActive) }

        Dialog(onDismissRequest = { isFormOpen = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Dialog Header
                    Text(
                        text = if (isNew) "Register New Client" else "Edit Client Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Form Fields with vertical scroll
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Client/Client Name *") },
                            modifier = Modifier.fillMaxWidth().testTag("client_name_field"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = { mobile = it },
                                label = { Text("Mobile Number *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f).testTag("client_mobile_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = whatsapp,
                                onValueChange = { whatsapp = it },
                                label = { Text("WhatsApp (Optional)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text("Company Name (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = contactPerson,
                            onValueChange = { contactPerson = it },
                            label = { Text("Secondary Contact Person") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = siteLocation,
                            onValueChange = { siteLocation = it },
                            label = { Text("Project / Site Location") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Billing / Full Address") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                label = { Text("State") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = pincode,
                                onValueChange = { pincode = it },
                                label = { Text("Pincode") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = gstin,
                                onValueChange = { gstin = it },
                                label = { Text("GSTIN (Optional)") },
                                modifier = Modifier.weight(1.8f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Client/Project Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Active Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Client Account Status",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isActive) "Active & available in directory" else "Inactive / Hidden from shortcuts",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Switch(
                                checked = isActive,
                                onCheckedChange = { isActive = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isFormOpen = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Client Name is required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (mobile.isBlank()) {
                                    Toast.makeText(context, "Mobile Number is required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val updated = currentClient.copy(
                                    clientName = name.trim(),
                                    companyName = company.trim(),
                                    contactPerson = contactPerson.trim(),
                                    mobileNumber = mobile.trim(),
                                    whatsappNumber = whatsapp.trim(),
                                    email = email.trim(),
                                    address = address.trim(),
                                    siteLocation = siteLocation.trim(),
                                    city = city.trim(),
                                    state = state.trim(),
                                    district = district.trim(),
                                    pincode = pincode.trim(),
                                    gstin = gstin.trim(),
                                    notes = notes.trim(),
                                    isActive = isActive
                                )

                                scope.launch {
                                    // Duplicate Check (Skip check if edit on the same client ID)
                                    val existing = clientViewModel.getClientByMobile(mobile)
                                    if (existing != null && existing.clientId != currentClient.clientId) {
                                        pendingSaveClient = updated
                                        showDuplicateWarning = existing
                                    } else {
                                        if (isNew) {
                                            clientViewModel.saveClient(updated) { id ->
                                                Toast.makeText(context, "Client registered successfully", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            clientViewModel.updateClient(updated) {
                                                Toast.makeText(context, "Client profile updated", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        isFormOpen = null
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isNew) "Register" else "Save Changes")
                        }
                    }
                }
            }
        }
    }

    // 2. VIEW DETAILS DIALOG
    viewDetailsClient?.let { client ->
        Dialog(onDismissRequest = { viewDetailsClient = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = client.clientName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (client.companyName.isNotBlank()) {
                                Text(
                                    text = client.companyName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Details Column
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailRow(icon = Icons.Filled.Phone, label = "Mobile", value = client.mobileNumber)
                        if (client.whatsappNumber.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Message, label = "WhatsApp", value = client.whatsappNumber)
                        }
                        if (client.email.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Email, label = "Email", value = client.email)
                        }
                        if (client.siteLocation.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.LocationOn, label = "Site Location", value = client.siteLocation)
                        }
                        if (client.address.isNotBlank()) {
                            val fullAddr = listOf(client.address, client.city, client.state, client.pincode)
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                            DetailRow(icon = Icons.Filled.Home, label = "Address", value = fullAddr)
                        }
                        if (client.gstin.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Assignment, label = "GSTIN", value = client.gstin)
                        }
                        if (client.notes.isNotBlank()) {
                            DetailRow(icon = Icons.Filled.Notes, label = "Notes", value = client.notes)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                isFormOpen = client
                                viewDetailsClient = null
                            }
                        ) {
                            Text("Edit Profile")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { viewDetailsClient = null }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    // 3. DUPLICATE CLIENT WARNING DIALOG
    showDuplicateWarning?.let { existing ->
        AlertDialog(
            onDismissRequest = {
                showDuplicateWarning = null
                pendingSaveClient = null
            },
            title = { Text("Duplicate Mobile Number") },
            text = {
                Text("A client named '${existing.clientName}' already exists with mobile number ${existing.mobileNumber}. Do you want to overwrite or register anyway?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toSave = pendingSaveClient
                        if (toSave != null) {
                            scope.launch {
                                // Overwrite existing client using their original clientId
                                clientViewModel.saveClient(toSave.copy(clientId = existing.clientId)) {
                                    Toast.makeText(context, "Client profile updated", Toast.LENGTH_SHORT).show()
                                }
                                isFormOpen = null
                                showDuplicateWarning = null
                                pendingSaveClient = null
                            }
                        }
                    }
                ) {
                    Text("Overwrite Existing")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val toSave = pendingSaveClient
                        if (toSave != null) {
                            scope.launch {
                                // Register as a distinct client by forcing clientId to 0
                                clientViewModel.saveClient(toSave.copy(clientId = 0)) {
                                    Toast.makeText(context, "Registered as distinct client", Toast.LENGTH_SHORT).show()
                                }
                                isFormOpen = null
                                showDuplicateWarning = null
                                pendingSaveClient = null
                            }
                        }
                    }
                ) {
                    Text("Register Distinct")
                }
            }
        )
    }

    // 4. DELETE CONFIRMATION DIALOG
    showDeleteConfirm?.let { client ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Client?") },
            text = { Text("Are you sure you want to permanently delete '${client.clientName}' from your directory? This action cannot be undone.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        clientViewModel.deleteClient(client) {
                            Toast.makeText(context, "Client deleted permanently", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteConfirm = null
                    }
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
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
