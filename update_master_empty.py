import re

file_path = "/app/applet/app/src/main/java/com/example/ui/company/MasterDataScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

old_empty = """            // --- MASTERS LIST / EMPTY STATE ---
            if (filteredMasters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(24.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (searchQuery.isEmpty()) Icons.Filled.List else Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) "No $typeLabel Found" else "No Results Match",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isEmpty()) {
                                    "Your $typeLabel directory is currently empty. Tap the '+' button below to add your first record."
                                } else {
                                    "Try adjusting your search criteria or toggling 'Show Inactive'."
                                },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(280.dp),
                                lineHeight = 20.sp
                            )
                            if (searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { masterViewModel.searchQuery.value = "" },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reset Search")
                                }
                            }
                        }
                    }
                }
            } else {"""

new_empty = """            // --- MASTERS LIST / EMPTY STATE ---
            if (filteredMasters.isEmpty()) {
                com.example.ui.components.EmptyState(
                    title = if (searchQuery.isEmpty()) "No $typeLabel Found" else "No Results Match",
                    message = if (searchQuery.isEmpty()) {
                        "Your $typeLabel directory is currently empty. Tap the '+' button below to add your first record."
                    } else {
                        "Try adjusting your search criteria or toggling 'Show Inactive'."
                    },
                    icon = if (searchQuery.isEmpty()) Icons.AutoMirrored.Filled.List else Icons.Filled.SearchOff,
                    actionLabel = if (searchQuery.isNotEmpty()) "Reset Search" else null,
                    onActionClick = if (searchQuery.isNotEmpty()) { { masterViewModel.searchQuery.value = "" } } else null,
                    modifier = Modifier.weight(1f)
                )
            } else {"""

content = content.replace(old_empty, new_empty)

with open(file_path, "w") as f:
    f.write(content)
