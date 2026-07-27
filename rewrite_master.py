import sys

content = """package com.example.ui.company

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MasterEntity
import kotlinx.coroutines.delay
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
        "COLOUR" to "Colours & Shades",
        "GST_RATE" to "GST Rates",
        "PAYMENT_TERM" to "Payment Terms"
    )

    var selectedTabIndex by remember { mutableStateOf(0) }
    val (currentType, typeLabel) = masterTypes[selectedTabIndex]

    val searchQuery by masterViewModel.searchQuery.collectAsState()
    val sortBy by masterViewModel.sortBy.collectAsState()
    val showInactive by masterViewModel.showInactive.collectAsState()

    val filteredMasters by masterViewModel.getFilteredMasters(currentType).collectAsState(initial = emptyList())

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingMaster by remember { mutableStateOf<MasterEntity?>(null) }
        
    var formName by remember { mutableStateOf("") }
    var formDescription by remember { mutableStateOf("") }
    var formDisplayOrder by remember { mutableStateOf("0") }
    var formIsActive by remember { mutableStateOf(true) }

    var masterToDelete by remember { mutableStateOf<MasterEntity?>(null) }
    val context = LocalContext.current
    
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(currentType) {
        isLoading = true
        delay(300)
        isLoading = false
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingMaster = null
                    formName = ""
                    formDescription = ""
                    formDisplayOrder = "0"
                    formIsActive = true
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add $typeLabel") },
                text = { Text("Add ${typeLabel.trimEnd('s')}", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sticky Header Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Business Masters",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { masterViewModel.searchQuery.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search $typeLabel...") },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Search, 
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            ) 
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { masterViewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                    if (sortBy == MasterSortOption.DISPLAY_ORDER) "Sort: Order" else "Sort: Name A-Z",
                                    fontWeight = FontWeight.SemiBold
                                ) 
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (sortBy == MasterSortOption.DISPLAY_ORDER) Icons.AutoMirrored.Filled.Sort else Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Sort Icon",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )

                        FilterChip(
                            selected = showInactive,
                            onClick = { masterViewModel.showInactive.value = !showInactive },
                            label = { Text("Show Inactive", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showInactive) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Visibility Toggle",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth(),
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
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            // Content Section
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (filteredMasters.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isNotEmpty()) Icons.Outlined.SearchOff else Icons.Outlined.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No Results Found" else "No $typeLabel Found",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try adjusting your search criteria." else "Get started by adding a new record.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { masterViewModel.searchQuery.value = "" }) {
                                Text("Clear Search")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = {
                                editingMaster = null
                                formName = ""
                                formDescription = ""
                                formDisplayOrder = "0"
                                formIsActive = true
                                showAddEditDialog = true
                            }) {
                                Text("Add New Record")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredMasters, key = { it.id }) { master ->
                            MasterItemCard(
                                master = master,
                                typeLabel = typeLabel,
                                onToggleActive = { masterViewModel.toggleMasterActive(master) },
                                onEdit = {
                                    editingMaster = master
                                    formName = master.name
                                    formDescription = master.description
                                    formDisplayOrder = master.displayOrder.toString()
                                    formIsActive = master.isActive
                                    showAddEditDialog = true
                                },
                                onDuplicate = {
                                    masterViewModel.saveMaster(
                                        masterType = master.masterType,
                                        name = "${master.name} (Copy)",
                                        description = master.description,
                                        displayOrder = master.displayOrder,
                                        onDuplicate = {
                                            Toast.makeText(context, "Duplicate found!", Toast.LENGTH_SHORT).show()
                                        },
                                        onSuccess = {
                                            Toast.makeText(context, "Duplicated successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onDelete = { masterToDelete = master }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp)) // FAB padding
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        com.example.ui.components.PremiumDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = if (editingMaster != null) "Edit $typeLabel" else "Add to $typeLabel",
            actions = {
                com.example.ui.components.PremiumTextButton(onClick = { showAddEditDialog = false }) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                com.example.ui.components.PremiumPrimaryButton(
                    onClick = {
                        val trimmedName = formName.trim()
                        if (trimmedName.isEmpty()) {
                            Toast.makeText(context, "Name is required.", Toast.LENGTH_SHORT).show()
                            return@PremiumPrimaryButton
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
                    }
                ) { Text("Save") }
            }
        ) {
            com.example.ui.components.PremiumOutlinedTextField(
                value = formName,
                onValueChange = { formName = it },
                label = "Name *",
                placeholder = "e.g. Modular Wardrobe",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            com.example.ui.components.PremiumOutlinedTextField(
                value = formDescription,
                onValueChange = { formDescription = it },
                label = "Description",
                placeholder = "Optional helpful details",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            com.example.ui.components.PremiumOutlinedTextField(
                value = formDisplayOrder,
                onValueChange = { formDisplayOrder = it },
                label = "Display Order",
                placeholder = "Number for sorting",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
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
            }
        }
    }

    masterToDelete?.let { master ->
        com.example.ui.components.PremiumDialog(
            onDismissRequest = { masterToDelete = null },
            title = "Confirm Deletion",
            actions = {
                com.example.ui.components.PremiumTextButton(onClick = { masterToDelete = null }) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                com.example.ui.components.PremiumPrimaryButton(
                    onClick = {
                        masterViewModel.deleteMaster(master) { isSoftDeleted ->
                            masterToDelete = null
                            if (isSoftDeleted) {
                                Toast.makeText(context, "'${master.name}' has been deleted.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "'${master.name}' is in use, so it was set to Inactive to protect data.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            }
        ) {
            Text(
                text = "Are you sure you want to delete '${master.name}'?\n\n" +
                       "Please note: If this master parameter has been used in any past or active quotations, " +
                       "it will be automatically deactivated instead of permanently deleted to protect historic records."
            )
        }
    }
}

@Composable
fun MasterItemCard(
    master: MasterEntity,
    typeLabel: String,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (master.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = master.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (master.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!master.isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "INACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Category: $typeLabel",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (master.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = master.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Switch(
                    checked = master.isActive,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Display Order: ${master.displayOrder}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
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
"""

with open('/app/applet/app/src/main/java/com/example/ui/company/MasterDataScreen.kt', 'w') as f:
    f.write(content)

