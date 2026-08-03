import re

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'r') as f:
    content = f.read()

content = content.replace("import com.example.domain.models.*", "import com.example.domain.models.*\nimport com.example.pdf.PdfGenerator")

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'w') as f:
    f.write(content)

