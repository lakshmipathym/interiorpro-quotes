import sys

with open("app/src/test/java/com/example/domain/usecases/CalculateQuotationUseCaseTest.kt", "r") as f:
    content = f.read()

content = content.replace("\\`", "`")

with open("app/src/test/java/com/example/domain/usecases/CalculateQuotationUseCaseTest.kt", "w") as f:
    f.write(content)

