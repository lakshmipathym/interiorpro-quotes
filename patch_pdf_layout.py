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

# 3. Wrapping for terms and conditions
# Make sure the value is properly aligned. The bullets already are aligned.
# Let's ensure the label, separator, and value columns are correctly placed.
# No changes needed here if it's already looking correct, but we'll check later.

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
