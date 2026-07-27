import sys

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'r') as f:
    content = f.read()

# First replacement
target1_start = "                            val (userDesc, specs) = parseItemSpecs(item.description)\n                            ElevatedCard(\n                                modifier = Modifier.fillMaxWidth(),"
target1_end = "                                        }\n                                    }\n                                }\n                            }\n"
start_idx = content.find(target1_start)
end_idx = content.find(target1_end, start_idx) + len(target1_end)

if start_idx != -1 and end_idx != -1:
    replacement1 = """                            QuotationItemCard(
                                item = item,
                                index = index,
                                onEdit = {
                                    editingItemIndex = index
                                    showItemConfigDialog = true
                                },
                                onDuplicate = { quotationViewModel.duplicateQuoteItem(index) },
                                onDelete = { quotationViewModel.removeQuoteItem(index) }
                            )\n"""
    content = content[:start_idx] + replacement1 + content[end_idx:]

# Second replacement
target2_start = "                    val (userDesc, specs) = parseItemSpecs(item.description)\n                    \n                    ElevatedCard(\n                        modifier = Modifier\n                            .animateItem()"
target2_end = "                                    }\n                                }\n                            }\n                        }\n                    }\n"
start_idx2 = content.find(target2_start)
if start_idx2 != -1:
    end_idx2 = content.find(target2_end, start_idx2) + len(target2_end) - len("                    }\n") # don't remove the outermost ReorderableItem block

    replacement2 = """                    QuotationItemCard(
                        item = item,
                        index = index,
                        modifier = Modifier.animateItem(),
                        onEdit = {
                            editingItemIndex = index
                            showItemConfigDialog = true
                        },
                        onDelete = { quotationViewModel.removeQuoteItem(index) }
                    )\n"""
    content = content[:start_idx2] + replacement2 + content[end_idx2:]

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'w') as f:
    f.write(content)
