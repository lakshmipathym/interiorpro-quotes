import sys

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

old_chip = "onClick = { grade = opt },"
# Only replace the one under gradeOptions
new_chip = """onClick = { 
                                            grade = opt 
                                            val newFinishes = getFinishesForMaterial(material, opt, finishes, allMasterData)
                                            if (finish.isNotBlank() && !newFinishes.contains(finish)) {
                                                finish = newFinishes.firstOrNull() ?: ""
                                            }
                                        },"""

# we need to be careful as Finish chip also has `onClick = { finish = opt }` but we are matching `grade = opt`.
content = content.replace(old_chip, new_chip)

with open(file_path, "w") as f:
    f.write(content)
print("Patched grade chip")
