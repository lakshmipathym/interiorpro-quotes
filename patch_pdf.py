import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# 1. Update billableLabel in drawItemsTable
content = re.sub(
    r'val billableLabel = when \{.*?\n\s+else -> ""\n\s+\}',
    '''val billableLabel = when {
                uLower.contains("sq") -> "Billable Area :"
                uLower.contains("cu") -> "Billable Volume :"
                uLower.contains("ft") || uLower.contains("meter") -> "Running Length :"
                else -> ""
            }''',
    content,
    flags=re.DOTALL
)

# 2. Terms and Conditions formatting and removing 80f extra space
content = re.sub(
    r'val isLast = index == wrappedTerms\.lastIndex\n\s+val extraSpace = if \(isLast\) 80f else 0f\n\s+engine\.ensureSpace\(wt\.itemH \+ termSpacing \+ extraSpace, reserveHeader = true\)',
    'engine.ensureSpace(wt.itemH + termSpacing, reserveHeader = true)',
    content,
    flags=re.DOTALL
)

# 3. Fix Payment Method width and spacing
content = re.sub(
    r'val leftX = engine\.marginX\n\s+val leftW = 245f',
    '''val leftX = engine.marginX
        val hasQrWidth = showQrCode && company.upiId.trim().isNotBlank()
        val leftW = if (hasQrWidth) 245f else 170f''',
    content
)

# 4. Fix Reference Images pagination
ref_images_old = '''        var currentImgIdx = 0
        var isFirstTitle = true
        while (currentImgIdx < validImages.size) {
            val titleHeight = if (isFirstTitle) 30f else 0f
            val requiredHeight = titleHeight + cardH
            // If we can't even fit one row, start a new page
            if (engine.currentY + requiredHeight > engine.maxContentY) {
                engine.startNewPage(reserveHeader = true)
            }
            if (isFirstTitle || engine.currentY == engine.topMargin) {'''

ref_images_new = '''        var currentImgIdx = 0
        var isFirstTitle = true
        var lastPageIndex = -1
        while (currentImgIdx < validImages.size) {
            val titleHeight = if (isFirstTitle) 30f else 0f
            val requiredHeight = titleHeight + cardH
            // If we can't even fit one row, start a new page
            if (engine.currentY + requiredHeight > engine.maxContentY) {
                engine.startNewPage(reserveHeader = true)
            }
            if (isFirstTitle || engine.currentPageIndex != lastPageIndex) {'''

content = content.replace(ref_images_old, ref_images_new)

content = content.replace(
    '''                isFirstTitle = false
            }
            val startY = engine.currentY''',
    '''                isFirstTitle = false
                lastPageIndex = engine.currentPageIndex
            }
            val startY = engine.currentY'''
)

# 5. Fix Footer Business Name
content = content.replace(
    '"Powered by InteriorPro Technologies"',
    'company.companyName.ifBlank { "Quotation" }'
)

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)

