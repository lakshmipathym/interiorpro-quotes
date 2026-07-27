import os

file_path = "/app/applet/app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

old_parse = """fun parseItemSpecs(description: String): Pair<String, ItemSpecs> {
    if (!description.contains("|||")) {
        return Pair(description, ItemSpecs())
    }"""

new_parse = """fun parseItemSpecs(description: String): Pair<String, ItemSpecs> {
    if (!description.contains("|||")) {
        val trimmed = description.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = org.json.JSONObject(trimmed)
                val specs = ItemSpecs(
                    width = json.optString("width", ""),
                    height = json.optString("height", ""),
                    depth = json.optString("depth", ""),
                    doorType = json.optString("doorType", ""),
                    finish = json.optString("finish", ""),
                    hardware = json.optString("hardware", ""),
                    brand = json.optString("brand", ""),
                    thickness = json.optString("thickness", ""),
                    colour = json.optString("colour", ""),
                    laminateImageUri = json.optString("laminateImageUri", ""),
                    designImageUri = json.optString("designImageUri", ""),
                    profileSeries = json.optString("profileSeries", ""),
                    profileColour = json.optString("profileColour", ""),
                    glassType = json.optString("glassType", ""),
                    glassThickness = json.optString("glassThickness", ""),
                    acpColour = json.optString("acpColour", ""),
                    grade = json.optString("grade", "")
                )
                // Return original description but if it was entirely JSON we just show it empty or a default string.
                // However, they said "Description field displays JSON instead of user description". 
                // That implies we should hide the JSON.
                return Pair("", specs)
            } catch (e: Exception) {
                // ignore
            }
        }
        return Pair(description, ItemSpecs())
    }"""

content = content.replace(old_parse, new_parse)

old_edit = """    LaunchedEffect(itemIndex) {
        if (itemIndex != null && itemIndex in currentItems.indices) {
            val existing = currentItems[itemIndex]
            itemName = existing.itemName
            unit = existing.unit
            quantityStr = existing.quantity.toString()
            rateStr = existing.rate.toString()
            userDescription = existing.description

            material = existing.material
            userDescription = existing.description
        }
    }"""

new_edit = """    LaunchedEffect(itemIndex) {
        if (itemIndex != null && itemIndex in currentItems.indices) {
            val existing = currentItems[itemIndex]
            itemName = existing.itemName
            unit = existing.unit
            quantityStr = existing.quantity.toString()
            rateStr = existing.rate.toString()
            
            val (desc, specs) = parseItemSpecs(existing.description)
            userDescription = desc
            material = existing.material
            
            profileSeries = specs.profileSeries
            profileColour = specs.profileColour
            glassType = specs.glassType
            glassThickness = specs.glassThickness
            acpColour = specs.acpColour
            thickness = specs.thickness
            grade = specs.grade
            brand = specs.brand
            hardware = specs.hardware
            widthStr = specs.width
            heightStr = specs.height
            depthStr = specs.depth
            designPath = specs.designImageUri
        }
    }"""

content = content.replace(old_edit, new_edit)


old_save = """                    val item = QuotationItem(
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
                    )"""

new_save = """                    val specs = ItemSpecs(
                        width = widthStr.trim(),
                        height = heightStr.trim(),
                        depth = depthStr.trim(),
                        doorType = "",
                        finish = thickness.trim(),
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
                        grade = grade.trim()
                    )
                    
                    val finalDesc = serializeItemSpecs(userDescription.trim(), specs)

                    val item = QuotationItem(
                        id = if (itemIndex != null) currentItems[itemIndex].id else 0,
                        quotationId = 0,
                        itemName = itemName.trim(),
                        description = finalDesc,
                        material = material,
                        finish = thickness,
                        quantity = qty,
                        unit = unit,
                        rate = rt,
                        amount = qty * rt
                    )"""

content = content.replace(old_save, new_save)

with open(file_path, "w") as f:
    f.write(content)

