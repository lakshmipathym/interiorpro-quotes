import os
file_path = "/app/applet/app/src/main/java/com/example/ui/theme/Theme.kt"
with open(file_path, "r") as f:
    content = f.read()

shapes_code = """
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)
"""

if "val Shapes" not in content:
    content = content.replace("import androidx.compose.ui.platform.LocalContext", "import androidx.compose.ui.platform.LocalContext\n" + shapes_code)
    content = content.replace("typography = Typography,", "typography = Typography,\n        shapes = Shapes,")
    
with open(file_path, "w") as f:
    f.write(content)
