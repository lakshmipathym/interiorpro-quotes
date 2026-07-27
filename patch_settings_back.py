with open("app/src/main/java/com/example/ui/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """    val companyProfile by companyViewModel.companyProfile.collectAsState()"""
replacement = """    val companyProfile by companyViewModel.companyProfile.collectAsState()
    
    BackHandler(enabled = currentScreen != "main") {
        currentScreen = "main"
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/settings/SettingsScreen.kt", "w") as f:
        f.write(content)
    print("Patched BackHandler successfully")
else:
    print("Target not found")
