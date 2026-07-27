import re

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'r') as f:
    content = f.read()

# 1. Update the call
old_call = """                    else -> WizardStepReview(
                        quoteNumber = quoteNumber,
                        customerName = currentCustomer?.customerName ?: "Unknown",
                        customerPhone = currentCustomer?.mobileNumber ?: "",
                        customerAddress = currentCustomer?.address ?: "",
                        siteLocation = currentCustomer?.siteLocation ?: "",
                        itemsCount = quoteItems.size,
                        subtotal = subtotal,
                        discount = discount,
                        gstRate = gstRate,
                        gstAmount = gstAmount,
                        grandTotal = grandTotal,
                        terms = termsAndConditions,
                        warranty = warranty,
                        onDiscountChange = { quotationViewModel.setDiscount(it) },
                        onGstRateChange = { quotationViewModel.setGstRate(it) },
                        onTermsChange = { quotationViewModel.setTerms(it) },
                        onWarrantyChange = { quotationViewModel.setWarranty(it) },
                        onSave = {"""

new_call = """                    else -> WizardStepReview(
                        quotationViewModel = quotationViewModel,
                        quoteNumber = quoteNumber,
                        customerName = currentCustomer?.customerName ?: "Unknown",
                        customerPhone = currentCustomer?.mobileNumber ?: "",
                        itemsCount = quoteItems.size,
                        subtotal = subtotal,
                        discount = discount,
                        gstAmount = gstAmount,
                        grandTotal = grandTotal,
                        onSave = {"""

content = content.replace(old_call, new_call)

# 2. Replace WizardStepReview definition
# We will use regex to find `fun WizardStepReview( ... ) { ... }` up to `@Composable\nfun QuotationItemCard`

start_str = "fun WizardStepReview("
end_str = "fun QuotationItemCard("

start_idx = content.find(start_str)
end_idx = content.find(end_str)

if start_idx == -1 or end_idx == -1:
    print("Cannot find markers")
    exit(1)

# Find the @Composable just before end_str
composable_idx = content.rfind("@Composable", start_idx, end_idx)

new_func = """fun WizardStepReview(
    quotationViewModel: com.example.ui.quotation.QuotationViewModel,
    quoteNumber: String,
    customerName: String,
    customerPhone: String,
    itemsCount: Int,
    subtotal: Double,
    discount: Double,
    gstAmount: Double,
    grandTotal: Double,
    onSave: () -> Unit
) {
    val gstRate by quotationViewModel.newQuoteGstRate.collectAsState()
    val transport by quotationViewModel.newQuoteTransport.collectAsState()
    val installation by quotationViewModel.newQuoteInstallation.collectAsState()
    val extraCharges by quotationViewModel.newQuoteExtraCharges.collectAsState()
    val roundOff by quotationViewModel.newQuoteRoundOff.collectAsState()
    val advance by quotationViewModel.newQuoteAdvance.collectAsState()
    val balance by quotationViewModel.newQuoteBalance.collectAsState()
    
    val terms by quotationViewModel.newQuoteTerms.collectAsState()
    val warranty by quotationViewModel.newQuoteWarranty.collectAsState()
    val customerNotes by quotationViewModel.newQuoteCustomerNotes.collectAsState()
    val internalNotes by quotationViewModel.newQuoteInternalNotes.collectAsState()

    var discountStr by remember { mutableStateOf(if (discount > 0) discount.toString() else "") }
    var gstRateStr by remember { mutableStateOf(gstRate.toString()) }
    var transportStr by remember { mutableStateOf(if (transport > 0) transport.toString() else "") }
    var installationStr by remember { mutableStateOf(if (installation > 0) installation.toString() else "") }
    var extraStr by remember { mutableStateOf(if (extraCharges > 0) extraCharges.toString() else "") }
    var roundOffStr by remember { mutableStateOf(if (roundOff != 0.0) roundOff.toString() else "") }
    var advanceStr by remember { mutableStateOf(if (advance > 0) advance.toString() else "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Final Quotation Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(text = quoteNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = customerName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(text = customerPhone, fontSize = 14.sp)
                Text(text = "$itemsCount Items Included", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // Adjustments / Additions
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Billing Adjustments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it; quotationViewModel.setDiscount(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text("Discount Amount (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = gstRateStr,
                        onValueChange = { gstRateStr = it; quotationViewModel.setGstRate(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text("GST Rate (%)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = transportStr,
                        onValueChange = { transportStr = it; quotationViewModel.setTransport(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text("Transport (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = installationStr,
                        onValueChange = { installationStr = it; quotationViewModel.setInstallation(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text("Installation (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = extraStr,
                        onValueChange = { extraStr = it; quotationViewModel.setExtraCharges(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text("Extra Charges (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = roundOffStr,
                        onValueChange = { roundOffStr = it; quotationViewModel.setRoundOff(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text("Round Off (₹)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
                
                OutlinedTextField(
                    value = advanceStr,
                    onValueChange = { advanceStr = it; quotationViewModel.setAdvance(it.toDoubleOrNull() ?: 0.0) },
                    label = { Text("Advance Received (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }

        // Summary
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Final Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(subtotal)}", fontWeight = FontWeight.Bold)
                }
                if (discount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Discount", color = MaterialTheme.colorScheme.error)
                        Text("-₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(discount)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
                if (gstAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GST (${gstRate}%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(gstAmount)}", fontWeight = FontWeight.Bold)
                    }
                }
                if (transport > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Transport", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(transport)}", fontWeight = FontWeight.Bold)
                    }
                }
                if (installation > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Installation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(installation)}", fontWeight = FontWeight.Bold)
                    }
                }
                if (extraCharges > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Extra Charges", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(extraCharges)}", fontWeight = FontWeight.Bold)
                    }
                }
                if (roundOff != 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Round Off", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(roundOff)}", fontWeight = FontWeight.Bold)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Grand Total", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(grandTotal)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                
                if (advance > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Advance Paid", color = MaterialTheme.colorScheme.tertiary)
                        Text("-₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(advance)}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Balance Due", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(balance)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        // Notes & Terms
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Terms & Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = customerNotes,
                    onValueChange = { quotationViewModel.setCustomerNotes(it) },
                    label = { Text("Customer Notes (Visible on PDF)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                
                OutlinedTextField(
                    value = internalNotes,
                    onValueChange = { quotationViewModel.setInternalNotes(it) },
                    label = { Text("Internal Notes (Private)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                
                OutlinedTextField(
                    value = terms,
                    onValueChange = { quotationViewModel.setTerms(it) },
                    label = { Text("Terms & Conditions") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
                OutlinedTextField(
                    value = warranty,
                    onValueChange = { quotationViewModel.setWarranty(it) },
                    label = { Text("Warranty Terms") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Quotation", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
"""

content = content[:start_idx] + new_func + content[composable_idx:]

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'w') as f:
    f.write(content)
print("Updated review successfully")

