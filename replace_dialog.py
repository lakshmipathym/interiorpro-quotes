import re

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'r') as f:
    content = f.read()

# I will find the bounds of fun ItemConfigDialog(
start_index = content.find("fun ItemConfigDialog(")
end_index = content.find("// --- STEP 4: TAXES, WARRANTIES & TERMS ---")

# Replace this chunk completely
new_dialog = '''fun ItemConfigDialog(
    itemIndex: Int?,
    currentItems: List<QuotationItem>,
    onDismiss: () -> Unit,
    onSave: (QuotationItem) -> Unit,
    finishes: List<String> = emptyList()
) {
    val context = LocalContext.current

    // Properties
    var itemName by remember { mutableStateOf("") }
    var userDescription by remember { mutableStateOf("") }

    // Material selection
    var material by remember { mutableStateOf("Plywood") }
    var finish by remember { mutableStateOf("") }

    // Material specific properties
    var profileSeries by remember { mutableStateOf("") }
    var profileColour by remember { mutableStateOf("") }
    var glassType by remember { mutableStateOf("") }
    var glassThickness by remember { mutableStateOf("") }
    var acpColour by remember { mutableStateOf("") }
    var thickness by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var hardware by remember { mutableStateOf("") }
    var cncDesign by remember { mutableStateOf("") }

    // Common properties
    var widthStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var depthStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Sq.Ft") }
    var quantityStr by remember { mutableStateOf("1.0") }
    var rateStr by remember { mutableStateOf("") }

    var designPath by remember { mutableStateOf("") }

    val designLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val copied = copyUriToInternalStorage(context, it, "temp_des_${System.currentTimeMillis()}.jpg")
            if (copied != null) {
                if (designPath.isNotEmpty() && designPath != copied) {
                    val oldFile = java.io.File(designPath)
                    if (oldFile.exists() && oldFile.name.startsWith("temp_des_")) {
                        oldFile.delete()
                    }
                }
                designPath = copied
            }
        }
    }

    LaunchedEffect(itemIndex) {
        if (itemIndex != null && itemIndex in currentItems.indices) {
            val existing = currentItems[itemIndex]
            itemName = existing.itemName
            unit = existing.unit
            rateStr = existing.rate.toString()

            val (desc, specs) = parseItemSpecs(existing.description)
            userDescription = desc
            material = existing.material
            finish = specs.finish.ifBlank { existing.finish }

            profileSeries = specs.profileSeries
            profileColour = specs.profileColour
            glassType = specs.glassType
            glassThickness = specs.glassThickness
            acpColour = specs.acpColour
            thickness = specs.thickness
            grade = specs.grade
            cncDesign = specs.cncDesign
            brand = specs.brand
            hardware = specs.hardware
            widthStr = specs.width
            heightStr = specs.height
            depthStr = specs.depth
            designPath = specs.designImageUri
            quantityStr = if (existing.quantity % 1.0 == 0.0) existing.quantity.toInt().toString() else existing.quantity.toString()
        }
    }

    // Calculations
    val wFeet = com.example.engine.QuotationCalculationEngine.parseDimensionToFeet(widthStr)
    val hFeet = com.example.engine.QuotationCalculationEngine.parseDimensionToFeet(heightStr)
    val dFeet = com.example.engine.QuotationCalculationEngine.parseDimensionToFeet(depthStr)
    
    val areaSqFt = wFeet * hFeet
    val runFeet = wFeet
    val volCubicFt = wFeet * hFeet * dFeet
    
    val qtyVal = quantityStr.toDoubleOrNull() ?: 0.0
    val rateVal = rateStr.toDoubleOrNull() ?: 0.0
    val uLower = unit.trim().lowercase(Locale.US)
    
    val calculatedAmount = com.example.engine.QuotationCalculationEngine.calculateQuantity(widthStr, heightStr, qtyVal, unit) * rateVal
    val isAreaBased = uLower.contains("sq") || uLower.contains("sft")
    val isRftBased = uLower.contains("rft") || uLower.contains("run") || uLower.contains("meter")

    com.example.ui.components.PremiumDialog(
        onDismissRequest = onDismiss,
        title = if (itemIndex == null) "Configure Item" else "Edit Item",
        modifier = Modifier.fillMaxWidth(),
        actions = {
            com.example.ui.components.PremiumTextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
            Spacer(modifier = Modifier.width(8.dp))
            com.example.ui.components.PremiumPrimaryButton(
                onClick = {
                    if (itemName.isBlank()) {
                        Toast.makeText(context, "Item Name is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (qtyVal < 0 || rateVal < 0) {
                        Toast.makeText(context, "Values cannot be negative", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (qtyVal == 0.0) {
                        Toast.makeText(context, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }

                    val specs = ItemSpecs(
                        width = widthStr.trim(),
                        height = heightStr.trim(),
                        depth = depthStr.trim(),
                        doorType = "",
                        finish = finish.trim(),
                        hardware = hardware.trim(),
                        brand = brand.trim(),
                        thickness = thickness.trim(),
                        colour = "",
                        laminateImageUri = "",
                        designImageUri = designPath,
                        profileSeries = profileSeries.trim(),
                        profileColour = profileColour.trim(),
                        glassType = glassType.trim(),
                        glassThickness = glassThickness.trim(),
                        acpColour = acpColour.trim(),
                        grade = grade.trim(),
                        cncDesign = cncDesign.trim()
                    )

                    val finalDesc = serializeItemSpecs(userDescription.trim(), specs)
                    val item = QuotationItem(
                        id = if (itemIndex != null) currentItems[itemIndex].id else 0,
                        quotationId = 0,
                        itemName = itemName.trim(),
                        description = finalDesc,
                        material = material,
                        finish = finish.trim(),
                        quantity = qtyVal,
                        unit = unit,
                        rate = rateVal,
                        amount = calculatedAmount
                    )
                    onSave(item)
                }
            ) { Text("Save Item", fontWeight = FontWeight.Bold) }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -- 1. Basic Information --
            Text("Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            com.example.ui.components.PremiumOutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = "Item Name *"
            )

            HorizontalDivider()

            // -- 2. Material & Finish --
            Text("Material & Finish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            com.example.ui.components.PremiumDropdown(
                label = "Material Type *",
                value = material,
                options = listOf("Plywood", "Particle Board", "MDF", "HDHMR", "WPC", "Aluminium", "ACP", "Glass", "PVC Board", "Hardware"),
                onValueChange = { m ->
                    material = m
                    thickness = ""; grade = ""; profileSeries = ""; profileColour = "";
                    glassType = ""; glassThickness = ""; acpColour = ""; brand = ""; hardware = ""; cncDesign = "";
                }
            )

            val mLower = material.lowercase(Locale.US)
            
            if (mLower.contains("plywood") || mLower.contains("particle")) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumDropdown(label = "Grade", value = grade, options = listOf("MR", "BWR", "BWP", "Marine", "Standard", "Premium"), onValueChange = { grade = it }, modifier = Modifier.weight(1f))
                    val availableFinishes = finishes.ifEmpty { listOf("High Gloss Laminate", "Matte Laminate", "Acrylic Finish", "PU Paint", "Powder Coated", "Anodized") }
                    com.example.ui.components.PremiumDropdown(label = "Finish Type *", value = finish, options = availableFinishes, onValueChange = { finish = it }, modifier = Modifier.weight(1f))
                }
                com.example.ui.components.PremiumOutlinedTextField(
                    value = hardware,
                    onValueChange = { hardware = it },
                    label = "Recommended Hardware"
                )
            } else if (mLower.contains("mdf")) {
                val availableFinishes = finishes.ifEmpty { listOf("High Gloss Laminate", "Matte Laminate", "Acrylic Finish", "PU Paint", "Powder Coated", "Anodized") }
                com.example.ui.components.PremiumDropdown(label = "Finish Type *", value = finish, options = availableFinishes, onValueChange = { finish = it })
                com.example.ui.components.PremiumDropdown(label = "CNC Options", value = cncDesign, options = listOf("None", "Simple Groove", "Complex Pattern", "Jali Design"), onValueChange = { cncDesign = it })
            } else if (mLower.contains("aluminium")) {
                val availableFinishes = finishes.ifEmpty { listOf("High Gloss Laminate", "Matte Laminate", "Acrylic Finish", "PU Paint", "Powder Coated", "Anodized") }
                com.example.ui.components.PremiumDropdown(label = "Finish Type *", value = finish, options = availableFinishes, onValueChange = { finish = it })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumDropdown(label = "Profile Type", value = profileSeries, options = listOf("18x40 Series", "20x45 Series", "45x45 Series", "Slim Line", "Heavy Duty"), onValueChange = { profileSeries = it }, modifier = Modifier.weight(1f))
                    com.example.ui.components.PremiumDropdown(label = "Profile Colour", value = profileColour, options = listOf("Anodized Silver", "Champagne Gold", "Rose Gold", "Charcoal Grey", "Matt Black"), onValueChange = { profileColour = it }, modifier = Modifier.weight(1f))
                }
            } else if (mLower.contains("glass")) {
                com.example.ui.components.PremiumDropdown(label = "Glass Type", value = glassType, options = listOf("Clear", "Frosted", "Tinted", "Fluted", "Lacquered"), onValueChange = { glassType = it })
            } else {
                // Fallback for others
                val availableFinishes = finishes.ifEmpty { listOf("High Gloss Laminate", "Matte Laminate", "Acrylic Finish", "PU Paint", "Powder Coated", "Anodized") }
                com.example.ui.components.PremiumDropdown(label = "Finish Type *", value = finish, options = availableFinishes, onValueChange = { finish = it })
            }

            HorizontalDivider()

            // -- 3. Dimensions --
            Text("Dimensions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.example.ui.components.PremiumOutlinedTextField(value = widthStr, onValueChange = { widthStr = it }, label = "Width", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), modifier = Modifier.weight(1f))
                com.example.ui.components.PremiumOutlinedTextField(value = heightStr, onValueChange = { heightStr = it }, label = "Height", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), modifier = Modifier.weight(1f))
                com.example.ui.components.PremiumOutlinedTextField(value = depthStr, onValueChange = { depthStr = it }, label = "Depth", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), modifier = Modifier.weight(1f))
            }

            // -- 4. Calculation Preview --
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Live Dimension Preview", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Area: ${com.example.utils.CurrencyFormatter.formatIndianCurrency(areaSqFt)} Sq.Ft", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("R.Ft: ${com.example.utils.CurrencyFormatter.formatIndianCurrency(runFeet)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    if (dFeet > 0.0) {
                        Text("Volume: ${com.example.utils.CurrencyFormatter.formatIndianCurrency(volCubicFt)} Cu.Ft", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            HorizontalDivider()

            // -- 5. Pricing --
            Text("Pricing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.example.ui.components.PremiumDropdown(label = "Unit *", value = unit, options = listOf("Sq.Ft", "R.Ft", "Sqm", "Nos", "Lumpsum", "Set"), onValueChange = { unit = it }, modifier = Modifier.weight(1f))
                com.example.ui.components.PremiumOutlinedTextField(value = quantityStr, onValueChange = { quantityStr = it }, label = "Qty *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                com.example.ui.components.PremiumOutlinedTextField(value = rateStr, onValueChange = { rateStr = it }, label = "Rate *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }

            // Live Price Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estimated Amount", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val calcString = when {
                        isAreaBased -> "${com.example.utils.CurrencyFormatter.formatIndianCurrency(areaSqFt)} (Area) × $qtyVal (Qty) × ₹$rateVal (Rate)"
                        isRftBased -> "${com.example.utils.CurrencyFormatter.formatIndianCurrency(runFeet)} (R.Ft) × $qtyVal (Qty) × ₹$rateVal (Rate)"
                        else -> "$qtyVal (Qty) × ₹$rateVal (Rate)"
                    }
                    
                    Text(calcString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(calculatedAmount)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            HorizontalDivider()

            // -- 6. Reference Image --
            Text("Reference Image", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            if (designPath.isNotEmpty() && java.io.File(designPath).exists()) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(java.io.File(designPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Design Preview",
                    modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(if (designPath.isNotEmpty()) "Image Attached" else "No Image Attached", color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.example.ui.components.PremiumSecondaryButton(
                    onClick = { designLauncher.launch("image/*") },
                    modifier = Modifier.width(150.dp)
                ) { Text(if (designPath.isNotEmpty()) "Change Image", fontWeight = FontWeight.SemiBold) else Text("Attach Image", fontWeight = FontWeight.SemiBold) }
            }

            HorizontalDivider()

            // -- 7. Notes --
            Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            com.example.ui.components.PremiumOutlinedTextField(
                value = userDescription,
                onValueChange = { userDescription = it },
                label = "Description / Additional Notes (Optional)",
                singleLine = false,
                minLines = 3
            )
        }
    }
}
'''

content = content[:start_index] + new_dialog + content[end_index:]

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'w') as f:
    f.write(content)
