sed -i -e 's/engine.ensureSpace(totalsBoxH + sectionSpacing, reserveHeader = true)/engine.ensureSpace(totalsBoxH, reserveHeader = true)/g' app/src/main/java/com/example/pdf/PdfGenerator.kt
sed -i -e 's/engine.currentY += totalsBoxH + 8f/engine.currentY += totalsBoxH/g' app/src/main/java/com/example/pdf/PdfGenerator.kt
sed -i -e 's/engine.ensureSpace(wordsH + sectionSpacing, reserveHeader = true)/engine.ensureSpace(wordsH, reserveHeader = true)/g' app/src/main/java/com/example/pdf/PdfGenerator.kt
sed -i -e 's/engine.currentY += wordsH + sectionSpacing/engine.currentY += wordsH + 8f + sectionSpacing/g' app/src/main/java/com/example/pdf/PdfGenerator.kt
