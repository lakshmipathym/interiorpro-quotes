import os
import sys

def check_file(path):
    with open(path, 'r') as f:
        lines = f.readlines()
        
    balance = 0
    for i, line in enumerate(lines):
        for char in line:
            if char == '(':
                balance += 1
            elif char == ')':
                balance -= 1
            
            if balance < 0:
                print(f"{path}:{i+1} Unbalanced closing parenthesis! {line.strip()}")
                balance = 0
                
    if balance != 0:
        print(f"{path}: Final balance = {balance} (Missing closing parens!)")
        
ui_dir = "app/src/main/java/com/example/ui"
for root, _, files in os.walk(ui_dir):
    for file in files:
        if file.endswith(".kt"):
            check_file(os.path.join(root, file))

