import sys

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

old_onchange = "onValueChange = { grade = it }"
new_onchange = """onValueChange = { 
                        grade = it 
                        val newFinishes = getFinishesForMaterial(material, it, finishes, allMasterData)
                        if (finish.isNotBlank() && !newFinishes.contains(finish)) {
                            finish = newFinishes.firstOrNull() ?: ""
                        }
                    }"""
                    
content = content.replace(old_onchange, new_onchange)

with open(file_path, "w") as f:
    f.write(content)
print("Patched grade change logic")
