with open("app/src/main/java/com/example/ui/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

# Revert previous patch
bad_replacement = """    val companyProfile by companyViewModel.companyProfile.collectAsState()
    
    BackHandler(enabled = currentScreen != "main") {
        currentScreen = "main"
    }"""
content = content.replace(bad_replacement, "    val companyProfile by companyViewModel.companyProfile.collectAsState()")

# Apply correctly
target2 = """    var currentScreen by remember { mutableStateOf("main") }

    Scaffold("""

replacement2 = """    var currentScreen by remember { mutableStateOf("main") }

    BackHandler(enabled = currentScreen != "main") {
        currentScreen = "main"
    }

    Scaffold("""

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(content)
