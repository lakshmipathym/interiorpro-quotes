import sys

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# restore asterisks
content = content.replace('-> "Profile Type"', '-> "Profile Type *"')
content = content.replace('-> "Glass Type"', '-> "Glass Type *"')
content = content.replace('-> "Grade"', '-> "Grade *"')
content = content.replace('label = "Finish Type"', 'label = "Finish Type *"')

# And I will also add the asterisk for conditionally required Thickness:
old_thickness_label = 'label = "Thickness",'
new_thickness_label = 'label = "Thickness *",'
if old_thickness_label in content:
    content = content.replace(old_thickness_label, new_thickness_label)

# Now add the save validation logic
old_validation_block = """                    if (material.isBlank()) {
                        Toast.makeText(context, "Material is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }"""
new_validation_block = """                    if (material.isBlank()) {
                        Toast.makeText(context, "Material is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    val currentGradeOpts = getGradesForMaterial(material, allMasterData)
                    if (currentGradeOpts.isNotEmpty() && grade.isBlank()) {
                        Toast.makeText(context, "Grade/Type is required for $material", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    val currentFinishOpts = getFinishesForMaterial(material, finishes, allMasterData)
                    if (currentFinishOpts.isNotEmpty() && finish.isBlank()) {
                        Toast.makeText(context, "Finish Type is required for $material", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    val currentThicknessOpts = getThicknessOptionsForMaterial(material, allMasterData)
                    if (currentThicknessOpts.isNotEmpty() && thickness.isBlank()) {
                        Toast.makeText(context, "Thickness is required for $material", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }"""

if old_validation_block in content:
    content = content.replace(old_validation_block, new_validation_block)
    with open(file_path, "w") as f:
        f.write(content)
    print("Patched conditional validations")
else:
    print("Could not find material validation block")
