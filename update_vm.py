import re

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'r') as f:
    content = f.read()

pattern = r'fun duplicateQuoteItem\(index: Int\) \{[\s\S]*?\}'
replacement = '''fun duplicateQuoteItem(index: Int) {
        val current = _newQuoteItems.value.toMutableList()
        if (index in current.indices) {
            val original = current[index]
            var duplicatedDesc = original.description
            try {
                if (duplicatedDesc.startsWith("{") && duplicatedDesc.endsWith("}")) {
                    val json = org.json.JSONObject(duplicatedDesc)
                    
                    val laminateImageUri = json.optString("laminateImageUri", "")
                    if (laminateImageUri.isNotEmpty()) {
                        val oldFile = java.io.File(laminateImageUri)
                        if (oldFile.exists()) {
                            val newFile = java.io.File(oldFile.parent, "temp_lam_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().substring(0,5)}.jpg")
                            oldFile.copyTo(newFile, overwrite = true)
                            json.put("laminateImageUri", newFile.absolutePath)
                        }
                    }
                    
                    val designImageUri = json.optString("designImageUri", "")
                    if (designImageUri.isNotEmpty()) {
                        val oldFile = java.io.File(designImageUri)
                        if (oldFile.exists()) {
                            val newFile = java.io.File(oldFile.parent, "temp_des_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().substring(0,5)}.jpg")
                            oldFile.copyTo(newFile, overwrite = true)
                            json.put("designImageUri", newFile.absolutePath)
                        }
                    }
                    
                    duplicatedDesc = json.toString()
                }
            } catch (e: Exception) {
                // Ignore
            }
            val duplicated = original.copy(id = 0, description = duplicatedDesc)
            current.add(index + 1, duplicated)
            _newQuoteItems.value = current
        }
    }'''

new_content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'w') as f:
    f.write(new_content)
