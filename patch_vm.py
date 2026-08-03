import re

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("e.printStackTrace()\\n                    println(\\"Finalize Error: ${e.message}\\")", "throw e")
content = content.replace("e.printStackTrace()\n                    println(\"Finalize Error: ${e.message}\")", "throw e")
content = content.replace("// Do not update status on failure", "throw e")

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'w') as f:
    f.write(content)
