import sys

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

bad_string = """                    profileSeries = ""
                    profileColour = ""
                    glassType = ""
                    glassThickness = ""
                    acpColour = ""
                    cncDesign = ""
                }
            }
            )"""

good_string = """                    profileSeries = ""
                    profileColour = ""
                    glassType = ""
                    glassThickness = ""
                    acpColour = ""
                    cncDesign = ""
                }
            )"""

if bad_string in content:
    content = content.replace(bad_string, good_string)
    with open(file_path, "w") as f:
        f.write(content)
    print("Fixed syntax")
else:
    print("Could not find bad string")
