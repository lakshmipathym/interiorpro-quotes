import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# We can replace the `items.forEachIndexed` to first flatten everything into lines, but that changes spacing.
