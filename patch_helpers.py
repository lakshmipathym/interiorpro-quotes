import sys

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# patch getGradesForMaterial
old_grades = """    return when (resolveMaterialType(material)) {
        "plywood" -> listOf("MR", "BWR", "BWP", "Marine Ply", "Commercial Ply")
        "mdf" -> listOf("Standard MDF", "HDMR", "HDF", "Exterior MDF")
        "particle" -> listOf("Standard", "Prelam", "Moisture Resistant")
        "acp" -> listOf("3mm", "4mm", "6mm")
        "aluminium" -> listOf("Slim", "Standard", "Heavy", "Modular")
        "glass" -> listOf("Clear", "Frosted", "Toughened", "Lacquered", "Fluted")
        else -> emptyList()
    }"""
new_grades = """    return when (resolveMaterialType(material)) {
        "plywood" -> listOf("MR", "BWR", "BWP", "Marine Ply", "Commercial Ply")
        "mdf" -> listOf("Standard MDF", "HDMR", "HDF", "Exterior MDF")
        "particle" -> listOf("Standard / Plain", "Pre-Laminated", "Moisture Resistant")
        "acp" -> listOf("Interior Grade", "Exterior Grade")
        "aluminium" -> listOf("Slim", "Standard", "Heavy", "Modular")
        "glass" -> listOf("Clear", "Frosted", "Toughened", "Lacquered", "Fluted", "Laminated")
        "wpc" -> listOf("Standard", "High Density")
        "pvc" -> listOf("Standard", "Premium")
        "blockboard" -> listOf("MR", "BWP")
        else -> emptyList()
    }"""
content = content.replace(old_grades, new_grades)

# patch getFinishesForMaterial
old_finishes = """    return when (resolveMaterialType(material)) {
        "plywood" -> listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane")
        "aluminium" -> listOf("Powder Coating", "Anodized", "PVDF", "Matt", "Gloss")
        "glass" -> listOf("Clear", "Frosted", "Tinted", "Etched", "Toughened")
        "mdf" -> listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane")
        "particle" -> listOf("Laminate", "Veneer", "PU")
        "other" -> masterFinishes.ifEmpty { listOf("Laminate", "PU", "Acrylic") }
        else -> emptyList()
    }"""
new_finishes = """    return when (resolveMaterialType(material)) {
        "plywood" -> listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane")
        "aluminium" -> listOf("Powder Coating", "Anodized", "PVDF", "Matt", "Gloss")
        "glass" -> listOf("Plain", "Tinted", "Etched", "Back-Painted")
        "mdf" -> listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane")
        "particle" -> listOf("Laminate", "Veneer", "PU")
        "wpc" -> listOf("Plain", "PVC Laminate", "PU Paint", "Acrylic")
        "pvc" -> listOf("Plain", "PVC Laminate")
        "blockboard" -> listOf("Laminate", "Veneer", "PU")
        "other" -> masterFinishes.ifEmpty { listOf("Laminate", "PU", "Acrylic") }
        else -> emptyList()
    }"""
content = content.replace(old_finishes, new_finishes)

# patch getThicknessOptionsForMaterial
old_thicknesses = """    return when (resolveMaterialType(material)) {
        "plywood" -> listOf("6 mm", "9 mm", "12 mm", "16 mm", "18 mm", "25 mm")
        "acp" -> listOf("3 mm", "4 mm", "6 mm")
        "glass" -> listOf("5 mm", "8 mm", "10 mm", "12 mm")
        "mdf" -> listOf("6 mm", "9 mm", "12 mm", "17 mm", "18 mm", "25 mm")
        "particle" -> listOf("9 mm", "12 mm", "18 mm", "25 mm")
        else -> emptyList()
    }"""
new_thicknesses = """    return when (resolveMaterialType(material)) {
        "plywood" -> listOf("6 mm", "9 mm", "12 mm", "16 mm", "18 mm", "25 mm")
        "acp" -> listOf("3 mm", "4 mm", "6 mm")
        "glass" -> listOf("5 mm", "8 mm", "10 mm", "12 mm")
        "mdf" -> listOf("6 mm", "9 mm", "12 mm", "17 mm", "18 mm", "25 mm")
        "particle" -> listOf("6 mm", "8 mm", "9 mm", "12 mm", "15 mm", "17 mm", "18 mm", "25 mm")
        "wpc" -> listOf("6 mm", "12 mm", "18 mm", "25 mm", "28 mm")
        "pvc" -> listOf("6 mm", "12 mm", "18 mm")
        "blockboard" -> listOf("19 mm", "25 mm")
        else -> emptyList()
    }"""
content = content.replace(old_thicknesses, new_thicknesses)

# Also fix resolveMaterialType to add wpc, pvc, blockboard
old_resolve = """        mLower.contains("glass") -> "glass"
        mLower.contains("edge band") || mLower.contains("edgeband") -> "edgeband"
        mLower.contains("hinge") -> "hinges"
        mLower.contains("handle") -> "handles"
        mLower.contains("profile") -> "profile"
        else -> "other"
    }"""
new_resolve = """        mLower.contains("glass") -> "glass"
        mLower.contains("wpc") -> "wpc"
        mLower.contains("pvc") -> "pvc"
        mLower.contains("blockboard") || mLower.contains("block board") -> "blockboard"
        mLower.contains("edge band") || mLower.contains("edgeband") -> "edgeband"
        mLower.contains("hinge") -> "hinges"
        mLower.contains("handle") -> "handles"
        mLower.contains("profile") -> "profile"
        else -> "other"
    }"""
content = content.replace(old_resolve, new_resolve)

with open(file_path, "w") as f:
    f.write(content)
print("Patched helpers")
