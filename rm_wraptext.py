import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Remove the old wrapText from PdfEngine
start_idx = content.find('fun wrapText(text: String, width: Int, paint: Paint): List<String> {', content.find('class PdfEngine'))
if start_idx != -1:
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
            
    content = content[:start_idx] + content[end_idx:]

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
