with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "r") as f:
    content = f.read()

reorder_funcs = """
    fun moveQuoteItemUp(index: Int) {
        if (index > 0) {
            val list = _newQuoteItems.value.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _newQuoteItems.value = list
        }
    }

    fun moveQuoteItemDown(index: Int) {
        if (index < _newQuoteItems.value.size - 1) {
            val list = _newQuoteItems.value.toMutableList()
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _newQuoteItems.value = list
        }
    }
"""

if "fun moveQuoteItemUp" not in content:
    content = content.replace("    fun duplicateQuoteItem(index: Int) {", reorder_funcs + "\n    fun duplicateQuoteItem(index: Int) {")
    with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "w") as f:
        f.write(content)
