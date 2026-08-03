import sys

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

import re

# Patch getMaterialRules
old_rules = """        MaterialRule(
            material = "Particle Board",
            allowedGrades = listOf("Standard", "Prelam", "Moisture Resistant"),
            allowedFinishes = listOf("Laminate", "Veneer", "PU"),
            allowedThicknesses = listOf("9 mm", "12 mm", "18 mm", "25 mm"),
            defaultGrade = "Prelam",
            defaultFinish = "Laminate",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "ACP",
            allowedGrades = listOf("3mm", "4mm", "6mm"),
            allowedFinishes = listOf("Matt", "Gloss", "PVDF", "Metallic"),
            allowedThicknesses = listOf("3 mm", "4 mm", "6 mm"),
            defaultGrade = "4mm",
            defaultFinish = "Matt",
            defaultThickness = "4 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "Aluminium Composite Panel (ACP)",
            allowedGrades = listOf("3mm", "4mm", "6mm"),
            allowedFinishes = listOf("Matt", "Gloss", "PVDF", "Metallic"),
            allowedThicknesses = listOf("3 mm", "4 mm", "6 mm"),
            defaultGrade = "4mm",
            defaultFinish = "Matt",
            defaultThickness = "4 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "Aluminium",
            allowedGrades = listOf("Slim", "Standard", "Heavy", "Modular"),
            allowedFinishes = listOf("Powder Coating", "Anodized", "PVDF", "Matt", "Gloss"),
            allowedThicknesses = listOf("1 mm", "1.5 mm", "2 mm", "3 mm"),
            defaultGrade = "Standard",
            defaultFinish = "Powder Coating",
            defaultThickness = "1.5 mm",
            recommendedUnit = "R.Ft",
            recommendedHardware = listOf("Nylon Rollers", "Floor Springs", "D-Handles")
        ),
        MaterialRule(
            material = "Aluminium Section Framework",
            allowedGrades = listOf("Slim", "Standard", "Heavy", "Modular"),
            allowedFinishes = listOf("Powder Coating", "Anodized", "PVDF", "Matt", "Gloss"),
            allowedThicknesses = listOf("1 mm", "1.5 mm", "2 mm", "3 mm"),
            defaultGrade = "Standard",
            defaultFinish = "Powder Coating",
            defaultThickness = "1.5 mm",
            recommendedUnit = "R.Ft",
            recommendedHardware = listOf("Nylon Rollers", "Floor Springs", "D-Handles")
        ),
        MaterialRule(
            material = "Glass",
            allowedGrades = listOf("Clear", "Frosted", "Toughened", "Lacquered", "Fluted"),
            allowedFinishes = listOf("Clear", "Frosted", "Tinted", "Etched", "Toughened"),
            allowedThicknesses = listOf("5 mm", "8 mm", "10 mm", "12 mm"),
            defaultGrade = "Toughened",
            defaultFinish = "Clear",
            defaultThickness = "8 mm",
            recommendedUnit = "Sq.Ft",
            recommendedHardware = listOf("Shower Hinges", "SS Connectors", "Water Barriers")
        )"""

new_rules = """        MaterialRule(
            material = "Particle Board",
            allowedGrades = listOf("Standard / Plain", "Pre-Laminated", "Moisture Resistant"),
            allowedFinishes = listOf("Laminate", "Veneer", "PU"),
            allowedThicknesses = listOf("6 mm", "8 mm", "9 mm", "12 mm", "15 mm", "17 mm", "18 mm", "25 mm"),
            defaultGrade = "Pre-Laminated",
            defaultFinish = "",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "ACP",
            allowedGrades = listOf("Interior Grade", "Exterior Grade"),
            allowedFinishes = listOf("Matt", "Gloss", "PVDF", "Metallic"),
            allowedThicknesses = listOf("3 mm", "4 mm", "6 mm"),
            defaultGrade = "Exterior Grade",
            defaultFinish = "Matt",
            defaultThickness = "4 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "Aluminium Composite Panel (ACP)",
            allowedGrades = listOf("Interior Grade", "Exterior Grade"),
            allowedFinishes = listOf("Matt", "Gloss", "PVDF", "Metallic"),
            allowedThicknesses = listOf("3 mm", "4 mm", "6 mm"),
            defaultGrade = "Exterior Grade",
            defaultFinish = "Matt",
            defaultThickness = "4 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "Aluminium",
            allowedGrades = listOf("Slim", "Standard", "Heavy", "Modular"),
            allowedFinishes = listOf("Powder Coating", "Anodized", "PVDF", "Matt", "Gloss"),
            allowedThicknesses = listOf("1 mm", "1.5 mm", "2 mm", "3 mm"),
            defaultGrade = "Standard",
            defaultFinish = "Powder Coating",
            defaultThickness = "1.5 mm",
            recommendedUnit = "R.Ft",
            recommendedHardware = listOf("Nylon Rollers", "Floor Springs", "D-Handles")
        ),
        MaterialRule(
            material = "Aluminium Section Framework",
            allowedGrades = listOf("Slim", "Standard", "Heavy", "Modular"),
            allowedFinishes = listOf("Powder Coating", "Anodized", "PVDF", "Matt", "Gloss"),
            allowedThicknesses = listOf("1 mm", "1.5 mm", "2 mm", "3 mm"),
            defaultGrade = "Standard",
            defaultFinish = "Powder Coating",
            defaultThickness = "1.5 mm",
            recommendedUnit = "R.Ft",
            recommendedHardware = listOf("Nylon Rollers", "Floor Springs", "D-Handles")
        ),
        MaterialRule(
            material = "Glass",
            allowedGrades = listOf("Clear", "Frosted", "Toughened", "Lacquered", "Fluted", "Laminated"),
            allowedFinishes = listOf("Plain", "Tinted", "Etched", "Back-Painted"),
            allowedThicknesses = listOf("5 mm", "8 mm", "10 mm", "12 mm"),
            defaultGrade = "Toughened",
            defaultFinish = "Plain",
            defaultThickness = "8 mm",
            recommendedUnit = "Sq.Ft",
            recommendedHardware = listOf("Shower Hinges", "SS Connectors", "Water Barriers")
        ),
        MaterialRule(
            material = "WPC",
            allowedGrades = listOf("Standard", "High Density"),
            allowedFinishes = listOf("Plain", "PVC Laminate", "PU Paint", "Acrylic"),
            allowedThicknesses = listOf("6 mm", "12 mm", "18 mm", "25 mm", "28 mm"),
            defaultGrade = "High Density",
            defaultFinish = "Plain",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "PVC Board",
            allowedGrades = listOf("Standard", "Premium"),
            allowedFinishes = listOf("Plain", "PVC Laminate"),
            allowedThicknesses = listOf("6 mm", "12 mm", "18 mm"),
            defaultGrade = "Premium",
            defaultFinish = "Plain",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "Blockboard",
            allowedGrades = listOf("MR", "BWP"),
            allowedFinishes = listOf("Laminate", "Veneer", "PU"),
            allowedThicknesses = listOf("19 mm", "25 mm"),
            defaultGrade = "BWP",
            defaultFinish = "Laminate",
            defaultThickness = "19 mm",
            recommendedUnit = "Sq.Ft"
        )"""

if old_rules in content:
    content = content.replace(old_rules, new_rules)
    with open(file_path, "w") as f:
        f.write(content)
    print("Patched rules successfully.")
else:
    print("Could not find rules string")
