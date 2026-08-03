import re

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'r') as f:
    content = f.read()

# Replace runBlocking so it is a valid JUnit method
content = content.replace("fun testPdfCustomerSnapshotIsolation() = runBlocking {", "fun testPdfCustomerSnapshotIsolation() {\nrunBlocking {")

# Append a closing brace
content = content.replace("}\n}", "}\n}\n}")

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'w') as f:
    f.write(content)

print("Success")
