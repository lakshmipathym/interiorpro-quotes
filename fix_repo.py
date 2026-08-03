with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("getCustomerByIdDirect(", "getCustomerById(")

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'w') as f:
    f.write(content)
print("Fixed repo method")
