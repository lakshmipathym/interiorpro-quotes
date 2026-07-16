package com.example.ui.company

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MasterEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataScreen(masterViewModel: MasterViewModel) {
    val masterTypes = listOf(
        "PROJECT_TYPE" to "Projects",
        "CATEGORY" to "Categories",
        "MATERIAL" to "Materials",
        "FINISH_TYPE" to "Finishes",
        "UNIT" to "Units",
        "WARRANTY" to "Warranties",
        "ACCESSORY" to "Accessories",
        "TERMS" to "Terms & Conditions",
        "DOOR_SYSTEM" to "Door Systems",
        "HARDWARE" to "Hardware Packages",
        "BRAND" to "Brands",
        "THICKNESS" to "Thicknesses",
        "COLOUR" to "Colours & Shades"
    )

    var selectedTabIndex by remember { mutableStateOf(0) }
    val (currentType, typeLabel) = masterTypes[selectedTabIndex]

    // Observe flows from ViewModel
    val searchQuery by masterViewModel.searchQuery.collectAsState()
    val sortBy by masterViewModel.sortBy.collectAsState()
    val showInactive by masterViewModel.showInactive.collectAsState()

    val filteredMasters by masterViewModel.getFilteredMasters(currentType).collectAsState(initial = emptyList())

    // Dialog & Form State
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingMaster by remember { mutableStateOf<MasterEntity?>(null) }
    
    var formName by remember { mutableStateOf("") }
    var formDescription by remember { mutableStateOf("") }
    var formDisplayOrder by remember { mutableStateOf("0") }
    var formIsActive by remember { mutableStateOf(true) }

    // Confirmation dialogues
    var masterToDelete by remember { mutableStateOf<MasterEntity?>(null) }

    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingMaster = null
                    formName = ""
                    formDescription = ""
                    formDisplayOrder = "0"
                    formIsActive = true
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Master Item")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Business Masters",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search Bar & Filter Options Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { masterViewModel.searchQuery.value = it },
                    placeholder = { Text("Search by name...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search icon", tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { masterViewModel.searchQuery.value = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }

            // Quick Sort & Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort Chip
                FilterChip(
                    selected = sortBy == MasterSortOption.DISPLAY_ORDER,
                    onClick = {
                        masterViewModel.sortBy.value = if (sortBy == MasterSortOption.DISPLAY_ORDER) {
                            MasterSortOption.NAME_AZ
                        } else {
                            MasterSortOption.DISPLAY_ORDER
                        }
                    },
                    label = {
                        Text(
                            text = if (sortBy == MasterSortOption.DISPLAY_ORDER) "Sort: Order" else "Sort: Name A-Z",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (sortBy == MasterSortOption.DISPLAY_ORDER) Icons.Filled.SortByAlpha else Icons.Filled.FormatListNumbered,
                            contentDescription = "Sort Icon",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // Show Inactive Toggle Chip
                FilterChip(
                    selected = showInactive,
                    onClick = { masterViewModel.showInactive.value = !showInactive },
                    label = { Text("Show Inactive", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showInactive) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Visibility Toggle",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            // Scrollable Master Type Navigation Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                divider = { HorizontalDivider() }
            ) {
                masterTypes.forEachIndexed { index, pair ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = pair.second,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Options List Header Info
            Text(
                text = "Manage customizable parameters for $typeLabel:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Dynamic Content
            if (filteredMasters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inbox,
                            contentDescription = "Empty master list",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No masters configured.",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the '+' button to add your first custom $typeLabel record.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMasters, key = { it.id }) { master ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (master.isActive) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = master.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (master.isActive) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            },
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (master.description.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = master.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Status Switch & Quick Badges
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Order: ${master.displayOrder}", fontSize = 10.sp) },
                                            modifier = Modifier.height(28.dp)
                                        )
                                        Switch(
                                            checked = master.isActive,
                                            onCheckedChange = { masterViewModel.toggleMasterActive(master) },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(4.dp))

                                // Edit / Delete Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            editingMaster = master
                                            formName = master.name
                                            formDescription = master.description
                                            formDisplayOrder = master.displayOrder.toString()
                                            formIsActive = master.isActive
                                            showAddEditDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit master item",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    IconButton(
                                        onClick = { masterToDelete = master }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete master item",
                                            tint = MaterialTheme.colorScheme.error
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

    // --- ADD / EDIT DIALOG ---
    if (showAddEditDialog) {
        Dialog(onDismissRequest = { showAddEditDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    Text(
                        text = if (editingMaster != null) "Edit $typeLabel" else "Add to $typeLabel",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Form Fields
                    OutlinedTextField(
                        value = formName,
                        onValueChange = { formName = it },
                        label = { Text("Name *") },
                        placeholder = { Text("e.g. Modular Wardrobe") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = formDescription,
                        onValueChange = { formDescription = it },
                        label = { Text("Description") },
                        placeholder = { Text("Optional helpful details") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = formDisplayOrder,
                        onValueChange = { formDisplayOrder = it },
                        label = { Text("Display Order") },
                        placeholder = { Text("Number for sorting") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (editingMaster != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Status:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Switch(
                                checked = formIsActive,
                                onCheckedChange = { formIsActive = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Dialog Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showAddEditDialog = false }
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmedName = formName.trim()
                                if (trimmedName.isEmpty()) {
                                    Toast.makeText(context, "Name is required.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val orderVal = formDisplayOrder.toIntOrNull() ?: 0

                                if (editingMaster == null) {
                                    masterViewModel.saveMaster(
                                        masterType = currentType,
                                        name = trimmedName,
                                        description = formDescription,
                                        displayOrder = orderVal,
                                        onDuplicate = {
                                            Toast.makeText(context, "Duplicate found! A record with this name already exists.", Toast.LENGTH_LONG).show()
                                        },
                                        onSuccess = {
                                            showAddEditDialog = false
                                            Toast.makeText(context, "Added successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    masterViewModel.updateMaster(
                                        master = editingMaster!!,
                                        name = trimmedName,
                                        description = formDescription,
                                        displayOrder = orderVal,
                                        isActive = formIsActive,
                                        onDuplicate = {
                                            Toast.makeText(context, "Duplicate found! A record with this name already exists.", Toast.LENGTH_LONG).show()
                                        },
                                        onSuccess = {
                                            showAddEditDialog = false
                                            Toast.makeText(context, "Updated successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // --- CONFIRM DELETE DIALOG ---
    masterToDelete?.let { master ->
        AlertDialog(
            onDismissRequest = { masterToDelete = null },
            title = { Text("Confirm Deletion") },
            text = {
                Text(
                    text = "Are you sure you want to delete '${master.name}'?\n\n" +
                           "Please note: If this master parameter has been used in any past or active quotations, " +
                           "it will be automatically deactivated instead of permanently deleted to protect historic records."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        masterViewModel.deleteMaster(master) { isSoftDeleted ->
                            masterToDelete = null
                            if (isSoftDeleted) {
                                Toast.makeText(context, "'${master.name}' has been deleted.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "'${master.name}' is in use, so it was set to Inactive to protect data.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { masterToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
