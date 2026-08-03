sed -i '/val pageInfo = PdfDocument.PageInfo.Builder(595, 842, idx + 1).create()/d' app/src/main/java/com/example/pdf/PdfGenerator.kt
sed -i 's/val page = pdfDocument.startPage(pageInfo)//g' app/src/main/java/com/example/pdf/PdfGenerator.kt
sed -i 's/val canvas = page.canvas/val canvas = pdfDocument.beginPage(595, 842, idx + 1)/g' app/src/main/java/com/example/pdf/PdfGenerator.kt
sed -i 's/pdfDocument.finishPage(page)/pdfDocument.endPage()/g' app/src/main/java/com/example/pdf/PdfGenerator.kt
