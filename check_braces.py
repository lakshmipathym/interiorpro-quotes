with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Let's count braces for the whole file
brace_count = 0
for i, c in enumerate(content):
    if c == '{': brace_count += 1
    elif c == '}': brace_count -= 1
    if brace_count < 0:
        print(f"Negative brace count at {i}")
        break
print(f"Final brace count: {brace_count}")
