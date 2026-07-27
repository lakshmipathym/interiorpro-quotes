with open("app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt", "r") as f:
    content = f.read()

target = """            // --- SCOPE & MATERIAL ---
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = "Material & Scope", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    com.example.ui.components.PremiumDropdown(
                        value = currentProjectType.ifEmpty { "Select Product Type" },
                        onValueChange = { quotationViewModel.selectProjectType(it) },
                        label = "Product Type *",
                        options = projectTypes,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    com.example.ui.components.PremiumDropdown(
                        value = currentCategory.ifEmpty { "Select Category" },
                        onValueChange = { quotationViewModel.selectCategory(it) },
                        label = "Project Category *",
                        options = categories,
                        modifier = Modifier.fillMaxWidth()
                    )

                    com.example.ui.components.PremiumDropdown(
                        value = currentMaterial.ifEmpty { "Select Main Material" },
                        onValueChange = { quotationViewModel.selectMaterial(it) },
                        label = "Main Material *",
                        options = materials,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    com.example.ui.components.PremiumDropdown(
                        value = currentFinish.ifEmpty { "Select Main Finish" },
                        onValueChange = { quotationViewModel.selectFinish(it) },
                        label = "Main Finish *",
                        options = finishes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }"""

if target in content:
    content = content.replace(target, "")
    with open("app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt", "w") as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
