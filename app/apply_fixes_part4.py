file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Locate the start and end indices of the corrupted block
start_marker = "// --- STEP 1: CUSTOMER SELECTION & QUOTATION ITEMS ---"
end_marker = "@Composable\\nfun SpecTagChip"

# Since end_marker has escaping in python, let's look for "@Composable\\nfun SpecTagChip" or "@Composable\nfun SpecTagChip"
if "@Composable\nfun SpecTagChip" in content:
    end_text = "@Composable\nfun SpecTagChip"
else:
    end_text = "@Composable\r\nfun SpecTagChip"

start_idx = content.find(start_marker)
end_idx = content.find(end_text)

if start_idx != -1 and end_idx != -1:
    print(f"Markers found! start_idx: {start_idx}, end_idx: {end_idx}")
    
    new_wizard_steps_code = """// --- STEP 1: CUSTOMER SELECTION & QUOTATION ITEMS ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WizardStepCustomer(
    customerViewModel: com.example.ui.customer.CustomerViewModel,
    quotationViewModel: com.example.ui.quotation.QuotationViewModel,
    quoteItems: List<QuotationItem>,
    onOpenQuickAdd: () -> Unit
) {
    val customers by customerViewModel.customers.collectAsState()
    val searchQuery by customerViewModel.searchQuery.collectAsState()
    val selectedCustomer by quotationViewModel.newQuoteCustomer.collectAsState()

    val siteName by quotationViewModel.newQuoteSiteName.collectAsState()
    val siteAddress by quotationViewModel.newQuoteSiteAddress.collectAsState()

    var showItemConfigDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedCustomer == null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Customer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Search Box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { customerViewModel.searchQuery.value = it },
                            label = { Text("Search Customer (Name, Phone...)") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { customerViewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Customers list
                        val displayList = customers.take(4)
                        if (displayList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No customers found. Click 'Add Customer Inline' below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                displayList.forEach { cust ->
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                quotationViewModel.selectCustomer(cust)
                                            },
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = cust.customerName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Phone: ${cust.mobileNumber}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Filled.AddCircle,
                                                contentDescription = "Select",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Inline Add Customer Button
                        Button(
                            onClick = onOpenQuickAdd,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Customer (Inline)")
                        }
                    }
                }
            } else {
                // Selected Customer Elegantly
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedCustomer!!.customerName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Phone: ${selectedCustomer!!.mobileNumber}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { quotationViewModel.startNewQuotation() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Change", fontSize = 11.sp)
                        }
                    }
                }

                // Site Details Section
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Site Location Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = siteName,
                            onValueChange = {
                                quotationViewModel.updateSiteDetails(it, siteAddress)
                            },
                            label = { Text("Site Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = siteAddress,
                            onValueChange = {
                                quotationViewModel.updateSiteDetails(siteName, it)
                            },
                            label = { Text("Site Address *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2
                        )
                    }
                }

                // Clean Item List Title & Content
                Text(
                    text = "Quotation Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (quoteItems.isEmpty()) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LibraryAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No items added yet.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Use the '+' button below to add your first item.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        quoteItems.forEachIndexed { index, item ->
                            QuotationItemCard(
                                item = item,
                                index = index,
                                onEdit = {
                                    editingItemIndex = index
                                    showItemConfigDialog = true
                                },
                                onDuplicate = { quotationViewModel.duplicateQuoteItem(index) },
                                onDelete = { quotationViewModel.removeQuoteItem(index) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
        }

        // Floating Action Button overlay for adding item directly
        if (selectedCustomer != null) {
            FloatingActionButton(
                onClick = {
                    editingItemIndex = null
                    showItemConfigDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Item")
            }
        }
    }

    if (showItemConfigDialog) {
        val masterDataVal = quotationViewModel.allMasterData.collectAsState().value
        val finishes = masterDataVal
            .filter { it.masterType == "FINISH_TYPE" }
            .map { it.name }
        ItemConfigDialog(
            itemIndex = editingItemIndex,
            currentItems = quoteItems,
            onDismiss = { showItemConfigDialog = false },
            onSave = { updatedOrNewItem ->
                if (editingItemIndex == null) {
                    quotationViewModel.addQuoteItem(updatedOrNewItem)
                } else {
                    quotationViewModel.updateQuoteItem(editingItemIndex!!, updatedOrNewItem)
                }
                showItemConfigDialog = false
            },
            finishes = finishes,
            allMasterData = masterDataVal,
            projectType = quotationViewModel.newQuoteProjectType.collectAsState().value,
            category = quotationViewModel.newQuoteCategory.collectAsState().value
        )
    }
}

// --- STEP 2: PROJECT SELECTION ---
@Composable
fun WizardStepConfig(
    templates: List<QuotationTemplate>,
    projectTypes: List<String>,
    materials: List<String>,
    currentTemplate: QuotationTemplate?,
    currentMaterial: String,
    currentProjectType: String,
    onSelectTemplate: (QuotationTemplate?) -> Unit,
    onSelectMaterial: (String) -> Unit,
    onSelectProjectType: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Project Scope",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                com.example.ui.components.PremiumDropdown(
                    value = currentTemplate?.name ?: "No Preset (Custom)",
                    onValueChange = { name -> 
                        if (name == "No Preset (Custom)") {
                            onSelectTemplate(null)
                        } else {
                            val tmpl = templates.find { it.name == name }
                            if (tmpl != null) onSelectTemplate(tmpl)
                        }
                    },
                    label = "Project Preset (Optional)",
                    options = listOf("No Preset (Custom)") + templates.map { it.name },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                com.example.ui.components.PremiumDropdown(
                    value = currentProjectType.ifEmpty { "Select Product Type" },
                    onValueChange = onSelectProjectType,
                    label = "Product Type *",
                    options = if (projectTypes.isEmpty()) listOf("Modular Kitchen", "Wardrobe", "Living Room TV Unit", "Full Home Interior") else projectTypes,
                    modifier = Modifier.fillMaxWidth()
                )

                com.example.ui.components.PremiumDropdown(
                    value = currentMaterial.ifEmpty { "Select Material Type" },
                    onValueChange = onSelectMaterial,
                    label = "Material Type *",
                    options = if (materials.isEmpty()) listOf("BWP Plywood", "MDF (Exterior Grade)", "HDF", "Particle Board", "Aluminium Section Framework") else materials,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- STEP 3: QUOTATION ITEMS LIST & DIALOGS ---
@Composable
fun WizardStepItems(
    quotationViewModel: com.example.ui.quotation.QuotationViewModel,
    quoteItems: List<QuotationItem>,
    currentMaterial: String,
    currentFinish: String,
    currentProjectType: String
) {
    var showItemConfigDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showSuggestionsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toolbar actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    editingItemIndex = null
                    showItemConfigDialog = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Item", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { showSuggestionsDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Suggestions", fontSize = 13.sp)
            }
        }
        if (quoteItems.isEmpty()) {
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
                                imageVector = Icons.Filled.LibraryAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Your quotation is empty.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Click 'Add Item' or load suggestions to begin.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quoteItems.size, key = { quoteItems[it].id }) { index ->
                    val item = quoteItems[index]
                    QuotationItemCard(
                        item = item,
                        index = index,
                        modifier = Modifier.animateItem(),
                        onEdit = {
                            editingItemIndex = index
                            showItemConfigDialog = true
                        },
                        onDelete = { quotationViewModel.removeQuoteItem(index) }
                    )
                }
            }
        }
    }

    if (showItemConfigDialog) {
        val masterDataVal = quotationViewModel.allMasterData.collectAsState().value
        val finishes = masterDataVal
            .filter { it.masterType == "FINISH_TYPE" }
            .map { it.name }
        ItemConfigDialog(
            itemIndex = editingItemIndex,
            currentItems = quoteItems,
            onDismiss = { showItemConfigDialog = false },
            onSave = { updatedOrNewItem ->
                if (editingItemIndex == null) {
                    quotationViewModel.addQuoteItem(updatedOrNewItem)
                } else {
                    quotationViewModel.updateQuoteItem(editingItemIndex!!, updatedOrNewItem)
                }
                showItemConfigDialog = false
            },
            finishes = finishes,
            allMasterData = masterDataVal,
            projectType = currentProjectType,
            category = currentFinish
        )
    }

    if (showSuggestionsDialog) {
        val masterDataVal = quotationViewModel.allMasterData.collectAsState().value
        SmartSuggestionsDialog(
            onDismiss = { showSuggestionsDialog = false },
            onSelectSuggestion = { sug ->
                quotationViewModel.addQuoteItem(sug)
                showSuggestionsDialog = false
            },
            projectType = currentProjectType,
            masterData = masterDataVal
        )
    }
}

"""
    # Splice content
    updated_content = content[:start_idx] + new_wizard_steps_code + content[end_idx:]
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(updated_content)
    print("[SUCCESS] Replaced wizard steps cleanly.")
else:
    print(f"[FAILED] Could not locate start_marker or end_marker. start_idx: {start_idx}, end_idx: {end_idx}")
