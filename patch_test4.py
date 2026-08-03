import re

with open('app/src/test/java/com/example/ui/history/FinalizeWorkflowTest.kt', 'r') as f:
    content = f.read()

content = content.replace("while(repository.getQuotationByIdDirect(1)?.status == \"Draft\" && retry < 50)", "while(retry < 50) {\n            if (repository.getQuotationByIdDirect(1)?.status == \"FINALIZED\") break\n            ")

with open('app/src/test/java/com/example/ui/history/FinalizeWorkflowTest.kt', 'w') as f:
    f.write(content)
