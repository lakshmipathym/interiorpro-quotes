import re
v = "CableManager,LEDProfile,WallHanging Brackets"
formatted = re.sub(r'([a-z])([A-Z])', r'\1 \2', v)
formatted = re.sub(r'([A-Z])([A-Z][a-z])', r'\1 \2', formatted)
formatted = formatted.replace(',', ', ')
formatted = re.sub(r'\s+', ' ', formatted).strip()
print(formatted)
