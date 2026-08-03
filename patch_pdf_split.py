import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Let's find the table drawing logic for items
# It starts at: items.forEachIndexed { index, item ->
# And ends at the end of the forEachIndexed block.
