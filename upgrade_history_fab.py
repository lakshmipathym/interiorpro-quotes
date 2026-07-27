import re

file_path = "/app/applet/app/src/main/java/com/example/ui/history/QuotationHistoryScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

old_fab = """        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    quotationViewModel.startNewQuotation()
                    onNavigateToCreate()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_new_quote")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Quotation")
            }
        }"""

new_fab = """        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    quotationViewModel.startNewQuotation()
                    onNavigateToCreate()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_new_quote"),
                text = { Text("New Quote", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Add, contentDescription = "New Quotation") }
            )
        }"""

content = content.replace(old_fab, new_fab)
with open(file_path, "w") as f:
    f.write(content)
