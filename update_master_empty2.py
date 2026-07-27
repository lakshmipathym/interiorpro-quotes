import re

file_path = "/app/applet/app/src/main/java/com/example/ui/company/MasterDataScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

def replace_empty_state():
    global content
    idx = content.find("if (filteredMasters.isEmpty()) {")
    if idx == -1: return
    
    end_idx = content.find("} else {", idx)
    if end_idx == -1: return
    
    new_empty = """if (filteredMasters.isEmpty()) {
                com.example.ui.components.EmptyState(
                    title = if (searchQuery.isNotEmpty()) "No Matching Masters" else "No $typeLabel Configured",
                    message = if (searchQuery.isNotEmpty()) "No results match \\"$searchQuery\\". Try adjusting your search query." else "Tap the '+' button below to register your first custom $typeLabel record.",
                    icon = if (searchQuery.isNotEmpty()) Icons.Filled.SearchOff else Icons.Filled.Inbox,
                    actionLabel = if (searchQuery.isNotEmpty()) "Reset Search" else null,
                    onActionClick = if (searchQuery.isNotEmpty()) { { masterViewModel.searchQuery.value = "" } } else null,
                    modifier = Modifier.weight(1f)
                )
            """
    content = content[:idx] + new_empty + content[end_idx:]

replace_empty_state()
with open(file_path, "w") as f:
    f.write(content)
