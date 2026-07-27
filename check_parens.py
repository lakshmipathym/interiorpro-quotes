import os
import re

ui_dir = "app/src/main/java/com/example/ui"
files_to_fix = []

for root, _, files in os.walk(ui_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r") as f:
                content = f.read()
            
            # Find specific known broken patterns from `)),` -> `),`
            # 1. verticalScroll(rememberScrollState(),
            content = content.replace('verticalScroll(rememberScrollState(),', 'verticalScroll(rememberScrollState()),')
            
            # 2. padding(PaddingValues(...,
            content = re.sub(r'padding\(PaddingValues\(([^)]+)\),', r'padding(PaddingValues(\1)),', content)
            
            # 3. keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
            # Wait, KeyboardType.Number doesn't have `()`. KeyboardOptions(...) has one `)`.
            
            # 4. Icon(imageVector = Icons.Default.Something,
            # Wait, `Icon(...)` has one `)`.
            
            # What else? Let's look at `Arrangement.spacedBy(...)`.
            # `verticalArrangement = Arrangement.spacedBy(16.dp),` - one `)`
            
            # Let's see what else was broken.
            with open(path, "w") as f:
                f.write(content)
