import sys

file_path = "/app/applet/app/src/test/java/com/example/pdf/PdfDocumentTest.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("@Config(sdk = [34])", "@Config(sdk = [33])")

with open(file_path, "w") as f:
    f.write(content)
