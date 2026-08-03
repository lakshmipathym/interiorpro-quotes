import re

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {", "viewModelScope.launch {")
content = content.replace("kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {", "")
content = content.replace("onComplete(newId)\n            }", "onComplete(newId)")

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'w') as f:
    f.write(content)
