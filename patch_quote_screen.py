with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'r') as f:
    content = f.read()

# Replace CATEGORY and MATERIAL filtering logic
# Ensure we distinct() them
import re

content = content.replace(
    'masterData.filter { it.masterType == "CATEGORY" }.map { it.name }',
    'masterData.filter { it.masterType == "CATEGORY" || it.masterType == "PROJECT_CATEGORY" }.map { it.name }.distinct()'
)

content = content.replace(
    'masterData.filter { it.masterType == "MATERIAL" }.map { it.name }',
    'masterData.filter { it.masterType == "MATERIAL" || it.masterType == "MATERIAL_TYPE" }.map { it.name }.distinct()'
)

content = content.replace(
    'allMasterData.filter { it.masterType == "PROJECT_CATEGORY" }.map { it.name }.ifEmpty { listOf("Premium", "Standard", "Economy") }',
    'allMasterData.filter { it.masterType == "CATEGORY" || it.masterType == "PROJECT_CATEGORY" }.map { it.name }.distinct().ifEmpty { listOf("Premium", "Standard", "Economy") }'
)

content = content.replace(
    'allMasterData.filter { it.masterType == "MATERIAL_TYPE" }.map { it.name }.ifEmpty { listOf("BWP Plywood", "MDF (Exterior Grade)", "HDF", "Particle Board", "Aluminium Section Framework") }',
    'allMasterData.filter { it.masterType == "MATERIAL" || it.masterType == "MATERIAL_TYPE" }.map { it.name }.distinct().ifEmpty { listOf("BWP Plywood", "MDF (Exterior Grade)", "HDF", "Particle Board", "Aluminium Section Framework") }'
)

content = content.replace(
    'allMasterData.filter { it.masterType == "MATERIAL" }.map { it.name }',
    'allMasterData.filter { it.masterType == "MATERIAL" || it.masterType == "MATERIAL_TYPE" }.map { it.name }.distinct()'
)

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'w') as f:
    f.write(content)
