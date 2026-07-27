import re

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'r') as f:
    content = f.read()

pattern = r'val duplicatedItems = items\.map \{[\s\S]*?\}'
replacement = '''val duplicatedItems = items.map { item ->
                var duplicatedDesc = item.description
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
                
                item.copy(
                    id = 0,
                    quotationId = 0,
                    description = duplicatedDesc
                )
            }'''

new_content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'w') as f:
    f.write(new_content)
