import re

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'r') as f:
    lines = f.readlines()

brace_level = 0
for i, line in enumerate(lines):
    stripped = line.strip()
    if not stripped or stripped.startswith('//'):
        continue
    
    # count braces
    open_b = line.count('{')
    close_b = line.count('}')
    
    indent = len(line) - len(line.lstrip())
    
    expected_indent = brace_level * 4
    
    brace_level += open_b - close_b
    
    if indent < expected_indent and not stripped.startswith('}') and not stripped.startswith('.'):
        print(f"Line {i+1}: Indent {indent}, expected {expected_indent}. Content: {stripped}")

