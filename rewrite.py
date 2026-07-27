import re

with open('app/src/main/java/com/example/ui/settings/SettingsScreen.kt', 'r') as f:
    code = f.read()

# We want to replace the `Scaffold { ... }` block inside `fun SettingsScreen(...)`.
# The Scaffold starts right after `BackHandler(enabled = currentScreen != "main") { currentScreen = "main" }`

# Let's find BackHandler
back_handler_idx = code.find('BackHandler(enabled = currentScreen != "main")')
if back_handler_idx == -1:
    print("BackHandler not found")
    exit(1)

# Find the end of SettingsScreen function
settings_main_list_idx = code.find('fun SettingsMainList(')
if settings_main_list_idx == -1:
    print("SettingsMainList not found")
    exit(1)

# The part we want to replace is from `var currentScreen` to just before `fun SettingsMainList`
current_screen_idx = code.find('var currentScreen by remember { mutableStateOf("main") }')

if current_screen_idx != -1 and settings_main_list_idx != -1:
    part1 = code[:current_screen_idx]
    
    # We also need to get the helper functions at the bottom
    # They are after `fun SettingsMainList`
    helper_functions = code[code.find('private fun copyUriToInternalStorage'):]
    
    # Let's write the new Scaffold block
    new_ui = """
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Profile & Bank", "Quotation Defaults", "PDF Prefs", "Data & Backup")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1) }
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // Profile & Bank Tab
                        SettingsProfileAndBankTab(
                            coName = coName, onCoNameChange = { coName = it },
                            tagline = tagline, onTaglineChange = { tagline = it },
                            ownerName = ownerName, onOwnerNameChange = { ownerName = it },
                            phone = phone, onPhoneChange = { phone = it },
                            whatsappNumber = whatsappNumber, onWhatsappChange = { whatsappNumber = it },
                            email = email, onEmailChange = { email = it },
                            website = website, onWebsiteChange = { website = it },
                            gstin = gstin, onGstinChange = { gstin = it },
                            address = address, onAddressChange = { address = it },
                            city = city, onCityChange = { city = it },
                            district = district, onDistrictChange = { district = it },
                            state = state, onStateChange = { state = it },
                            pincode = pincode, onPincodeChange = { pincode = it },
                            bankName = bankName, onBankNameChange = { bankName = it },
                            accountHolderName = accountHolderName, onAccountHolderNameChange = { accountHolderName = it },
                            bankAccount = bankAccount, onBankAccountChange = { bankAccount = it },
                            bankIfsc = bankIfsc, onBankIfscChange = { bankIfsc = it },
                            bankBranch = bankBranch, onBankBranchChange = { bankBranch = it },
                            upiId = upiId, onUpiIdChange = { upiId = it },
                            coNameError = coNameError, phoneError = phoneError, emailError = emailError, gstinError = gstinError
                        )
                        // Save Button
                        Button(
                            onClick = {
                                val hasCoNameError = coName.trim().isEmpty()
                                val hasPhoneError = phone.trim().isEmpty()
                                val hasEmailError = !validateEmail(email)
                                val hasGstError = !validateGst(gstin)

                                coNameError = if (hasCoNameError) "Company Name is required" else null
                                phoneError = if (hasPhoneError) "Phone Number is required" else null
                                emailError = if (hasEmailError) "Invalid Email Address" else null
                                gstinError = if (hasGstError) "Invalid GSTIN format" else null

                                if (!hasCoNameError && !hasPhoneError && !hasEmailError && !hasGstError) {
                                    val currentProfile = companyProfile
                                    val newProfile = currentProfile?.copy(
                                        companyName = coName.trim(),
                                        contactPerson = ownerName.trim(),
                                        ownerName = ownerName.trim(),
                                        phone = phone.trim(),
                                        whatsappNumber = whatsappNumber.trim(),
                                        email = email.trim(),
                                        website = website.trim(),
                                        gstin = gstin.trim().uppercase(),
                                        address = address.trim(),
                                        city = city.trim(),
                                        district = district.trim(),
                                        state = state.trim(),
                                        pincode = pincode.trim(),
                                        bankName = bankName.trim(),
                                        accountHolderName = accountHolderName.trim(),
                                        accountNumber = bankAccount.trim(),
                                        ifsc = bankIfsc.trim().uppercase(),
                                        branch = bankBranch.trim(),
                                        upiId = upiId.trim(),
                                        tagline = tagline.trim()
                                    ) ?: CompanyProfile(
                                        id = 1,
                                        companyName = coName.trim(),
                                        contactPerson = ownerName.trim(),
                                        ownerName = ownerName.trim(),
                                        phone = phone.trim(),
                                        whatsappNumber = whatsappNumber.trim(),
                                        email = email.trim(),
                                        website = website.trim(),
                                        gstin = gstin.trim().uppercase(),
                                        address = address.trim(),
                                        city = city.trim(),
                                        district = district.trim(),
                                        state = state.trim(),
                                        pincode = pincode.trim(),
                                        bankName = bankName.trim(),
                                        accountHolderName = accountHolderName.trim(),
                                        accountNumber = bankAccount.trim(),
                                        ifsc = bankIfsc.trim().uppercase(),
                                        branch = bankBranch.trim(),
                                        upiId = upiId.trim(),
                                        tagline = tagline.trim()
                                    )
                                    companyViewModel.saveCompanyProfile(newProfile)
                                    Toast.makeText(context, "Company profile saved successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please fix all validation errors before saving.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Save Profile & Bank")
                        }
                    }
                    1 -> {
                        // Quotation Defaults Tab
                        SettingsQuotationDefaultsTab(
                            defaultGstRateStr = defaultGstRateStr, onDefaultGstRateStrChange = { defaultGstRateStr = it },
                            defaultDiscountStr = defaultDiscountStr, onDefaultDiscountStrChange = { defaultDiscountStr = it },
                            defaultValidityDaysStr = defaultValidityDaysStr, onDefaultValidityDaysStrChange = { defaultValidityDaysStr = it },
                            defaultDeliveryDaysStr = defaultDeliveryDaysStr, onDefaultDeliveryDaysStrChange = { defaultDeliveryDaysStr = it },
                            defaultWarranty = defaultWarranty, onDefaultWarrantyChange = { defaultWarranty = it },
                            defaultDeliveryTime = defaultDeliveryTime, onDefaultDeliveryTimeChange = { defaultDeliveryTime = it },
                            defaultInstallationTime = defaultInstallationTime, onDefaultInstallationTimeChange = { defaultInstallationTime = it },
                            defaultPaymentTerms = defaultPaymentTerms, onDefaultPaymentTermsChange = { defaultPaymentTerms = it },
                            defaultQuoteValidity = defaultQuoteValidity, onDefaultQuoteValidityChange = { defaultQuoteValidity = it },
                            additionalConditions = additionalConditions, onAdditionalConditionsChange = { additionalConditions = it },
                            termsAndConditions = termsAndConditions, onTermsAndConditionsChange = { termsAndConditions = it },
                            paymentTermsExpanded = paymentTermsExpanded, onPaymentTermsExpandedChange = { paymentTermsExpanded = it },
                            paymentTermsMaster = paymentTermsMaster
                        )
                        // Save Button
                        Button(
                            onClick = {
                                val currentProfile = companyProfile
                                val newProfile = currentProfile?.copy(
                                    defaultGstRate = defaultGstRateStr.toDoubleOrNull() ?: 18.0,
                                    defaultDiscount = defaultDiscountStr.toDoubleOrNull() ?: 0.0,
                                    defaultValidityDays = defaultValidityDaysStr.toIntOrNull() ?: 30,
                                    defaultDeliveryDays = defaultDeliveryDaysStr.toIntOrNull() ?: 15,
                                    termsAndConditions = termsAndConditions.trim(),
                                    defaultWarranty = defaultWarranty.trim(),
                                    defaultDeliveryTime = defaultDeliveryTime.trim(),
                                    defaultInstallationTime = defaultInstallationTime.trim(),
                                    defaultPaymentTerms = defaultPaymentTerms.trim(),
                                    defaultQuoteValidity = defaultQuoteValidity.trim(),
                                    additionalConditions = additionalConditions.trim()
                                ) ?: CompanyProfile(
                                    id = 1,
                                    defaultGstRate = defaultGstRateStr.toDoubleOrNull() ?: 18.0,
                                    defaultDiscount = defaultDiscountStr.toDoubleOrNull() ?: 0.0,
                                    defaultValidityDays = defaultValidityDaysStr.toIntOrNull() ?: 30,
                                    defaultDeliveryDays = defaultDeliveryDaysStr.toIntOrNull() ?: 15,
                                    termsAndConditions = termsAndConditions.trim(),
                                    defaultWarranty = defaultWarranty.trim(),
                                    defaultDeliveryTime = defaultDeliveryTime.trim(),
                                    defaultInstallationTime = defaultInstallationTime.trim(),
                                    defaultPaymentTerms = defaultPaymentTerms.trim(),
                                    defaultQuoteValidity = defaultQuoteValidity.trim(),
                                    additionalConditions = additionalConditions.trim()
                                )
                                companyViewModel.saveCompanyProfile(newProfile)
                                Toast.makeText(context, "Quotation defaults saved successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Save Defaults")
                        }
                    }
                    2 -> {
                        // PDF Preferences Tab
                        SettingsPdfPreferencesTab(
                            pdfShowLogo = pdfShowLogo, onPdfShowLogoChange = { pdfShowLogo = it; pdfPrefs.edit().putBoolean("pdf_show_logo", it).apply() },
                            pdfShowGst = pdfShowGst, onPdfShowGstChange = { pdfShowGst = it; pdfPrefs.edit().putBoolean("pdf_show_gst", it).apply() },
                            pdfShowWebsite = pdfShowWebsite, onPdfShowWebsiteChange = { pdfShowWebsite = it; pdfPrefs.edit().putBoolean("pdf_show_website", it).apply() },
                            pdfShowWhatsapp = pdfShowWhatsapp, onPdfShowWhatsappChange = { pdfShowWhatsapp = it; pdfPrefs.edit().putBoolean("pdf_show_whatsapp", it).apply() },
                            pdfShowValidUntil = pdfShowValidUntil, onPdfShowValidUntilChange = { pdfShowValidUntil = it; pdfPrefs.edit().putBoolean("pdf_show_valid_until", it).apply() },
                            pdfShowQrCode = pdfShowQrCode, onPdfShowQrCodeChange = { pdfShowQrCode = it; pdfPrefs.edit().putBoolean("pdf_show_qr_code", it).apply() },
                            pdfShowBankDetails = pdfShowBankDetails, onPdfShowBankDetailsChange = { pdfShowBankDetails = it; pdfPrefs.edit().putBoolean("pdf_show_bank_details", it).apply() },
                            pdfShowAmountInWords = pdfShowAmountInWords, onPdfShowAmountInWordsChange = { pdfShowAmountInWords = it; pdfPrefs.edit().putBoolean("pdf_show_amount_in_words", it).apply() },
                            pdfShowCompanySeal = pdfShowCompanySeal, onPdfShowCompanySealChange = { pdfShowCompanySeal = it; pdfPrefs.edit().putBoolean("pdf_show_company_seal", it).apply() },
                            pdfShowSignature = pdfShowSignature, onPdfShowSignatureChange = { pdfShowSignature = it; pdfPrefs.edit().putBoolean("pdf_show_signature", it).apply() },
                            pdfShowTermsConditions = pdfShowTermsConditions, onPdfShowTermsConditionsChange = { pdfShowTermsConditions = it; pdfPrefs.edit().putBoolean("pdf_show_terms_conditions", it).apply() },
                            pdfShowPageNumber = pdfShowPageNumber, onPdfShowPageNumberChange = { pdfShowPageNumber = it; pdfPrefs.edit().putBoolean("pdf_show_page_number", it).apply() }
                        )
                    }
                    3 -> {
                        // Data & Backup Tab
                        SettingsDataAndBackupTab(
                            onCreateWorkspace = { createWorkspaceLauncher.launch("InteriorPro_Workspace.ipro") },
                            onImportWorkspace = { importWorkspaceLauncher.launch(arrayOf("*/*")) },
                            lastBackupDate = lastBackupDate ?: "Never",
                            onNavigateToMasters = onNavigateToMasters,
                            onNavigateToSyncDashboard = onNavigateToSyncDashboard,
                            themeManager = themeManager,
                            onNavigateToAbout = onNavigateToAbout
                        )
                    }
                }
            }
        }
    }
}
"""
    
    with open('app/src/main/java/com/example/ui/settings/SettingsScreen.kt', 'w') as f:
        f.write(part1)
        f.write(new_ui)
        # No need to inject the sub-components here, I'll inject them as separate functions
