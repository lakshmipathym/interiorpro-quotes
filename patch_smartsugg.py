import re
file_path = "/app/applet/app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

replacement = """    com.example.ui.components.PremiumDialog(
        onDismissRequest = onDismiss,
        title = "Smart Item Suggestions",
        actions = {
            com.example.ui.components.PremiumTextButton(
                text = "Cancel", 
                onClick = onDismiss
            )
            Spacer(modifier = Modifier.width(8.dp))
            com.example.ui.components.PremiumPrimaryButton(
                text = "Add Selected",
                onClick = {
                    val finalItems = mutableListOf<QuotationItem>()
                    suggestions.forEachIndexed { idx, item ->
                        if (selectedItemsMap[idx] == true) {
                            val qty = qtyMap[idx]?.toDoubleOrNull() ?: item.quantity
                            val rate = rateMap[idx]?.toDoubleOrNull() ?: item.rate
                            finalItems.add(
                                item.copy(
                                    quantity = qty,
                                    rate = rate,
                                    amount = qty * rate
                                )
                            )
                        }
                    }
                    onAddItems(finalItems)
                    onDismiss()
                }
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Standard item presets based on selected configuration. Check items to add and adjust Qty/Rate as needed:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions.size) { idx ->
                    val item = suggestions[idx]
                    val isSelected = selectedItemsMap[idx] ?: false
                    
                    com.example.ui.components.PremiumCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedItemsMap[idx] = !isSelected }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { selectedItemsMap[idx] = it }
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                val (cleanDesc, _) = parseItemSpecs(item.description)
                                Text(cleanDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        com.example.ui.components.PremiumTextField(
                                            value = qtyMap[idx] ?: "",
                                            onValueChange = { qtyMap[idx] = it },
                                            label = "Qty",
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                        com.example.ui.components.PremiumTextField(
                                            value = rateMap[idx] ?: "",
                                            onValueChange = { rateMap[idx] = it },
                                            label = "Rate",
                                            modifier = Modifier.weight(1.5f),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }"""

pattern = re.compile(r"    AlertDialog\(\n        onDismissRequest = onDismiss,\n        title = \{ Text\(\"Smart Item Suggestions\"\) \},.*?        dismissButton = \{\n            TextButton\(onClick = onDismiss\) \{\n                Text\(\"Cancel\"\)\n            \}\n        \}\n    \)", re.MULTILINE | re.DOTALL)

match = pattern.search(content)
if match:
    content = content[:match.start()] + replacement + content[match.end():]
else:
    print("Could not match SmartSuggestionsDialog")

with open(file_path, "w") as f:
    f.write(content)
