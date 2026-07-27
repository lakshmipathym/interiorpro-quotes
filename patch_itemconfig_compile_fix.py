import re

file_path = "/app/applet/app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace the QuotionItem initialization and LaunchedEffect parsing:
content = content.replace("""            try {
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
            }""", """            material = existing.material
            userDescription = existing.description""")

content = content.replace("""            designPath = existing.referenceImagePath ?: ""
            userDescription = existing.userDescription ?: ""
""", """            userDescription = existing.description
""")

content = content.replace("""                    val specJson = JSONObject().apply {
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
                    )""", """
                    val qty = quantityStr.toDoubleOrNull() ?: 1.0
                    val rt = rateStr.toDoubleOrNull() ?: 0.0
                    val item = QuotationItem(
                        id = if (itemIndex != null) currentItems[itemIndex].id else 0,
                        quotationId = 0,
                        itemName = itemName.trim(),
                        description = userDescription.trim(),
                        material = material,
                        finish = thickness,
                        quantity = qty,
                        unit = unit,
                        rate = rt,
                        amount = qty * rt
                    )""")

with open(file_path, "w") as f:
    f.write(content)
