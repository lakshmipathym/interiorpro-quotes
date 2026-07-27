import re

file_path = "/app/applet/app/src/main/java/com/example/ui/history/QuotationHistoryScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

def fix_empty1():
    global content
    idx = content.find("if (quotations.isEmpty()) {")
    if idx == -1: return
    
    end_idx = content.find("} else {", idx)
    if end_idx == -1: return
    
    new_empty = """if (quotations.isEmpty()) {
                        item {
                            com.example.ui.components.EmptyState(
                                title = "No Quotations Yet",
                                message = "You haven't created any quotations. Tap the 'New Quotation' button below to get started.",
                                icon = Icons.Filled.History,
                                actionLabel = "Create Quotation",
                                onActionClick = { 
                                    quotationViewModel.startNewQuotation()
                                    onNavigateToCreate()
                                }
                            )
                        }
                    """
    content = content[:idx] + new_empty + content[end_idx:]

def fix_empty2():
    global content
    idx = content.find("if (filteredAndSortedQuotes.isEmpty()) {")
    if idx == -1: return
    
    end_idx = content.find("} else {", idx)
    if end_idx == -1: return
    
    new_empty = """if (filteredAndSortedQuotes.isEmpty()) {
                        item {
                            com.example.ui.components.EmptyState(
                                title = "No matching quotations found",
                                message = "Try a different search query or filter by a different status.",
                                icon = Icons.Filled.SearchOff,
                                actionLabel = "Reset Search",
                                onActionClick = {
                                    searchQuery = ""
                                    selectedStatusFilter = "All"
                                }
                            )
                        }
                    """
    content = content[:idx] + new_empty + content[end_idx:]

fix_empty1()
fix_empty2()
with open(file_path, "w") as f:
    f.write(content)
