import sys

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "// Auto-update grade" in line:
        if "val allowedGrades = getGradesForMaterial(m, allMasterData)" in lines[i+1]:
            skip = True
            new_lines.append(line)
            new_lines.append(lines[i+1])
            new_lines.append("""                    if (matchedRule != null && matchedRule.defaultGrade.isNotBlank() && allowedGrades.contains(matchedRule.defaultGrade)) {
                        grade = matchedRule.defaultGrade
                    } else if (!allowedGrades.contains(grade)) {
                        grade = allowedGrades.firstOrNull() ?: ""
                    }
                    
                    // Auto-update finish
                    val allowedFinishes = getFinishesForMaterial(m, finishes, allMasterData)
                    if (matchedRule != null && matchedRule.defaultFinish.isNotBlank() && allowedFinishes.contains(matchedRule.defaultFinish)) {
                        finish = matchedRule.defaultFinish
                    } else if (!allowedFinishes.contains(finish)) {
                        finish = allowedFinishes.firstOrNull() ?: ""
                    }
                    
                    // Auto-update thickness
                    val allowedThicknesses = getThicknessOptionsForMaterial(m, allMasterData)
                    if (matchedRule != null && matchedRule.defaultThickness.isNotBlank() && allowedThicknesses.contains(matchedRule.defaultThickness)) {
                        thickness = matchedRule.defaultThickness
                    } else if (!allowedThicknesses.contains(thickness)) {
                        thickness = allowedThicknesses.firstOrNull() ?: ""
                    }
                    
                    // Auto-update brand (optional)
                    if (matchedRule != null && matchedRule.recommendedBrand.isNotBlank()) {
                        brand = matchedRule.recommendedBrand
                    } else {
                        brand = ""
                    }
                    
                    // Auto-update hardware
                    if (matchedRule != null && matchedRule.recommendedHardware.isNotEmpty()) {
                        val suggested = getRecommendedHardware(itemName, projectType, category)
                        hardware = if (suggested.isNotEmpty()) suggested.joinToString(", ") else matchedRule.recommendedHardware.joinToString(", ")
                    }
""")
            continue
    
    if skip:
        if "profileSeries =" in line:
            skip = False
            new_lines.append(line)
    else:
        new_lines.append(line)

with open(file_path, "w") as f:
    f.writelines(new_lines)
print("Done patching.")
