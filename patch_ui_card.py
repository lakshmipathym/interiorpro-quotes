with open("app/src/main/java/com/example/ui/components/UIComponents.kt", "r") as f:
    content = f.read()

content = content.replace("Card(\n            modifier = modifier", "ElevatedCard(\n            modifier = modifier")
content = content.replace("CardDefaults.cardColors", "CardDefaults.elevatedCardColors")
content = content.replace("CardDefaults.cardElevation(defaultElevation = Elevation.Small)", "CardDefaults.elevatedCardElevation(defaultElevation = Elevation.Medium)")

with open("app/src/main/java/com/example/ui/components/UIComponents.kt", "w") as f:
    f.write(content)
