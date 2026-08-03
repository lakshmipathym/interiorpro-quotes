import sys

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace('-> "Profile Type *"', '-> "Profile Type"')
content = content.replace('-> "Glass Type *"', '-> "Glass Type"')
content = content.replace('-> "Grade *"', '-> "Grade"')

content = content.replace('label = "Finish Type *"', 'label = "Finish Type"')

with open(file_path, "w") as f:
    f.write(content)
print("Patched labels")
