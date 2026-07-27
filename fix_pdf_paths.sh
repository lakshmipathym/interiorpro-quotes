perl -0777 -pi -e 's/val file = File\(specs\.laminateImageUri\)/val file = File\(context\.filesDir, File\(specs\.laminateImageUri\)\.name\)/g' app/src/main/java/com/example/pdf/PdfGenerator.kt
perl -0777 -pi -e 's/val file = File\(specs\.designImageUri\)/val file = File\(context\.filesDir, File\(specs\.designImageUri\)\.name\)/g' app/src/main/java/com/example/pdf/PdfGenerator.kt
