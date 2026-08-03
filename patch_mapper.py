import re

with open('app/src/main/java/com/example/data/snapshot/QuotationSnapshotMapper.kt', 'r') as f:
    content = f.read()

content = content.replace('status = "Draft",', 'status = "FINALIZED",')

with open('app/src/main/java/com/example/data/snapshot/QuotationSnapshotMapper.kt', 'w') as f:
    f.write(content)
