with open("app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt", "r") as f:
    content = f.read()

content = content.replace("IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp))", "IconButton(onClick = onMoveUp, modifier = Modifier.size(48.dp))")
content = content.replace("IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp))", "IconButton(onClick = onMoveDown, modifier = Modifier.size(48.dp))")
content = content.replace("IconButton(onClick = onEdit, modifier = Modifier.size(36.dp))", "IconButton(onClick = onEdit, modifier = Modifier.size(48.dp))")
content = content.replace("IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp))", "IconButton(onClick = onDuplicate, modifier = Modifier.size(48.dp))")
content = content.replace("IconButton(onClick = onDelete, modifier = Modifier.size(36.dp))", "IconButton(onClick = onDelete, modifier = Modifier.size(48.dp))")

# Also MasterDataScreen
with open("app/src/main/java/com/example/ui/company/MasterDataScreen.kt", "r") as f:
    master_content = f.read()
    
master_content = master_content.replace("IconButton(onClick = onEdit, modifier = Modifier.size(36.dp))", "IconButton(onClick = onEdit, modifier = Modifier.size(48.dp))")
master_content = master_content.replace("IconButton(onClick = onDelete, modifier = Modifier.size(36.dp))", "IconButton(onClick = onDelete, modifier = Modifier.size(48.dp))")

with open("app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/company/MasterDataScreen.kt", "w") as f:
    f.write(master_content)
    
