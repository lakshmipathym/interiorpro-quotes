file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Target block of code around lines 610 to 619
old_block = """                            quotationViewModel.saveQuotation { id ->
                                savedQuotationId = id
                            }
                        }
                    }
                 )

             // Persistent bottom navigation footer
                 }
             }"""

# Since whitespace might vary, let's search for lines 610-619 more precisely:
old_block_alt = """                            quotationViewModel.saveQuotation { id ->
                                savedQuotationId = id
                            }
                        }
                    }
                )

            // Persistent bottom navigation footer
                }
            }"""

new_block = """                            quotationViewModel.saveQuotation { id ->
                                savedQuotationId = id
                            }
                        }
                    )
            }
        }"""

replaced = False
if old_block in content:
    content = content.replace(old_block, new_block)
    replaced = True
elif old_block_alt in content:
    content = content.replace(old_block_alt, new_block)
    replaced = True
else:
    # Let's read lines 609 to 620 and replace them
    lines = content.splitlines()
    # Find lines matching 610 to 619
    for i in range(len(lines) - 10):
        if "quotationViewModel.saveQuotation { id ->" in lines[i] and "savedQuotationId = id" in lines[i+1] and "}" in lines[i+2] and "}" in lines[i+3] and "}" in lines[i+4] and ")" in lines[i+5]:
            print(f"Found match at line {i+1}")
            # Replace lines from i to i+9
            lines[i:i+10] = [
                "                            quotationViewModel.saveQuotation { id ->",
                "                                savedQuotationId = id",
                "                            }",
                "                        }",
                "                    )",
                "            }",
                "        }"
            ]
            content = "\\n".join(lines)
            replaced = True
            break

if replaced:
    print("[SUCCESS] Replaced the orphaned brace around line 614.")
else:
    print("[FAILED] Could not locate the orphaned brace block.")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Part 3 completed.")
