import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Fix payment method height calculation
old_pay_card_h = "val payCardH = if (hasBank || hasQr) maxOf(50f, bankRowCount * 10.5f + 14f) else 0f"
new_pay_card_h = "val payCardH = if (hasBank || hasQr) maxOf(if (hasQr) 65f else 0f, 28f + bankRowCount * 10.5f) else 0f"
content = content.replace(old_pay_card_h, new_pay_card_h)

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)

