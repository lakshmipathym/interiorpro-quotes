import sys

with open("app/src/test/java/com/example/domain/usecases/FinalizeQuotationUseCaseTest.kt", "r") as f:
    content = f.read()

content = content.replace("\\`", "`")

with open("app/src/test/java/com/example/domain/usecases/FinalizeQuotationUseCaseTest.kt", "w") as f:
    f.write(content)

