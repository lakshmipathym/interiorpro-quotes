import re

file_path = "/app/applet/app/src/main/java/com/example/ui/client/ClientsScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

old_fab = """        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEditDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Client")
            }
        }"""

new_fab = """        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddEditDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                text = { Text("New Client", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add Client") }
            )
        }"""

content = content.replace(old_fab, new_fab)
with open(file_path, "w") as f:
    f.write(content)
