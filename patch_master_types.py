with open('app/src/main/java/com/example/ui/company/MasterDataScreen.kt', 'r') as f:
    content = f.read()

target = """    val masterTypes = listOf(
        "PROJECT_TYPE" to "Projects",
        "CATEGORY" to "Categories",
        "MATERIAL" to "Materials",
        "FINISH_TYPE" to "Finishes",
        "UNIT" to "Units",
        "WARRANTY" to "Warranties",
        "ACCESSORY" to "Accessories",
        "TERMS" to "Terms & Conditions",
        "DOOR_SYSTEM" to "Door Systems",
        "HARDWARE" to "Hardware Packages",
        "BRAND" to "Brands",
        "THICKNESS" to "Thicknesses",
        "COLOUR" to "Colours & Shades",
        "GST_RATE" to "GST Rates",
        "PAYMENT_TERM" to "Payment Terms"
    )"""

replacement = """    val masterTypes = listOf(
        "PROJECT_TYPE" to "Projects",
        "CATEGORY" to "Categories",
        "MATERIAL" to "Materials",
        "FINISH_TYPE" to "Finishes",
        "UNIT" to "Units",
        "WARRANTY" to "Warranties",
        "ACCESSORY" to "Accessories",
        "TERMS" to "Terms & Conditions",
        "DOOR_SYSTEM" to "Door Systems",
        "HARDWARE" to "Hardware Packages",
        "BRAND" to "Brands",
        "THICKNESS" to "Thicknesses",
        "COLOUR" to "Colours & Shades",
        "GST_RATE" to "GST Rates",
        "PAYMENT_TERM" to "Payment Terms",
        "APPLIANCE" to "Appliances",
        "ITEM_TEMPLATE" to "Templates",
        "MATERIAL_RULE" to "Rules"
    )"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/company/MasterDataScreen.kt', 'w') as f:
    f.write(content)
