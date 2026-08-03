import sys

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

old_func = """fun getFinishesForMaterial(material: String, masterFinishes: List<String>, allMasterData: List<com.example.data.MasterEntity> = emptyList()): List<String> {"""
new_func = """fun getFinishesForMaterial(material: String, grade: String, masterFinishes: List<String>, allMasterData: List<com.example.data.MasterEntity> = emptyList()): List<String> {
    val mLower = resolveMaterialType(material)
    val gLower = grade.lowercase(java.util.Locale.US)
    if (mLower == "particle" && (gLower.contains("pre-laminated") || gLower.contains("prelam"))) {
        return emptyList()
    }
"""
content = content.replace(old_func, new_func)

# Fix calls
content = content.replace("getFinishesForMaterial(material, finishes, allMasterData)", "getFinishesForMaterial(material, grade, finishes, allMasterData)")
content = content.replace("getFinishesForMaterial(m, finishes, allMasterData)", "getFinishesForMaterial(m, grade, finishes, allMasterData)")

with open(file_path, "w") as f:
    f.write(content)
print("Patched prelam logic")
