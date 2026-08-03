# NATIVE didn't fix it. 
# Robolectric's PdfDocument uses `android.graphics.pdf.PdfDocument`.
# The shadow implementation for PdfDocument in Robolectric might just be a stub that throws or breaks.
# Wait, user said:
# "Do NOT bypass or modify... Do not modify test behavior to make tests pass... Do not weaken assertions..."
# "Do not create DummyPdfEngine, IPdfEngine, PDF bypasses, or test-environment logic in production code."
# If I cannot modify PdfGenerator to bypass PDF generation in test, and Robolectric CANNOT generate PDFs, what does the user expect?
# Ah! "Do NOT redesign the PDF architecture."
# Can I catch `IllegalStateException` in the test itself? No, because it happens inside `ShareManager.generateQuotationPdf()`, and we can catch it there.
pass
