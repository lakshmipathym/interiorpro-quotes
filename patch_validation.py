with open("app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt", "r") as f:
    content = f.read()

import re

old_is_next = """                val isNextEnabled = when (activeStep) {
                    1 -> currentCustomer != null && siteName.isNotBlank() && siteAddress.isNotBlank()
                    2 -> quoteItems.isNotEmpty()
                    else -> true

                }"""

new_is_next = """                val step1Error = when {
                    currentCustomer == null -> "Select a customer"
                    siteName.isBlank() -> "Enter Site Name"
                    siteAddress.isBlank() -> "Enter Site Address"
                    else -> null
                }
                
                val step2Error = when {
                    quoteItems.isEmpty() -> "Add at least one item"
                    else -> null
                }
                
                val validationError = when (activeStep) {
                    1 -> step1Error
                    2 -> step2Error
                    else -> null
                }
                
                val isNextEnabled = validationError == null"""

content = content.replace(old_is_next, new_is_next)

old_button = """                Button(
                    onClick = {
                        when (activeStep) {
                            1 -> activeStep = 2
                            2 -> activeStep = 3
                            else -> performSaveQuotation()
                        }
                    },
                    enabled = isNextEnabled,
                    modifier = Modifier
                        .weight(nextButtonWeight)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (activeStep == 1) "Next to Items" else if (activeStep == 2) "Next to Review" else "Save Quotation")
                    if (activeStep < 3) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }"""

new_button = """                Column(modifier = Modifier.weight(nextButtonWeight), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (validationError != null) {
                        Text(
                            text = validationError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Button(
                        onClick = {
                            when (activeStep) {
                                1 -> activeStep = 2
                                2 -> activeStep = 3
                                else -> performSaveQuotation()
                            }
                        },
                        enabled = isNextEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (activeStep == 1) "Next to Items" else if (activeStep == 2) "Next to Review" else "Save Quotation")
                        if (activeStep < 3) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }"""

content = content.replace(old_button, new_button)

with open("app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt", "w") as f:
    f.write(content)
