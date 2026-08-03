import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# We need to find the exact start and end of wrapText
start_idx = content.find('        fun wrapText(text: String, width: Int, paint: Paint): List<String> {')

# Find the end by counting braces
idx = start_idx
brace_count = 0
found_start = False
for i in range(start_idx, len(content)):
    if content[i] == '{':
        brace_count += 1
        found_start = True
    elif content[i] == '}':
        brace_count -= 1
    
    if found_start and brace_count == 0:
        end_idx = i + 1
        break

new_wrap_text = """        fun wrapText(text: String, width: Int, paint: Paint): List<String> {
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
        }"""

content = content[:start_idx] + new_wrap_text + content[end_idx:]

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
