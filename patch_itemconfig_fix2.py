import re

file_path = "/app/applet/app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# find:
#     } actions = {
# and everything down to:
#         )
#     }
# }

new_dialog_invocation = """
    com.example.ui.components.PremiumDialog(
        onDismissRequest = onDismiss,
        title = if (itemIndex == null) "Configure Item" else "Edit Item",
        modifier = Modifier.fillMaxWidth(),
        actions = {
            com.example.ui.components.PremiumTextButton(text = "Cancel", onClick = onDismiss)
            Spacer(modifier = Modifier.width(8.dp))
            com.example.ui.components.PremiumPrimaryButton(
                text = "Save",
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
    ) {
"""

# Let's just fix it manually with replace
old = """    } actions = {"""
new = """    }, actions = {"""
# wait I did PremiumDialog( ... ) { ... } actions = { ... } which is syntax error in Kotlin.
# I need to change:
#     com.example.ui.components.PremiumDialog(
#         onDismissRequest = onDismiss,
#         title = if (itemIndex == null) "Configure Item" else "Edit Item",
#         modifier = Modifier.fillMaxWidth()
#     ) {
#         Column(

# to:
#     com.example.ui.components.PremiumDialog(
#         onDismissRequest = onDismiss,
#         title = if (itemIndex == null) "Configure Item" else "Edit Item",
#         modifier = Modifier.fillMaxWidth(),
#         actions = { ... }
#     ) {

