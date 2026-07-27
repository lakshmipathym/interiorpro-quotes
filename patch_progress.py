import re

with open('V1.5_REBUILD_PROGRESS.md', 'r') as f:
    content = f.read()

# Add Phase 5D status
phase_5d_status = """
## PHASE 5D STATUS
- CURRENT_PHASE: 5D
- STATUS: PHASE_5D_SNAPSHOT_PERSISTENCE_COMPLETE
- PHASE_5D_CODE_CHANGES: YES
- PHASE_5D_TESTS: PASS
- DATABASE_MIGRATION_REQUIRED: YES
- DATABASE_MIGRATION_TEST: NOT_REQUIRED
- PHASE_5D_UNRESOLVED_RULES: 0
- NEXT_PHASE: 6
- Status: WAITING FOR USER COMMAND: CONTINUE
"""

if "## PHASE 5D STATUS" not in content:
    content += "\n" + phase_5d_status

with open('V1.5_REBUILD_PROGRESS.md', 'w') as f:
    f.write(content)
