import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Replace drawSignature function entirely
old_draw_signature_pattern = re.compile(r'    private fun drawSignature\(.*?engine\.currentY \+= signatureBoxH \+ sectionSpacing\n    \}', re.DOTALL)

new_signature = """    private fun drawSignature(
        engine: PdfEngine,
        company: CompanyProfile,
        showCompanySeal: Boolean,
        showSignature: Boolean
    ) {
        val sealPath = company.companySealPath
        val sigPath = company.signaturePath
        val hasSeal = showCompanySeal && sealPath.isNotBlank() && File(sealPath).exists()
        val hasSig = showSignature && sigPath.isNotBlank() && File(sigPath).exists()

        val sectionSpacing = 20f
        val signatureBoxH = 80f // Increased height slightly
        engine.ensureSpace(signatureBoxH + sectionSpacing, reserveHeader = true)
        val sigY = engine.currentY + sectionSpacing

        engine.addCommand { canvas, _, _ ->
            val linePaint = engine.getPaint(COLOR_BORDER, strokeWidth = 0.75f, style = Paint.Style.STROKE)

            // --- Left Column (Column 1): Company Seal ---
            val sealCenterX = engine.marginX + 75f
            if (hasSeal) {
                // Seal is 40x40. Draw centered horizontally on sealCenterX
                drawBitmapSafely(engine, canvas, sealPath, sealCenterX - 20f, sigY + 15f, 40f, 40f)
            }
            // Label centered below seal
            canvas.drawText("COMPANY SEAL", sealCenterX, sigY + 70f, engine.getPaint(COLOR_TEXT_SECONDARY, 6.5f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER))

            // --- Right Column (Column 2): Signature & Signatory Name ---
            val authCenterX = engine.endX - 85f
            
            // "Authorized Signature" text
            canvas.drawText("Authorized Signature", authCenterX, sigY + 10f, engine.getPaint(COLOR_TEXT_SECONDARY, 6.5f, TYPEFACE_NORMAL, textAlign = Paint.Align.CENTER))
            
            val signatureLineY = sigY + 50f
            if (hasSig) {
                // Signature is 80x25. Draw centered horizontally on authCenterX, bottom aligned to line
                drawBitmapSafely(engine, canvas, sigPath, authCenterX - 40f, signatureLineY - 25f, 80f, 25f)
            }
            
            // Draw a clean baseline under the signature
            canvas.drawLine(authCenterX - 65f, signatureLineY, authCenterX + 65f, signatureLineY, linePaint)
            
            // Draw owner name
            var sigLabelY = signatureLineY + 11f
            val ownerName = company.ownerName
            if (ownerName.isNotBlank()) {
                canvas.drawText(ownerName, authCenterX, sigLabelY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER))
                sigLabelY += 10f
            } else {
                val designation = company.signatureText.ifBlank { "Authorized Person" }
                canvas.drawText(designation, authCenterX, sigLabelY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER))
                sigLabelY += 10f
            }
            
            // "For [Canonical Company Name]"
            val coNameVal = canonicalCompanyName
            if (coNameVal.isNotBlank()) {
                canvas.drawText("For ${coNameVal.uppercase(Locale.US)}", authCenterX, sigLabelY, engine.getPaint(COLOR_TEXT_SECONDARY, 7f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER))
            }
        }
        engine.currentY += signatureBoxH + sectionSpacing
    }"""

if old_draw_signature_pattern.search(content):
    content = old_draw_signature_pattern.sub(new_signature, content)
else:
    print("WARNING: Could not find drawSignature method to patch")

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
