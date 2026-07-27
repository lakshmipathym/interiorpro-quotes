file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# --- Part 1: Fix Image Copy Try-Catch Block (around lines 570 to 615) ---
old_img_copy = """                            // Copy ALL reference design images
                            quoteItems.forEachIndexed { index, item ->
                                val (desc, specs) = parseItemSpecs(item.description)
                                var modified = false
                                var updatedSpecs = specs
                                val safeQuoteNum = quoteNumber.replace("/", "_")

                                if (specs.designImageUri.isNotBlank() && specs.designImageUri.contains("temp_des_")) {
                                    val file = File(context.filesDir, File(specs.designImageUri).name)
                                    if (file.exists()) {
                                        try {
                                            val destFile = File(context.filesDir, "design_${safeQuoteNum}_${index}.jpg")
                                            file.copyTo(destFile, overwrite = true)
                                            file.delete()
                                            updatedSpecs = updatedSpecs.copy(designImageUri = destFile.absolutePath)
                                            modified = true

                                        }
                                    }
                                }
                                if (specs.laminateImageUri.isNotBlank() && specs.laminateImageUri.contains("temp_lam_")) {
                                    val file = File(context.filesDir, File(specs.laminateImageUri).name)
                                    if (file.exists()) {
                                        try {
                                            val destFile = File(context.filesDir, "laminate_${safeQuoteNum}_${index}.jpg")
                                            file.copyTo(destFile, overwrite = true)
                                            file.delete()
                                            updatedSpecs = updatedSpecs.copy(laminateImageUri = destFile.absolutePath)
                                            modified = true

                                        }
                                    }
                                }
                                if (modified) {
                                    val newDesc = serializeItemSpecs(desc, updatedSpecs)
                                    quotationViewModel.updateQuoteItem(index, item.copy(description = newDesc))
                            // Save
                                }
                            }
                            quotationViewModel.saveQuotation { id ->
                                savedQuotationId = id
                            }
                        }
                    }
                    )"""

new_img_copy = """                            // Copy ALL reference design images
                            quoteItems.forEachIndexed { index, item ->
                                val (desc, specs) = parseItemSpecs(item.description)
                                var modified = false
                                var updatedSpecs = specs
                                val safeQuoteNum = quoteNumber.replace("/", "_")

                                if (specs.designImageUri.isNotBlank() && specs.designImageUri.contains("temp_des_")) {
                                    val file = File(context.filesDir, File(specs.designImageUri).name)
                                    if (file.exists()) {
                                        try {
                                            val destFile = File(context.filesDir, "design_${safeQuoteNum}_${index}.jpg")
                                            file.copyTo(destFile, overwrite = true)
                                            file.delete()
                                            updatedSpecs = updatedSpecs.copy(designImageUri = destFile.absolutePath)
                                            modified = true
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                                if (specs.laminateImageUri.isNotBlank() && specs.laminateImageUri.contains("temp_lam_")) {
                                    val file = File(context.filesDir, File(specs.laminateImageUri).name)
                                    if (file.exists()) {
                                        try {
                                            val destFile = File(context.filesDir, "laminate_${safeQuoteNum}_${index}.jpg")
                                            file.copyTo(destFile, overwrite = true)
                                            file.delete()
                                            updatedSpecs = updatedSpecs.copy(laminateImageUri = destFile.absolutePath)
                                            modified = true
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                                if (modified) {
                                    val newDesc = serializeItemSpecs(desc, updatedSpecs)
                                    quotationViewModel.updateQuoteItem(index, item.copy(description = newDesc))
                                }
                            }
                            quotationViewModel.saveQuotation { id ->
                                savedQuotationId = id
                            }
                        }
                    }
                )"""

if old_img_copy in content:
    content = content.replace(old_img_copy, new_img_copy)
    print("[SUCCESS] Replaced image copy try-catch blocks.")
else:
    print("[FAILED] Direct match not found for image copy blocks.")

# --- Part 2: Fix Success Dialog (around lines 684 to 847) ---
# Since there may be slight whitespace/bracing deviations, we will find by a robust regex from "Success dialog shown when Saved" down to line 847 "}" before "// --- STEP 1: CUSTOMER SELECTION"
import re
pattern = r"// Success dialog shown when Saved.*?\n\s+// --- STEP 1: CUSTOMER SELECTION & QUOTATION ITEMS ---"
# Let's inspect if we can match that. Since it spans many lines, let's use re.DOTALL

new_success_dialog_block = """// Success dialog shown when Saved
    savedQuotationId?.let { id ->
        AlertDialog(
            onDismissRequest = {
                savedQuotationId = null
                quotationViewModel.startNewQuotation()
                onSuccessReturn()
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Quotation Saved Successfully",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Quotation Number",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = quoteNumber,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // PDF success message
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PDF Generated Successfully",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stack of interactive buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Open PDF
                        Button(
                            onClick = {
                                scope.launch {
                                    val pdfFile = com.example.utils.ShareManager.generateQuotationPdf(context, quotationViewModel.repository, id)
                                    com.example.utils.ShareManager.openOrViewPdf(context, pdfFile)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open PDF")
                        }

                        // 2. Send to Customer (WhatsApp)
                        Button(
                            onClick = {
                                scope.launch {
                                    val pdfFile = com.example.utils.ShareManager.generateQuotationPdf(context, quotationViewModel.repository, id)
                                    val phone = currentCustomer?.mobileNumber ?: ""
                                    com.example.utils.ShareManager.shareToWhatsApp(context, pdfFile, phone)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366), // WhatsApp Green
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send to Customer")
                        }

                        // 3. Share PDF
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val pdfFile = com.example.utils.ShareManager.generateQuotationPdf(context, quotationViewModel.repository, id)
                                    com.example.utils.ShareManager.shareQuotation(context, pdfFile, quoteNumber)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share PDF")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        savedQuotationId = null
                        quotationViewModel.startNewQuotation()
                        onSuccessReturn()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

// --- STEP 1: CUSTOMER SELECTION & QUOTATION ITEMS ---"""

if re.search(pattern, content, re.DOTALL):
    content = re.sub(pattern, new_success_dialog_block, content, flags=re.DOTALL)
    print("[SUCCESS] Replaced Success Dialog block using regex.")
else:
    print("[FAILED] Could not match Success Dialog pattern using regex.")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Part 2 completed.")
