import re

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'r') as f:
    content = f.read()

pattern1 = r"INSERT INTO quotation_template \(name, projectType, category, material, finish, description, itemsJson\)(\s*)VALUES \('Standard Modular Kitchen', 'Modular Kitchen', 'Base Cabinets', 'BWP Plywood', 'Matte Laminate', 'Standard L-Shape modular kitchen package', '\$standardKitchenItemsJson'\)"

replacement1 = r"INSERT INTO quotation_template (name, projectType, category, material, finish, rawWidth, rawHeight, rawDepth, parsedWidth, parsedHeight, parsedDepth, rawQuantity, billableQuantity, description, itemsJson)\1VALUES ('Standard Modular Kitchen', 'Modular Kitchen', 'Base Cabinets', 'BWP Plywood', 'Matte Laminate', '', '', '', 0.0, 0.0, 0.0, 0.0, 0.0, 'Standard L-Shape modular kitchen package', '$standardKitchenItemsJson')"

pattern2 = r"INSERT INTO quotation_template \(name, projectType, category, material, finish, description, itemsJson\)(\s*)VALUES \('Premium Sliding Wardrobe', 'Wardrobe', 'Shutters', 'BWP Plywood', 'Acrylic Finish', 'Premium wardrobe with soft-close sliding doors', '\$standardWardrobeItemsJson'\)"

replacement2 = r"INSERT INTO quotation_template (name, projectType, category, material, finish, rawWidth, rawHeight, rawDepth, parsedWidth, parsedHeight, parsedDepth, rawQuantity, billableQuantity, description, itemsJson)\1VALUES ('Premium Sliding Wardrobe', 'Wardrobe', 'Shutters', 'BWP Plywood', 'Acrylic Finish', '', '', '', 0.0, 0.0, 0.0, 0.0, 0.0, 'Premium wardrobe with soft-close sliding doors', '$standardWardrobeItemsJson')"

new_content = re.sub(pattern1, replacement1, content)
new_content = re.sub(pattern2, replacement2, new_content)

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'w') as f:
    f.write(new_content)
