import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# I will write a custom replace for terms and conditions to chunk them if they are too large
