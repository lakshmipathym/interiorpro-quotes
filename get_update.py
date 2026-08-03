with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'r') as f:
    lines = f.readlines()

in_func = False
for line in lines:
    if "fun updateQuotationStatus" in line:
        in_func = True
    if in_func:
        print(line, end='')
        if line.strip() == "}" and "        }" not in line and "    }" not in line: # super hacky
            pass # just wait for next fun
    if "fun duplicateQuotation" in line:
        in_func = False
