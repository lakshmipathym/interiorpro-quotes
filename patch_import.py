with open("app/src/main/java/com/example/ui/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

import_statement = "import androidx.activity.compose.BackHandler\n"

if import_statement not in content:
    content = content.replace("import androidx.compose.ui.Alignment\n", "import androidx.compose.ui.Alignment\n" + import_statement)
    with open("app/src/main/java/com/example/ui/settings/SettingsScreen.kt", "w") as f:
        f.write(content)
