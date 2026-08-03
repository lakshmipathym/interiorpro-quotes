import re

with open('app/src/test/java/com/example/ui/history/FinalizeWorkflowTest.kt', 'r') as f:
    content = f.read()

content = content.replace("org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle()", 
"var retry = 0\n        while(repository.getQuotationByIdDirect(1)?.status == \"Draft\" && retry < 50) {\n            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()\n            Thread.sleep(100)\n            retry++\n        }")

with open('app/src/test/java/com/example/ui/history/FinalizeWorkflowTest.kt', 'w') as f:
    f.write(content)
