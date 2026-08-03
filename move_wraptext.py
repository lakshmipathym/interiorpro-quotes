import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Make wrapText accessible by putting it directly in PdfGenerator object
# Wait, it uses wrapTextCache which is in PdfEngine.
# Let's move wrapTextCache and wrapText to PdfGenerator object.

# Find object PdfGenerator {
obj_idx = content.find('object PdfGenerator {')
if obj_idx != -1:
    insert_idx = content.find('\n', obj_idx) + 1
    
    # We will just redefine wrapText and wrapTextCache here
    cache_and_func = """
    private val wrapTextCache = mutableMapOf<String, List<String>>()
    
    fun wrapText(text: String, width: Int, paint: Paint): List<String> {
        if (text.isEmpty()) return emptyList()
        val key = "$text:$width:${paint.textSize}:${paint.typeface?.hashCode()}"
        return wrapTextCache.getOrPut(key) {
            val lines = mutableListOf<String>()
            val explicitLines = text.split("\\n")
            for (explicitLine in explicitLines) {
                if (explicitLine.isEmpty()) {
                    lines.add("")
                    continue
                }
                val words = explicitLine.split(" ")
                var currentLine = ""
                for (word in words) {
                    if (currentLine.isEmpty()) {
                        currentLine = word
                    } else {
                        val testLine = "$currentLine $word"
                        if (paint.measureText(testLine) <= width) {
                            currentLine = testLine
                        } else {
                            lines.add(currentLine)
                            currentLine = word
                        }
                    }
                }
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
            }
            lines
        }
    }
"""
    content = content[:insert_idx] + cache_and_func + content[insert_idx:]

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
