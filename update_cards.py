import os
import re

ui_dir = "app/src/main/java/com/example/ui"

for root, _, files in os.walk(ui_dir):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r") as f:
                content = f.read()
            
            # Replace Card( with ElevatedCard(
            # but avoid ElevatedElevatedCard or something
            new_content = re.sub(r'\b(?<!Elevated)Card\(', 'ElevatedCard(', content)
            new_content = re.sub(r'\bCardDefaults\.cardColors\(', 'CardDefaults.elevatedCardColors(', new_content)
            new_content = re.sub(r'\bCardDefaults\.cardElevation\(', 'CardDefaults.elevatedCardElevation(', new_content)
            
            if content != new_content:
                if "import androidx.compose.material3.ElevatedCard" not in new_content and "import androidx.compose.material3.*" not in new_content:
                    new_content = new_content.replace("import androidx.compose.material3.Card", "import androidx.compose.material3.ElevatedCard\nimport androidx.compose.material3.Card")
                with open(path, "w") as f:
                    f.write(new_content)
                print(f"Updated Cards in {path}")
