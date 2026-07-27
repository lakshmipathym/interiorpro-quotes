import re

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'r') as f:
    content = f.read()

# We want to replace everything from "// --- STEP 1: CUSTOMER SELECTION" to just before "// --- STEP 3: QUOTATION ITEMS LIST"
start_marker = "// --- STEP 1: CUSTOMER SELECTION & QUOTATION ITEMS ---"
end_marker = "// --- STEP 3: QUOTATION ITEMS LIST & DIALOGS ---"

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

if start_idx == -1 or end_idx == -1:
    print("Markers not found")
    exit(1)

new_code = """// --- STEP 1: PROJECT DETAILS ---
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WizardStepDetails(
    customerViewModel: com.example.ui.customer.CustomerViewModel,
    quotationViewModel: com.example.ui.quotation.QuotationViewModel,
    onOpenQuickAdd: () -> Unit
) {
    val customers by customerViewModel.customers.collectAsState()
    val searchQuery by customerViewModel.searchQuery.collectAsState()
    val selectedCustomer by quotationViewModel.newQuoteCustomer.collectAsState()

    val siteName by quotationViewModel.newQuoteSiteName.collectAsState()
    val siteAddress by quotationViewModel.newQuoteSiteAddress.collectAsState()
    val projectName by quotationViewModel.newQuoteProjectName.collectAsState()
    val dateMillis by quotationViewModel.newQuoteDate.collectAsState()
    val validityDays by quotationViewModel.newQuoteValidityDays.collectAsState()
    
    val currentProjectType by quotationViewModel.newQuoteProjectType.collectAsState()
    val currentCategory by quotationViewModel.newQuoteCategory.collectAsState()
    val currentMaterial by quotationViewModel.newQuoteMaterial.collectAsState()
    val currentFinish by quotationViewModel.newQuoteFinish.collectAsState()
    
    val allMasterData by quotationViewModel.allMasterData.collectAsState()
    
    val projectTypes = allMasterData.filter { it.masterType == "PROJECT_TYPE" }.map { it.name }.ifEmpty { listOf("Modular Kitchen", "Wardrobe", "Living Room TV Unit", "Full Home Interior") }
    val categories = allMasterData.filter { it.masterType == "PROJECT_CATEGORY" }.map { it.name }.ifEmpty { listOf("Premium", "Standard", "Economy") }
    val materials = allMasterData.filter { it.masterType == "MATERIAL_TYPE" }.map { it.name }.ifEmpty { listOf("BWP Plywood", "MDF (Exterior Grade)", "HDF", "Particle Board", "Aluminium Section Framework") }
    val finishes = allMasterData.filter { it.masterType == "FINISH_TYPE" }.map { it.name }.ifEmpty { listOf("Laminate", "Acrylic", "PU Paint", "Veneer", "Glass") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- CUSTOMER SELECTION ---
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

                    val displayList = customers.take(4)
                    if (displayList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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
                                        .clickable { quotationViewModel.selectCustomer(cust) },
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = cust.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(text = "Phone: ${cust.mobileNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "Select", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = selectedCustomer!!.customerName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Phone: ${selectedCustomer!!.mobileNumber}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(
                        onClick = { quotationViewModel.startNewQuotation() }, // Needs to clear customer really, but startNewQuotation works for now
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Change", fontSize = 11.sp)
                    }
                }
            }

            // --- SITE & PROJECT DETAILS ---
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Project & Site Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { quotationViewModel.updateProjectName(it) },
                        label = { Text("Project Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(dateMillis))
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { },
                        label = { Text("Quotation Date") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = validityDays.toString(),
                        onValueChange = { quotationViewModel.updateValidityDays(it.toIntOrNull() ?: 30) },
                        label = { Text("Validity (Days)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = siteName,
                        onValueChange = { quotationViewModel.updateSiteDetails(it, siteAddress) },
                        label = { Text("Site Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = siteAddress,
                        onValueChange = { quotationViewModel.updateSiteDetails(siteName, it) },
                        label = { Text("Site Address *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
                }
            }

            // --- SCOPE & MATERIAL ---
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = "Material & Scope", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    com.example.ui.components.PremiumDropdown(
                        value = currentProjectType.ifEmpty { "Select Product Type" },
                        onValueChange = { quotationViewModel.selectProjectType(it) },
                        label = "Product Type *",
                        options = projectTypes,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    com.example.ui.components.PremiumDropdown(
                        value = currentCategory.ifEmpty { "Select Category" },
                        onValueChange = { quotationViewModel.selectCategory(it) },
                        label = "Project Category *",
                        options = categories,
                        modifier = Modifier.fillMaxWidth()
                    )

                    com.example.ui.components.PremiumDropdown(
                        value = currentMaterial.ifEmpty { "Select Main Material" },
                        onValueChange = { quotationViewModel.selectMaterial(it) },
                        label = "Main Material *",
                        options = materials,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    com.example.ui.components.PremiumDropdown(
                        value = currentFinish.ifEmpty { "Select Main Finish" },
                        onValueChange = { quotationViewModel.selectFinish(it) },
                        label = "Main Finish *",
                        options = finishes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
"""

new_file_content = content[:start_idx] + new_code + "\n" + content[end_idx:]

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'w') as f:
    f.write(new_file_content)
print("Updated successfully")
