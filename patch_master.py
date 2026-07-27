import re
file_path = "/app/applet/app/src/main/java/com/example/ui/company/MasterDataScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace OutlinedTextField with PremiumTextField
content = re.sub(r'OutlinedTextField\(', 'com.example.ui.components.PremiumTextField(', content)

# Replace Dialog
# Before:
'''    if (showAddEditDialog) {
        Dialog(onDismissRequest = { showAddEditDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    Text(
                        text = if (editingMaster != null) "Edit $typeLabel" else "Add to $typeLabel",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
'''
# I will just write a specific replacement for the dialogs.

with open(file_path, "w") as f:
    f.write(content)
