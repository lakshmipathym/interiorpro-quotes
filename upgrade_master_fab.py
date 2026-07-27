import re

file_path = "/app/applet/app/src/main/java/com/example/ui/company/MasterDataScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

old_fab = """        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEditDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add $typeLabel")
            }
        }"""

new_fab = """        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddEditDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                text = { Text("New $typeLabel", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add $typeLabel") }
            )
        }"""

content = content.replace(old_fab, new_fab)
with open(file_path, "w") as f:
    f.write(content)
