import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Replace the content of the customers route
pattern = r'composable\("customers"\)\s*\{[\s\S]*?launchSingleTop = true\s*\}\s*\}\s*\)'
replacement = '''composable("customers") {
                    CustomersScreen(
                        customerViewModel = customerViewModelProvider()
                    )'''
new_content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(new_content)
