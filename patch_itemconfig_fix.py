import re

file_path = "/app/applet/app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("    } actions = {", "    }, actions = {")

# Also fix the invocation syntax for trailing closures if needed.
# It should be: PremiumDialog(..., actions = { ... }) { ... }
pattern = re.compile(r"(com\.example\.ui\.components\.PremiumDialog\(\n.*?)(\} \}, actions = \{)(.*?^\}    \})", re.MULTILINE | re.DOTALL)
# Actually let's just do a manual replace for the specific block.
