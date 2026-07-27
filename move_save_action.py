with open("app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt", "r") as f:
    content = f.read()

import re

# We will just make the bottom bar's onClick call onSave logic.
# Wait, actually WizardStepReview has its own Save button right at the bottom.
# So if we just hide the bottom bar's "Save Quotation" button in step 3, it would be fine, but we still want the bottom bar for the Back button!
# Let's extract the `onSave` block to a local variable `val performSaveQuotation = { ... }` before Scaffold.

start_str = "    var showDiscardDialog by remember { mutableStateOf(false) }"

new_func = """    var showDiscardDialog by remember { mutableStateOf(false) }

    val performSaveQuotation = {
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
"""

content = content.replace(start_str, new_func)

# Remove the block in WizardStepReview
old_review = """                    else -> WizardStepReview(
                        quotationViewModel = quotationViewModel,
                        quoteNumber = quoteNumber,
                        customerName = currentCustomer?.customerName ?: "Unknown",
                        customerPhone = currentCustomer?.mobileNumber ?: "",
                        itemsCount = quoteItems.size,
                        subtotal = subtotal,
                        discount = discount,
                        gstAmount = gstAmount,
                        grandTotal = grandTotal,
                        onSave = {"""

new_review = """                    else -> WizardStepReview(
                        quotationViewModel = quotationViewModel,
                        quoteNumber = quoteNumber,
                        customerName = currentCustomer?.customerName ?: "Unknown",
                        customerPhone = currentCustomer?.mobileNumber ?: "",
                        itemsCount = quoteItems.size,
                        subtotal = subtotal,
                        discount = discount,
                        gstAmount = gstAmount,
                        grandTotal = grandTotal,
                        onSave = performSaveQuotation
                    )
            }
        }
"""

# Regex to replace the whole block until `)
#             }
#         }`
import re
# Wait, I can just use a simple string replace for the onSave block up to the `)`
block_to_remove_start = content.find("onSave = {")
block_to_remove_end = content.find("                    )", block_to_remove_start)

if block_to_remove_start != -1 and block_to_remove_end != -1:
    content = content[:block_to_remove_start] + "onSave = performSaveQuotation\n" + content[block_to_remove_end:]

# Now replace the bottom bar button action
old_bottom = """                Button(
                    onClick = {
                        when (activeStep) {
                            1 -> activeStep = 2
                            2 -> activeStep = 3
                            else -> { /* Handled inside WizardStepReview save callback */ }
                        }
                    },"""

new_bottom = """                Button(
                    onClick = {
                        when (activeStep) {
                            1 -> activeStep = 2
                            2 -> activeStep = 3
                            else -> performSaveQuotation()
                        }
                    },"""

content = content.replace(old_bottom, new_bottom)

with open("app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt", "w") as f:
    f.write(content)

