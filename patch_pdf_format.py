import sys

file_path = "/app/applet/app/src/main/java/com/example/pdf/PdfGenerator.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace 1: Area Label
content = content.replace("""            val billableLabel = when {
                uLower.contains("sq") -> "Area :"
                uLower.contains("cu") -> "Volume :"
                uLower.contains("ft") || uLower.contains("meter") || uLower == "r.m" || uLower == "rm" -> "Length :"
                else -> ""
            }""", """            val billableLabel = when {
                uLower.contains("sq") -> "Area:"
                uLower.contains("cu") -> "Volume:"
                uLower.contains("ft") || uLower.contains("meter") || uLower == "r.m" || uLower == "rm" -> "Length:"
                else -> ""
            }""")

# Replace 2: Display Unit
content = content.replace("""            if (billableLabel.isNotEmpty()) {
                val qtyRounded = Math.round(billableQty * 100.0) / 100.0
                val qtyStr = if (qtyRounded % 1.0 == 0.0) String.format(Locale.US, "%.0f %s", qtyRounded, item.unit.trim()) else String.format(Locale.US, "%.2f %s", qtyRounded, item.unit.trim())
                
                val combinedLine = "$billableLabel $qtyStr\"""", """            if (billableLabel.isNotEmpty()) {
                val qtyRounded = Math.round(billableQty * 100.0) / 100.0
                val displayUnit = when (item.unit.trim().uppercase(Locale.US)) {
                    "SQ_FT" -> "sq.ft."
                    "CU_FT" -> "cu.ft."
                    "R.M", "RM" -> "r.m."
                    "R.FT", "RFT" -> "r.ft."
                    else -> item.unit.trim().replace("_", " ")
                }
                val qtyStr = if (qtyRounded % 1.0 == 0.0) String.format(Locale.US, "%.0f %s", qtyRounded, displayUnit) else String.format(Locale.US, "%.2f %s", qtyRounded, displayUnit)
                
                val combinedLine = "$billableLabel $qtyStr\"""")

# Replace 3: Draw Paint
content = content.replace("""                sizeLinesStr.forEach { line ->
                    if (line.isNotBlank()) {
                        val paint = if (line.contains("Area :") || line.contains("Volume :") || line.contains("Length :")) engine.getPaint(COLOR_PRIMARY_BLUE, 6f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER) else sizeValuePaint
                        canvas.drawText(line, col3CenterX, sizeY, paint)
                    }
                    sizeY += 10.5f
                }""", """                sizeLinesStr.forEach { line ->
                    if (line.isNotBlank()) {
                        val paint = if (line.contains("Area:") || line.contains("Volume:") || line.contains("Length:") || line.contains("Area :") || line.contains("Volume :") || line.contains("Length :")) engine.getPaint(COLOR_PRIMARY_BLUE, 6f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER) else sizeValuePaint
                        canvas.drawText(line, col3CenterX, sizeY, paint)
                    }
                    sizeY += 10.5f
                }""")

# Replace 4: Specs
content = content.replace("""        fun formatValue(v: String): String {
            var trimmed = v.trim()
            trimmed = trimmed.replace(Regex("([a-z])([A-Z]+)"), "$1 $2")
            
            val replacements = mapOf(""", """        fun formatValue(v: String): String {
            var trimmed = v.trim()
            trimmed = trimmed.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            trimmed = trimmed.replace(Regex("([A-Z])([A-Z][a-z])"), "$1 $2")
            trimmed = trimmed.replace(",", ", ")
            trimmed = trimmed.replace(Regex("\\s+"), " ")
            
            val replacements = mapOf(""")

with open(file_path, "w") as f:
    f.write(content)

print("Patch applied successfully.")
