with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt') as f:
    text = f.read()

count = 0
for i, char in enumerate(text):
    if char == '{':
        count += 1
    elif char == '}':
        count -= 1
    
    if count < 0:
        print(f"Extra closing brace at index {i}")
        break

print(f"Final count: {count}")
