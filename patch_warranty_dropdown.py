import re
file_path = "/app/applet/app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace first Warranty Dropdown
w_old_1 = """                // Warranty dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = warranty.ifEmpty { "Select warranty limit" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Warranty") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedWarranty = true }
                    )
                    DropdownMenu(expanded = expandedWarranty, onDismissRequest = { expandedWarranty = false }) {
                        val displayWarranties = if (masterWarranties.isEmpty()) listOf("1 Year Warranty", "3 Years Warranty", "5 Years Warranty", "No Warranty") else masterWarranties
                        displayWarranties.forEach { w ->
                            DropdownMenuItem(text = { Text(w) }, onClick = {
                                onWarrantyChange(w)
                                expandedWarranty = false
                            })
                        }
                    }
                }"""

w_new_1 = """                // Warranty dropdown
                com.example.ui.components.PremiumDropdown(
                    value = warranty.ifEmpty { "Select warranty limit" },
                    onValueChange = onWarrantyChange,
                    label = "Warranty",
                    options = if (masterWarranties.isEmpty()) listOf("1 Year Warranty", "3 Years Warranty", "5 Years Warranty", "No Warranty") else masterWarranties,
                    modifier = Modifier.fillMaxWidth()
                )"""

w_old_2 = """                Box(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = warranty.ifEmpty { "Select warranty limit" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Warranty") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedWarranty = true }
                    )
                    DropdownMenu(
                        expanded = expandedWarranty,
                        onDismissRequest = { expandedWarranty = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        listOf("1 Year Warranty", "3 Years Warranty", "5 Years Warranty", "10 Years Warranty", "No Warranty").forEach { w ->
                            DropdownMenuItem(text = { Text(w) }, onClick = {
                                onWarrantyChange(w)
                                expandedWarranty = false
                            })
                        }
                    }
                }"""
                
w_new_2 = """                com.example.ui.components.PremiumDropdown(
                    value = warranty.ifEmpty { "Select warranty limit" },
                    onValueChange = onWarrantyChange,
                    label = "Warranty",
                    options = listOf("1 Year Warranty", "3 Years Warranty", "5 Years Warranty", "10 Years Warranty", "No Warranty"),
                    modifier = Modifier.fillMaxWidth()
                )"""
                
content = content.replace(w_old_1, w_new_1)
content = content.replace(w_old_2, w_new_2)

with open(file_path, "w") as f:
    f.write(content)
