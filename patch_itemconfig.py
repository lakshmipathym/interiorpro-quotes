import re

file_path = "/app/applet/app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# I need to completely replace fun ItemConfigDialog
pattern = re.compile(r"fun ItemConfigDialog\(.*?^\}", re.MULTILINE | re.DOTALL)
match = pattern.search(content)

if not match:
    print("Could not find ItemConfigDialog")
    exit(1)

new_dialog = """fun ItemConfigDialog(
    itemIndex: Int?,
    currentItems: List<QuotationItem>,
    onDismiss: () -> Unit,
    onSave: (QuotationItem) -> Unit
) {
    val context = LocalContext.current

    // Properties
    var itemName by remember { mutableStateOf("") }
    var userDescription by remember { mutableStateOf("") }
    
    // Material selection
    var material by remember { mutableStateOf("Plywood") }
    
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
            copied?.let { designPath = it }
        }
    }

    LaunchedEffect(itemIndex) {
        if (itemIndex != null && itemIndex in currentItems.indices) {
            val existing = currentItems[itemIndex]
            itemName = existing.itemName
            unit = existing.unit
            quantityStr = existing.quantity.toString()
            rateStr = existing.rate.toString()
            designPath = existing.referenceImagePath ?: ""
            userDescription = existing.userDescription ?: ""

            try {
                val specs = JSONObject(existing.specificationsJson)
                material = specs.optString("Material", "Plywood")
                widthStr = specs.optString("Width", "")
                heightStr = specs.optString("Height", "")
                depthStr = specs.optString("Depth", "")
                thickness = specs.optString("Thickness", "")
                grade = specs.optString("Grade", "")
                brand = specs.optString("Brand", "")
                profileSeries = specs.optString("Profile Series", "")
                profileColour = specs.optString("Profile Colour", "")
                glassType = specs.optString("Glass Type", "")
                glassThickness = specs.optString("Glass Thickness", "")
                acpColour = specs.optString("ACP Colour", "")
                hardware = specs.optString("Hardware", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    com.example.ui.components.PremiumDialog(
        onDismissRequest = onDismiss,
        title = if (itemIndex == null) "Configure Item" else "Edit Item",
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -- Core Details Section --
            Text("Core Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            com.example.ui.components.PremiumTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = "Item Name *"
            )
            com.example.ui.components.PremiumTextField(
                value = userDescription,
                onValueChange = { userDescription = it },
                label = "Description (Optional)",
                singleLine = false,
                minLines = 2
            )

            HorizontalDivider()

            // -- Material Specification Section --
            Text("Specifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            com.example.ui.components.PremiumDropdown(
                label = "Material Type *",
                value = material,
                options = listOf("Plywood", "Particle Board", "MDF", "HDHMR", "WPC", "Aluminium", "ACP", "Glass", "PVC Board", "Hardware"),
                onValueChange = { m ->
                    material = m
                    thickness = ""; grade = ""; profileSeries = ""; profileColour = "";
                    glassType = ""; glassThickness = ""; acpColour = ""; brand = ""; hardware = ""
                }
            )

            // Dynamic fields
            val mLower = material.lowercase(Locale.US)
            if (mLower.contains("plywood") || mLower.contains("hdhmr") || mLower.contains("mdf") || mLower.contains("wpc") || mLower.contains("pvc")) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumDropdown(label = "Thickness *", value = thickness, options = listOf("6 mm", "8 mm", "12 mm", "16 mm", "18 mm", "25 mm"), onValueChange = { thickness = it }, modifier = Modifier.weight(1f))
                    if (mLower.contains("plywood") || mLower.contains("particle")) {
                        com.example.ui.components.PremiumDropdown(label = "Grade", value = grade, options = listOf("MR", "BWR", "BWP", "Marine", "Standard", "Premium"), onValueChange = { grade = it }, modifier = Modifier.weight(1f))
                    }
                }
            } else if (mLower.contains("aluminium")) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumDropdown(label = "Profile Series", value = profileSeries, options = listOf("18x40 Series", "20x45 Series", "45x45 Series", "Slim Line", "Heavy Duty"), onValueChange = { profileSeries = it }, modifier = Modifier.weight(1f))
                    com.example.ui.components.PremiumDropdown(label = "Profile Colour", value = profileColour, options = listOf("Anodized Silver", "Champagne Gold", "Rose Gold", "Charcoal Grey", "Matt Black"), onValueChange = { profileColour = it }, modifier = Modifier.weight(1f))
                }
            } else if (mLower.contains("glass")) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumDropdown(label = "Type", value = glassType, options = listOf("Clear", "Frosted", "Tinted", "Fluted", "Lacquered"), onValueChange = { glassType = it }, modifier = Modifier.weight(1f))
                    com.example.ui.components.PremiumDropdown(label = "Thickness", value = glassThickness, options = listOf("4 mm", "5 mm", "6 mm", "8 mm", "10 mm", "12 mm"), onValueChange = { glassThickness = it }, modifier = Modifier.weight(1f))
                }
            }
            
            com.example.ui.components.PremiumTextField(
                value = brand,
                onValueChange = { brand = it },
                label = "Brand / Make (Optional)"
            )
            com.example.ui.components.PremiumTextField(
                value = hardware,
                onValueChange = { hardware = it },
                label = "Hardware Fittings (Optional)"
            )

            HorizontalDivider()

            // -- Dimensions Section --
            Text("Dimensions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.example.ui.components.PremiumTextField(value = widthStr, onValueChange = { widthStr = it }, label = "Width", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                com.example.ui.components.PremiumTextField(value = heightStr, onValueChange = { heightStr = it }, label = "Height", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                com.example.ui.components.PremiumTextField(value = depthStr, onValueChange = { depthStr = it }, label = "Depth", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }

            HorizontalDivider()

            // -- Pricing Section --
            Text("Pricing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.example.ui.components.PremiumDropdown(label = "Unit *", value = unit, options = listOf("Sq.Ft", "R.Ft", "Sqm", "Nos", "Lumpsum", "Set"), onValueChange = { unit = it }, modifier = Modifier.weight(1f))
                com.example.ui.components.PremiumTextField(value = quantityStr, onValueChange = { quantityStr = it }, label = "Qty *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            com.example.ui.components.PremiumTextField(value = rateStr, onValueChange = { rateStr = it }, label = "Rate *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            
            // Image Attachment
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(if (designPath.isNotEmpty()) "Image Attached" else "No Image Attached", color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.example.ui.components.PremiumSecondaryButton(
                    text = if (designPath.isNotEmpty()) "Change Image" else "Attach Image",
                    onClick = { designLauncher.launch("image/*") },
                    modifier = Modifier.width(150.dp)
                )
            }
        }
    } actions = {
        com.example.ui.components.PremiumTextButton(text = "Cancel", onClick = onDismiss)
        Spacer(modifier = Modifier.width(8.dp))
        com.example.ui.components.PremiumPrimaryButton(
            text = "Save Item",
            onClick = {
                if (itemName.isBlank() || quantityStr.toDoubleOrNull() == null || rateStr.toDoubleOrNull() == null) {
                    Toast.makeText(context, "Fill required fields correctly", Toast.LENGTH_SHORT).show()
                    return@PremiumPrimaryButton
                }
                val specJson = JSONObject().apply {
                    put("Material", material)
                    if (thickness.isNotBlank()) put("Thickness", thickness)
                    if (grade.isNotBlank()) put("Grade", grade)
                    if (profileSeries.isNotBlank()) put("Profile Series", profileSeries)
                    if (profileColour.isNotBlank()) put("Profile Colour", profileColour)
                    if (glassType.isNotBlank()) put("Glass Type", glassType)
                    if (glassThickness.isNotBlank()) put("Glass Thickness", glassThickness)
                    if (acpColour.isNotBlank()) put("ACP Colour", acpColour)
                    if (brand.isNotBlank()) put("Brand", brand)
                    if (hardware.isNotBlank()) put("Hardware", hardware)
                    if (widthStr.isNotBlank()) put("Width", widthStr)
                    if (heightStr.isNotBlank()) put("Height", heightStr)
                    if (depthStr.isNotBlank()) put("Depth", depthStr)
                }

                val item = QuotationItem(
                    id = if (itemIndex != null) currentItems[itemIndex].id else 0,
                    quotationId = 0,
                    itemName = itemName.trim(),
                    specificationsJson = specJson.toString(),
                    unit = unit,
                    quantity = quantityStr.toDoubleOrNull() ?: 1.0,
                    rate = rateStr.toDoubleOrNull() ?: 0.0,
                    referenceImagePath = designPath.ifEmpty { null },
                    userDescription = userDescription.ifBlank { null }
                )
                onSave(item)
            }
        )
    }
}"""

content = content[:match.start()] + new_dialog + content[match.end():]

with open(file_path, "w") as f:
    f.write(content)

