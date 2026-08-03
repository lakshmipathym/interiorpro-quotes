import re

with open('app/src/test/java/com/example/ui/history/FinalizeWorkflowTest.kt', 'r') as f:
    content = f.read()

content = content.replace("org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle()", 
"org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()\n        kotlinx.coroutines.delay(100)")

with open('app/src/test/java/com/example/ui/history/FinalizeWorkflowTest.kt', 'w') as f:
    f.write(content)
