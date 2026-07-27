with open("V1.5_REBUILD_PROGRESS.md", "r") as f:
    content = f.read()

import re

# Remove 6A
content = re.sub(r'## PHASE 6A STATUS.*?CONTINUE\n\n', '', content, flags=re.DOTALL)

# Add 6A at the end, replacing NEXT_PHASE: 6 in 5D
content = content.replace("- NEXT_PHASE: 6\n- Status: WAITING FOR USER COMMAND: CONTINUE", "- NEXT_PHASE: 6A\n- Status: WAITING FOR USER COMMAND: CONTINUE\n\n## PHASE 6A STATUS\n- CURRENT_PHASE: 6A\n- STATUS: PHASE_6A_DOMAIN_USE_CASES_COMPLETE\n- PHASE_6A_CODE_CHANGES: YES\n- PHASE_6A_TESTS: PASS\n- NEXT_PHASE: 6B\n- Status: WAITING FOR USER COMMAND: CONTINUE\n")

with open("V1.5_REBUILD_PROGRESS.md", "w") as f:
    f.write(content)
