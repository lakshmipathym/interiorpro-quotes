import re

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

replacements = []

# --- Fix 1: showDiscardDialog block (around line 372) ---
old_discard = """    if (showDiscardDialog) {
        com.example.ui.components.PremiumDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = "Discard Quotation?",
            actions = {
                com.example.ui.components.PremiumTextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continue Editing")
                Spacer(modifier = Modifier.width(8.dp))
                com.example.ui.components.PremiumPrimaryButton(
                    onClick = {
                        showDiscardDialog = false
                        quotationViewModel.startNewQuotation()
                        onSuccessReturn()
                }
                ) {
                    Text("Discard")
                }
            }
        }
        ) {
            Text("Are you sure you want to exit the quotation wizard? All progress on this quotation will be discarded.")

        }
    }"""

new_discard = """    if (showDiscardDialog) {
        com.example.ui.components.PremiumDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = "Discard Quotation?",
            actions = {
                com.example.ui.components.PremiumTextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continue Editing")
                }
                Spacer(modifier = Modifier.width(8.dp))
                com.example.ui.components.PremiumPrimaryButton(
                    onClick = {
                        showDiscardDialog = false
                        quotationViewModel.startNewQuotation()
                        onSuccessReturn()
                    }
                ) {
                    Text("Discard")
                }
            }
        ) {
            Text("Are you sure you want to exit the quotation wizard? All progress on this quotation will be discarded.")
        }
    }"""
replacements.append(("Discard Dialog", old_discard, new_discard))


# --- Fix 2: WizardStepReview block and Box matching (lines 495 to 619) ---
old_wizard_review = """        // Central Switch Content
        Box(modifier = Modifier.weight(1f)) {
            when (activeStep) {
                    1 -> WizardStepCustomer(
                        customerViewModel = customerViewModel,
                        quotationViewModel = quotationViewModel,
                        quoteItems = quoteItems,
                        onOpenQuickAdd = { isQuickAddCustomerOpen = true }
                    )
                    
                    else -> WizardStepReview(
                        quoteNumber = quoteNumber,
                        customerName = currentCustomer?.customerName ?: "Unknown",
                        customerPhone = currentCustomer?.mobileNumber ?: "",
                        customerAddress = currentCustomer?.address ?: "",
                        siteLocation = currentCustomer?.siteLocation ?: "",
                        itemsCount = quoteItems.size,
                        subtotal = subtotal,
                        discount = discount,
                        gstRate = gstRate,
                        gstAmount = gstAmount,
                        grandTotal = grandTotal,
                        terms = termsAndConditions,
                        warranty = warranty,
                        onDiscountChange = { quotationViewModel.setDiscount(it) },
                        onGstRateChange = { quotationViewModel.setGstRate(it) },
                        onTermsChange = { quotationViewModel.setTerms(it) },
                        onWarrantyChange = { quotationViewModel.setWarranty(it) },
                        onSave = {
                            // Consolidate spec summary string from items
                            val specSummary = buildString {
                                quoteItems.forEachIndexed { idx, item ->
                                    val (userDesc, specs) = parseItemSpecs(item.description)
                                    appendLine("Item ${idx + 1} [${item.itemName}]:")
                                    appendLine("  Material: ${item.material}")
                                    when (item.material) {
                                        "Plywood" -> {
                                            if (specs.thickness.isNotBlank()) appendLine("  Thickness: ${specs.thickness}")
                                            if (specs.grade.isNotBlank()) appendLine("  Grade: ${specs.grade}")
                                        }
                                        "Aluminium" -> {
                                            if (specs.profileSeries.isNotBlank()) appendLine("  Profile Series: ${specs.profileSeries}")
                                            if (specs.profileColour.isNotBlank()) appendLine("  Profile Colour: ${specs.profileColour}")
                                            if (specs.glassType.isNotBlank()) appendLine("  Glass Type: ${specs.glassType}")
                                        }
                                        "Glass" -> {
                                            if (specs.glassType.isNotBlank()) appendLine("  Glass Type: ${specs.glassType}")
                                            if (specs.glassThickness.isNotBlank()) appendLine("  Glass Thickness: ${specs.glassThickness}")
                                        }
                                        "ACP" -> {
                                            if (specs.acpColour.isNotBlank()) appendLine("  ACP Colour: ${specs.acpColour}")
                                        }
                                        "WPC" -> {
                                            if (specs.thickness.isNotBlank()) appendLine("  Thickness: ${specs.thickness}")
                                        }
                                    }
                                    if (specs.width.isNotBlank() || specs.height.isNotBlank() || specs.depth.isNotBlank()) {
                                        val dimStr = buildString {
                                            if (specs.width.isNotBlank()) append("W:${specs.width}ft")
                                            if (specs.height.isNotBlank()) {
                                                if (length > 0) append(" x ")
                                                append("H:${specs.height}ft")
                                            }
                                            if (specs.depth.isNotBlank()) {
                                                if (length > 0) append(" x ")
                                                append("D:${specs.depth}ft")
                                            }
                                        }
                                        appendLine("  Dimensions: $dimStr")
                                    }
                                    appendLine("  Quantity: ${item.quantity} ${item.unit}")
                                    appendLine()
                                }
                            }
                            quotationViewModel.selectFinish(specSummary)

                            // Copy ALL reference design images
                            quoteItems.forEachIndexed { index, item ->
                                val (desc, specs) = parseItemSpecs(item.description)
                                var modified = false
                                var updatedSpecs = specs
                                val safeQuoteNum = quoteNumber.replace("/", "_")

                                if (specs.designImageUri.isNotBlank() && specs.designImageUri.contains("temp_des_")) {
                                    val file = File(context.filesDir, File(specs.designImageUri).name)
                                    if (file.exists()) {
                                        try {
                                            val destFile = File(context.filesDir, "design_${safeQuoteNum}_${index}.jpg")
                                            file.copyTo(destFile, overwrite = true)
                                            file.delete()
                                            updatedSpecs = updatedSpecs.copy(designImageUri = destFile.absolutePath)
                                            modified = true
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                                if (specs.laminateImageUri.isNotBlank() && specs.laminateImageUri.contains("temp_lam_")) {
                                    val file = File(context.filesDir, File(specs.laminateImageUri).name)
                                    if (file.exists()) {
                                        try {
                                            val destFile = File(context.filesDir, "laminate_${safeQuoteNum}_${index}.jpg")
                                            file.copyTo(destFile, overwrite = true)
                                            file.delete()
                                            updatedSpecs = updatedSpecs.copy(laminateImageUri = destFile.absolutePath)
                                            modified = true
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                                if (modified) {
                                    val newDesc = serializeItemSpecs(desc, updatedSpecs)
                                    quotationViewModel.updateQuoteItem(index, item.copy(description = newDesc))
                                }
                            }
                            quotationViewModel.saveQuotation { id ->
                                savedQuotationId = id
                            }
                        }
                    }
                    )

            // Persistent bottom navigation footer
                }
            }"""

new_wizard_review = """        // Central Switch Content
        Box(modifier = Modifier.weight(1f)) {
            when (activeStep) {
                1 -> WizardStepCustomer(
                    customerViewModel = customerViewModel,
                    quotationViewModel = quotationViewModel,
                    quoteItems = quoteItems,
                    onOpenQuickAdd = { isQuickAddCustomerOpen = true }
                )
                
                else -> WizardStepReview(
                    quoteNumber = quoteNumber,
                    customerName = currentCustomer?.customerName ?: "Unknown",
                    customerPhone = currentCustomer?.mobileNumber ?: "",
                    customerAddress = currentCustomer?.address ?: "",
                    siteLocation = currentCustomer?.siteLocation ?: "",
                    itemsCount = quoteItems.size,
                    subtotal = subtotal,
                    discount = discount,
                    gstRate = gstRate,
                    gstAmount = gstAmount,
                    grandTotal = grandTotal,
                    terms = termsAndConditions,
                    warranty = warranty,
                    onDiscountChange = { quotationViewModel.setDiscount(it) },
                    onGstRateChange = { quotationViewModel.setGstRate(it) },
                    onTermsChange = { quotationViewModel.setTerms(it) },
                    onWarrantyChange = { quotationViewModel.setWarranty(it) },
                    onSave = {
                        // Consolidate spec summary string from items
                        val specSummary = buildString {
                            quoteItems.forEachIndexed { idx, item ->
                                val (userDesc, specs) = parseItemSpecs(item.description)
                                appendLine("Item ${idx + 1} [${item.itemName}]:")
                                appendLine("  Material: ${item.material}")
                                when (item.material) {
                                    "Plywood" -> {
                                        if (specs.thickness.isNotBlank()) appendLine("  Thickness: ${specs.thickness}")
                                        if (specs.grade.isNotBlank()) appendLine("  Grade: ${specs.grade}")
                                    }
                                    "Aluminium" -> {
                                        if (specs.profileSeries.isNotBlank()) appendLine("  Profile Series: ${specs.profileSeries}")
                                        if (specs.profileColour.isNotBlank()) appendLine("  Profile Colour: ${specs.profileColour}")
                                        if (specs.glassType.isNotBlank()) appendLine("  Glass Type: ${specs.glassType}")
                                    }
                                    "Glass" -> {
                                        if (specs.glassType.isNotBlank()) appendLine("  Glass Type: ${specs.glassType}")
                                        if (specs.glassThickness.isNotBlank()) appendLine("  Glass Thickness: ${specs.glassThickness}")
                                    }
                                    "ACP" -> {
                                        if (specs.acpColour.isNotBlank()) appendLine("  ACP Colour: ${specs.acpColour}")
                                    }
                                    "WPC" -> {
                                        if (specs.thickness.isNotBlank()) appendLine("  Thickness: ${specs.thickness}")
                                    }
                                }
                                if (specs.width.isNotBlank() || specs.height.isNotBlank() || specs.depth.isNotBlank()) {
                                    val dimStr = buildString {
                                        if (specs.width.isNotBlank()) append("W:${specs.width}ft")
                                        if (specs.height.isNotBlank()) {
                                            if (length > 0) append(" x ")
                                            append("H:${specs.height}ft")
                                        }
                                        if (specs.depth.isNotBlank()) {
                                            if (length > 0) append(" x ")
                                            append("D:${specs.depth}ft")
                                        }
                                    }
                                    appendLine("  Dimensions: $dimStr")
                                }
                                appendLine("  Quantity: ${item.quantity} ${item.unit}")
                                appendLine()
                            }
                        }
                        quotationViewModel.selectFinish(specSummary)

                        // Copy ALL reference design images
                        quoteItems.forEachIndexed { index, item ->
                            val (desc, specs) = parseItemSpecs(item.description)
                            var modified = false
                            var updatedSpecs = specs
                            val safeQuoteNum = quoteNumber.replace("/", "_")

                            if (specs.designImageUri.isNotBlank() && specs.designImageUri.contains("temp_des_")) {
                                val file = File(context.filesDir, File(specs.designImageUri).name)
                                if (file.exists()) {
                                    try {
                                        val destFile = File(context.filesDir, "design_${safeQuoteNum}_${index}.jpg")
                                        file.copyTo(destFile, overwrite = true)
                                        file.delete()
                                        updatedSpecs = updatedSpecs.copy(designImageUri = destFile.absolutePath)
                                        modified = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            if (specs.laminateImageUri.isNotBlank() && specs.laminateImageUri.contains("temp_lam_")) {
                                val file = File(context.filesDir, File(specs.laminateImageUri).name)
                                if (file.exists()) {
                                    try {
                                        val destFile = File(context.filesDir, "laminate_${safeQuoteNum}_${index}.jpg")
                                        file.copyTo(destFile, overwrite = true)
                                        file.delete()
                                        updatedSpecs = updatedSpecs.copy(laminateImageUri = destFile.absolutePath)
                                        modified = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            if (modified) {
                                val newDesc = serializeItemSpecs(desc, updatedSpecs)
                                quotationViewModel.updateQuoteItem(index, item.copy(description = newDesc))
                            }
                        }
                        quotationViewModel.saveQuotation { id ->
                            savedQuotationId = id
                        }
                    }
                )
            }
        }"""
replacements.append(("Wizard Step Review Box", old_wizard_review, new_wizard_review))


# --- Fix 3: Success Dialog at the end of NewQuotationScreen ---
old_success_dialog = """    // Success dialog shown when Saved
    savedQuotationId?.let { id ->
        AlertDialog(
            onDismissRequest = {
                savedQuotationId = null
                quotationViewModel.startNewQuotation()
                onSuccessReturn()
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            title = {
                Text(
                    text = "Quotation Saved Successfully",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quotation ID: #$id",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "The quotation has been saved and is now accessible from the History tab.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            confirmButton = {
                Button(
                    onClick = {
                        savedQuotationId = null
                        quotationViewModel.startNewQuotation()
                        onSuccessReturn()
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                }
                ) {
                    Text("Done")
                }
            }
        }
        )
    }
}"""

new_success_dialog = """    // Success dialog shown when Saved
    savedQuotationId?.let { id ->
        AlertDialog(
            onDismissRequest = {
                savedQuotationId = null
                quotationViewModel.startNewQuotation()
                onSuccessReturn()
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Quotation Saved Successfully",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Quotation ID: #$id",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "The quotation has been saved and is now accessible from the History tab.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        savedQuotationId = null
                        quotationViewModel.startNewQuotation()
                        onSuccessReturn()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done")
                }
            }
        )
    }
}"""
replacements.append(("Success Dialog", old_success_dialog, new_success_dialog))


# --- Fix 4: getCustomItemTemplates ---
old_custom_templates = """fun getCustomItemTemplates(masterData: List<com.example.data.MasterEntity>): List<InteriorItemTemplate> {
    return masterData.filter { it.masterType == "ITEM_TEMPLATE" }.mapNotNull { md ->
        try {
            val name = md.name
            val extraJson = md.description
            if (extraJson.startsWith("{") && extraJson.endsWith("}")) {
                val json = org.json.JSONObject(extraJson)
                val cat = json.optString("category", "General")
                val mat = json.optString("material", "Plywood")
                val grd = json.optString("grade", "")
                val fin = json.optString("finish", "")
                val thk = json.optString("thickness", "")
                val unt = json.optString("unit", "Sq.Ft")
                val hwStr = json.optString("hardware", "")
                val hwList = if (hwStr.isBlank()) emptyList() else hwStr.split(",").map { it.trim() }
                val dNotes = json.optString("notes", "")
                
                val presets = mutableListOf<DimensionPreset>()
                val presetsArr = json.optJSONArray("presets")
                if (presetsArr != null) {
                    for (i in 0 until presetsArr.length()) {
                        val pObj = presetsArr.getJSONObject(i)
                        presets.add(
                            DimensionPreset(
                                label = pObj.optString("label", "Preset"),
                                width = pObj.optString("width", ""),
                                height = pObj.optString("height", ""),
                                depth = pObj.optString("depth", "")
                            )
                        )
                    }
                }
                InteriorItemTemplate(
                    name = name,
                    category = cat,
                    material = mat,
                    grade = grd,
                    finish = fin,
                    thickness = thk,
                    unit = unt,
                    suggestedHardware = hwList,
                    defaultNotes = dNotes,
                    dimensionPresets = presets
                )
                InteriorItemTemplate(
                    name = name,
                    category = "General",
                    material = "Plywood",
                    unit = "Sq.Ft"
                )
            }
            null
        }
    }
}"""

new_custom_templates = """fun getCustomItemTemplates(masterData: List<com.example.data.MasterEntity>): List<InteriorItemTemplate> {
    return masterData.filter { it.masterType == "ITEM_TEMPLATE" }.mapNotNull { md ->
        try {
            val name = md.name
            val extraJson = md.description
            if (extraJson.startsWith("{") && extraJson.endsWith("}")) {
                val json = org.json.JSONObject(extraJson)
                val cat = json.optString("category", "General")
                val mat = json.optString("material", "Plywood")
                val grd = json.optString("grade", "")
                val fin = json.optString("finish", "")
                val thk = json.optString("thickness", "")
                val unt = json.optString("unit", "Sq.Ft")
                val hwStr = json.optString("hardware", "")
                val hwList = if (hwStr.isBlank()) emptyList() else hwStr.split(",").map { it.trim() }
                val dNotes = json.optString("notes", "")
                
                val presets = mutableListOf<DimensionPreset>()
                val presetsArr = json.optJSONArray("presets")
                if (presetsArr != null) {
                    for (i in 0 until presetsArr.length()) {
                        val pObj = presetsArr.getJSONObject(i)
                        presets.add(
                            DimensionPreset(
                                label = pObj.optString("label", "Preset"),
                                width = pObj.optString("width", ""),
                                height = pObj.optString("height", ""),
                                depth = pObj.optString("depth", "")
                            )
                        )
                    }
                }
                InteriorItemTemplate(
                    name = name,
                    category = cat,
                    material = mat,
                    grade = grd,
                    finish = fin,
                    thickness = thk,
                    unit = unt,
                    suggestedHardware = hwList,
                    defaultNotes = dNotes,
                    dimensionPresets = presets
                )
            } else {
                InteriorItemTemplate(
                    name = name,
                    category = "General",
                    material = "Plywood",
                    unit = "Sq.Ft"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}"""
replacements.append(("Custom Item Templates", old_custom_templates, new_custom_templates))


# --- Fix 5: getMaterialRules ---
old_material_rules = """    val customRules = allMasterData.filter { it.masterType == "MATERIAL_RULE" }.mapNotNull { md ->
        try {
            val matName = md.name
            val json = org.json.JSONObject(md.description)
            
            val allowedGradesArr = json.optJSONArray("allowedGrades")
            val allowedGrades = if (allowedGradesArr != null) {
                (0 until allowedGradesArr.length()).map { allowedGradesArr.getString(it) }

            }
            val allowedFinishesArr = json.optJSONArray("allowedFinishes")
            val allowedFinishes = if (allowedFinishesArr != null) {
                (0 until allowedFinishesArr.length()).map { allowedFinishesArr.getString(it) }

            }
            val allowedThicknessesArr = json.optJSONArray("allowedThicknesses")
            val allowedThicknesses = if (allowedThicknessesArr != null) {
                (0 until allowedThicknessesArr.length()).map { allowedThicknessesArr.getString(it) }

            }
            val recommendedHardwareArr = json.optJSONArray("recommendedHardware")
            val recommendedHardware = if (recommendedHardwareArr != null) {
                (0 until recommendedHardwareArr.length()).map { recommendedHardwareArr.getString(it) }

            }
            MaterialRule(
                material = matName,
                allowedGrades = allowedGrades,
                allowedFinishes = allowedFinishes,
                allowedThicknesses = allowedThicknesses,
                defaultGrade = json.optString("defaultGrade", ""),
                defaultFinish = json.optString("defaultFinish", ""),
                defaultThickness = json.optString("defaultThickness", ""),
                recommendedUnit = json.optString("recommendedUnit", "Sq.Ft"),
                recommendedHardware = recommendedHardware,
                recommendedBrand = json.optString("recommendedBrand", "")
            )
            null

        }
    }"""

new_material_rules = """    val customRules = allMasterData.filter { it.masterType == "MATERIAL_RULE" }.mapNotNull { md ->
        try {
            val matName = md.name
            val json = org.json.JSONObject(md.description)
            
            val allowedGradesArr = json.optJSONArray("allowedGrades")
            val allowedGrades = if (allowedGradesArr != null) {
                (0 until allowedGradesArr.length()).map { allowedGradesArr.getString(it) }
            } else {
                emptyList()
            }
            val allowedFinishesArr = json.optJSONArray("allowedFinishes")
            val allowedFinishes = if (allowedFinishesArr != null) {
                (0 until allowedFinishesArr.length()).map { allowedFinishesArr.getString(it) }
            } else {
                emptyList()
            }
            val allowedThicknessesArr = json.optJSONArray("allowedThicknesses")
            val allowedThicknesses = if (allowedThicknessesArr != null) {
                (0 until allowedThicknessesArr.length()).map { allowedThicknessesArr.getString(it) }
            } else {
                emptyList()
            }
            val recommendedHardwareArr = json.optJSONArray("recommendedHardware")
            val recommendedHardware = if (recommendedHardwareArr != null) {
                (0 until recommendedHardwareArr.length()).map { recommendedHardwareArr.getString(it) }
            } else {
                emptyList()
            }
            MaterialRule(
                material = matName,
                allowedGrades = allowedGrades,
                allowedFinishes = allowedFinishes,
                allowedThicknesses = allowedThicknesses,
                defaultGrade = json.optString("defaultGrade", ""),
                defaultFinish = json.optString("defaultFinish", ""),
                defaultThickness = json.optString("defaultThickness", ""),
                recommendedUnit = json.optString("recommendedUnit", "Sq.Ft"),
                recommendedHardware = recommendedHardware,
                recommendedBrand = json.optString("recommendedBrand", "")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }"""
replacements.append(("Material Rules", old_material_rules, new_material_rules))


# --- Fix 6: ItemConfigDialog Validation button braces ---
old_validation_braces = """            com.example.ui.components.PremiumPrimaryButton(
                onClick = {
                    if (itemName.isBlank()) {
                        Toast.makeText(context, "Item Name is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    if (qtyVal < 0 || rateVal < 0) {
                        Toast.makeText(context, "Values cannot be negative", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    if (qtyVal == 0.0) {
                        Toast.makeText(context, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (material.isBlank()) {
                        Toast.makeText(context, "Material is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (wFeet < 0 || hFeet < 0 || dFeet < 0) {
                        Toast.makeText(context, "Dimensions cannot be negative", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if ((isAreaBased || isRftBased) && (wFeet == 0.0 || (isAreaBased && hFeet == 0.0))) {
                        Toast.makeText(context, "Dimensions cannot be zero for the selected unit", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton

                    }"""

new_validation_braces = """            com.example.ui.components.PremiumPrimaryButton(
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
                    if (material.isBlank()) {
                        Toast.makeText(context, "Material is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (wFeet < 0 || hFeet < 0 || dFeet < 0) {
                        Toast.makeText(context, "Dimensions cannot be negative", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if ((isAreaBased || isRftBased) && (wFeet == 0.0 || (isAreaBased && hFeet == 0.0))) {
                        Toast.makeText(context, "Dimensions cannot be zero for the selected unit", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }"""
replacements.append(("Validation Braces", old_validation_braces, new_validation_braces))


# --- Fix 7: FilterChip onClick-label brace issues ---
old_filter_chips = """                                activeTemplate.dimensionPresets.forEach { preset ->
                                    val isSelected = widthStr == preset.width && heightStr == preset.height && depthStr == preset.depth
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            widthStr = preset.width
                                            heightStr = preset.height
                                            depthStr = preset.depth
                                        label = { Text(preset.label) }
                                    }
                                    )
                                }
                                FilterChip(
                                    selected = widthStr.isEmpty() && heightStr.isEmpty(),
                                    onClick = {
                                        widthStr = ""
                                        heightStr = ""
                                        depthStr = ""
                                    label = { Text("Custom") }
                                }
                                )"""

new_filter_chips = """                                activeTemplate.dimensionPresets.forEach { preset ->
                                    val isSelected = widthStr == preset.width && heightStr == preset.height && depthStr == preset.depth
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            widthStr = preset.width
                                            heightStr = preset.height
                                            depthStr = preset.depth
                                        },
                                        label = { Text(preset.label) }
                                    )
                                }
                                FilterChip(
                                    selected = widthStr.isEmpty() && heightStr.isEmpty(),
                                    onClick = {
                                        widthStr = ""
                                        heightStr = ""
                                        depthStr = ""
                                    },
                                    label = { Text("Custom") }
                                )"""
replacements.append(("Filter Chips", old_filter_chips, new_filter_chips))


# --- Fixes 8: ElevatedCard Corruptions (6 instances) ---

card_1_old = """            if (selectedCustomer == null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            }
        }
    }
}
) {
                    Column(modifier = Modifier.padding(16.dp)) {"""

card_1_new = """            if (selectedCustomer == null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {"""
replacements.append(("ElevatedCard 1", card_1_old, card_1_new))


card_2_old = """                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            }
        }
    }
}
) {
                    Row("""

card_2_new = """                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                ) {
                    Row("""
replacements.append(("ElevatedCard 2", card_2_old, card_2_new))


card_3_old = """                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            }
        }
    }
}
) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {"""

card_3_new = """                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {"""
replacements.append(("ElevatedCard 3", card_3_old, card_3_new))


card_4_old = """                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                }
            }
        }
    }
}
) {
                        Column("""

card_4_new = """                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                    ) {
                        Column("""
replacements.append(("ElevatedCard 4", card_4_old, card_4_new))


card_5_old = """        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    }
}
) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {"""

card_5_new = """        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {"""
replacements.append(("ElevatedCard 5", card_5_old, card_5_new))


card_6_old = """                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
            }
        }
    }
}
) {
                    Column("""

card_6_new = """                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                ) {
                    Column("""
replacements.append(("ElevatedCard 6", card_6_old, card_6_new))


for name, old, new in replacements:
    if old in content:
        content = content.replace(old, new)
        print(f"[SUCCESS] Replaced: {name}")
    else:
        # Try normalizing whitespace if direct replace fails
        print(f"[FAILED] Direct match not found for: {name}")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Fix script completed.")
