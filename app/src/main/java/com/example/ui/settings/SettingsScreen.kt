package com.example.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: com.example.ui.settings.SettingsViewModel,
    companyViewModel: com.example.ui.company.CompanyViewModel,
    masterViewModel: com.example.ui.company.MasterViewModel,
    themeManager: ThemeManager,
    onNavigateToMasters: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToSyncDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val companyProfile by companyViewModel.companyProfile.collectAsState()
    
    val gstRates by masterViewModel.getFilteredMasters("GST_RATE").collectAsState(initial = emptyList())
    val paymentTermsMaster by masterViewModel.getFilteredMasters("PAYMENT_TERM").collectAsState(initial = emptyList())

    // Company Profile state variables
    var coName by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }

    // Address
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }

    // Bank details
    var bankName by remember { mutableStateOf("") }
    var accountHolderName by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var bankIfsc by remember { mutableStateOf("") }
    var bankBranch by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }

    // Branding & Customization
    var logoPath by remember { mutableStateOf("") }
    var signaturePath by remember { mutableStateOf("") }
    var companySealPath by remember { mutableStateOf("") }
    var signatureText by remember { mutableStateOf("") }

    // Quotation Defaults State
    var defaultGstRateStr by remember { mutableStateOf("18.0") }
    var defaultDiscountStr by remember { mutableStateOf("0.0") }
    var defaultValidityDaysStr by remember { mutableStateOf("30") }
    var defaultDeliveryDaysStr by remember { mutableStateOf("15") }
    var logoRefreshKey by remember { mutableStateOf(System.currentTimeMillis()) }
    var sigRefreshKey by remember { mutableStateOf(System.currentTimeMillis()) }
    var sealRefreshKey by remember { mutableStateOf(System.currentTimeMillis()) }

    // Terms
    var termsAndConditions by remember { mutableStateOf("") }
    var defaultWarranty by remember { mutableStateOf("") }
    var defaultDeliveryTime by remember { mutableStateOf("") }
    var defaultInstallationTime by remember { mutableStateOf("") }
    var defaultPaymentTerms by remember { mutableStateOf("") }
    var defaultQuoteValidity by remember { mutableStateOf("") }
    var additionalConditions by remember { mutableStateOf("") }

    var gstExpanded by remember { mutableStateOf(false) }
    var paymentTermsExpanded by remember { mutableStateOf(false) }

    // Error states
    var coNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var gstinError by remember { mutableStateOf<String?>(null) }

    val sharedPref = remember { context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE) }
    val pdfPrefs = remember { context.getSharedPreferences("pdf_prefs", Context.MODE_PRIVATE) }
    var pdfShowLogo by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_logo", true)) }
    var pdfShowGst by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_gst", true)) }
    var pdfShowWebsite by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_website", true)) }
    var pdfShowWhatsapp by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_whatsapp", true)) }
    var pdfShowValidUntil by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_valid_until", true)) }
    var pdfShowQrCode by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_qr_code", true)) }
    var pdfShowBankDetails by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_bank_details", true)) }
    var pdfShowAmountInWords by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_amount_in_words", true)) }
    var pdfShowCompanySeal by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_company_seal", true)) }
    var pdfShowSignature by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_signature", true)) }
    var pdfShowTermsConditions by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_terms_conditions", true)) }
    var pdfShowPageNumber by remember { mutableStateOf(pdfPrefs.getBoolean("pdf_show_page_number", true)) }
    var lastBackupDate by remember { mutableStateOf(sharedPref.getString("last_backup_date", "Never")) }
    var isConfirmRestoreDialogOpen by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf("") }

    // Google Drive Sync & Authentication state
    val syncState by settingsViewModel.syncState.collectAsState()
    val isUserSignedIn by settingsViewModel.isUserSignedIn.collectAsState()
    val currentUserEmail by settingsViewModel.currentUserEmail.collectAsState()
    val currentUserDisplayName by settingsViewModel.currentUserDisplayName.collectAsState()
    val isSignInLoading by settingsViewModel.isSignInLoading.collectAsState()
    val authErrorMessage by settingsViewModel.authErrorMessage.collectAsState()

    val lastCloudBackupDate by settingsViewModel.lastBackupDate.collectAsState()
    val lastCloudBackupFileName by settingsViewModel.lastBackupFileName.collectAsState()
    val lastCloudBackupStatus by settingsViewModel.lastBackupStatus.collectAsState()
    val isBackupInProgress by settingsViewModel.isBackupInProgress.collectAsState()
    val cloudBackupsListState by settingsViewModel.cloudBackupsList.collectAsState()
    val isLoadingCloudBackups by settingsViewModel.isLoadingCloudBackups.collectAsState()
    var isCloudRestoreListDialogOpen by remember { mutableStateOf(false) }

    var cloudBackupsList by remember { mutableStateOf<List<com.example.core.drive.DriveFileInfo>>(emptyList()) }
    var workspacePreviewState by remember { mutableStateOf<com.example.core.backup.WorkspacePreview?>(null) }
    var isConfirmWorkspaceImportDialogOpen by remember { mutableStateOf(false) }

    var isConfirmSpecificRestoreDialogOpen by remember { mutableStateOf(false) }
    var specificRestoreFileId by remember { mutableStateOf("") }
    var specificRestoreFileName by remember { mutableStateOf("") }

    // License & Device Info
    val deviceBindingManager = remember { com.example.core.device.DeviceBindingManager(context) }
    val deviceBindingInfo by deviceBindingManager.bindingInfo.collectAsState()
    val cloudLicenseValidator = remember { com.example.core.license.CloudLicenseValidator(context) }
    val cloudLicenseState by cloudLicenseValidator.cloudState.collectAsState()
    var isCloudSyncing by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf(com.example.core.license.SubscriptionPlan.COMMERCIAL_ANNUAL) }
    var activationKeyText by remember { mutableStateOf("") }
    var activationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isUserSignedIn) {
        if (isUserSignedIn) {
            try {
                cloudBackupsList = settingsViewModel.listCloudBackups()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Backup & Restore launchers
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                settingsViewModel.exportBackupData { json ->
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(json.toByteArray(Charsets.UTF_8))
                        }
                        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        sharedPref.edit().putString("last_backup_date", currentDate).apply()
                        lastBackupDate = currentDate
                        Toast.makeText(context, "Backup file saved successfully!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to save backup file", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val createWorkspaceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val tempFile = File(context.cacheDir, "temp_export.ipro")
                settingsViewModel.exportWorkspaceBundle(tempFile) { file ->
                    try {
                        if (file != null && file.exists()) {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.write(file.readBytes())
                            }
                            Toast.makeText(context, "Workspace exported successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Workspace packaging failed.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Export writing failed", Toast.LENGTH_LONG).show()
                    } finally {
                        if (tempFile.exists()) tempFile.delete()
                    }
                }
            }
        }
    }

    val importWorkspaceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val tempFile = File(context.cacheDir, "temp_import.ipro")
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempFile.outputStream().use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytes = 0
                            val maxBytes = 50 * 1024 * 1024
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                totalBytes += bytesRead
                                if (totalBytes > maxBytes) throw SecurityException("Workspace file exceeds maximum allowed size")
                                outputStream.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                    settingsViewModel.verifyAndPreviewWorkspaceBundle(tempFile) { preview ->
                        if (tempFile.exists()) tempFile.delete()
                        if (preview.isValid) {
                            workspacePreviewState = preview
                            isConfirmWorkspaceImportDialogOpen = true
                        } else {
                            Toast.makeText(context, "Verification failed: ${preview.errorReason}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    if (tempFile.exists()) tempFile.delete()
                    Toast.makeText(context, "Failed to read file", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Image Pickers
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedPath = copyUriToInternalStorage(context, it, "company_logo.png")
            if (copiedPath != null) {
                logoPath = copiedPath
                logoRefreshKey = System.currentTimeMillis()
                Toast.makeText(context, "Company Logo updated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to copy logo image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val signaturePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedPath = copyUriToInternalStorage(context, it, "auth_signature.png")
            if (copiedPath != null) {
                signaturePath = copiedPath
                sigRefreshKey = System.currentTimeMillis()
                Toast.makeText(context, "Authorized Signature updated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to copy signature image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val companySealPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedPath = copyUriToInternalStorage(context, it, "company_seal.png")
            if (copiedPath != null) {
                companySealPath = copiedPath
                sealRefreshKey = System.currentTimeMillis()
                Toast.makeText(context, "Company Seal updated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to copy seal image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load initial profile
    LaunchedEffect(companyProfile) {
        companyProfile?.let {
            coName = it.companyName
            ownerName = it.ownerName
            phone = it.phone
            whatsappNumber = it.whatsappNumber
            email = it.email
            website = it.website
            gstin = it.gstin

            address = it.address
            city = it.city
            district = it.district
            state = it.state
            pincode = it.pincode

            bankName = it.bankName
            accountHolderName = it.accountHolderName
            bankAccount = it.accountNumber
            bankIfsc = it.ifsc
            bankBranch = it.branch

            upiId = it.upiId
            logoPath = it.logoPath
            signaturePath = it.signaturePath
            companySealPath = it.companySealPath
            signatureText = it.signatureText

            tagline = it.tagline
            defaultGstRateStr = it.defaultGstRate.toString()
            defaultDiscountStr = it.defaultDiscount.toString()
            defaultValidityDaysStr = it.defaultValidityDays.toString()
            defaultDeliveryDaysStr = it.defaultDeliveryDays.toString()
            termsAndConditions = it.termsAndConditions
            defaultWarranty = it.defaultWarranty
            defaultDeliveryTime = it.defaultDeliveryTime
            defaultInstallationTime = it.defaultInstallationTime
            defaultPaymentTerms = it.defaultPaymentTerms
            defaultQuoteValidity = it.defaultQuoteValidity
            additionalConditions = it.additionalConditions
        }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Settings Overview", "Company Profile", "Quotation Defaults", "PDF Prefs", "Cloud & Sync")

    fun saveProfile() {
        val hasCoNameError = coName.trim().isEmpty()
        val hasPhoneError = phone.trim().isEmpty()
        val hasEmailError = email.isNotEmpty() && !com.example.utils.ValidationManager.isValidEmail(email)
        val hasGstError = gstin.isNotEmpty() && !com.example.utils.ValidationManager.isValidGstin(gstin)

        coNameError = if (hasCoNameError) "Company Name is required" else null
        phoneError = if (hasPhoneError) "Phone Number is required" else null
        emailError = if (hasEmailError) "Invalid Email Address" else null
        gstinError = if (hasGstError) "Invalid GSTIN format" else null

        if (!hasCoNameError && !hasPhoneError && !hasEmailError && !hasGstError) {
            val newProfile = CompanyProfile(
                id = companyProfile?.id ?: 1,
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
                logoPath = logoPath.trim(),
                signaturePath = signaturePath.trim(),
                companySealPath = companySealPath.trim(),
                signatureText = signatureText.trim(),
                tagline = tagline.trim(),
                brandColor = companyProfile?.brandColor ?: "",
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
            Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please fix errors in Profile & Bank", Toast.LENGTH_LONG).show()
            selectedTabIndex = 1
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex in 1..2) {
                ExtendedFloatingActionButton(
                    onClick = { saveProfile() },
                    icon = { Icon(Icons.Default.Save, contentDescription = "Save Settings") },
                    text = { Text("Save Settings", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_save_settings")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = Spacing.L,
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
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.L, vertical = Spacing.L)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.L),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.L)
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            // --- OVERVIEW TAB: THE 7 COMMERCIAL GRADE SECTIONS ---

                            // 1. COMPANY SECTION
                            SettingsSectionHeader(title = "1. Company")
                            SettingsCard {
                                InformationRow(
                                    title = "Company Name",
                                    value = coName.ifBlank { "Not Configured" },
                                    icon = Icons.Default.Business
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "GST Number",
                                    value = gstin.ifBlank { "Not Registered" },
                                    icon = Icons.Default.Receipt
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Phone",
                                    value = phone.ifBlank { "Not Configured" },
                                    icon = Icons.Default.Phone
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Email",
                                    value = email.ifBlank { "Not Configured" },
                                    icon = Icons.Default.Email
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ActionRow(
                                    title = "Edit Company Profile",
                                    subtitle = "Update details, address, bank info & branding",
                                    actionLabel = "Edit",
                                    onActionClick = { selectedTabIndex = 1 },
                                    icon = Icons.Default.Edit
                                )
                            }

                            // 2. SUBSCRIPTION SECTION
                            SettingsSectionHeader(title = "2. Subscription")
                            SettingsCard {
                                InformationRow(
                                    title = "Plan",
                                    value = cloudLicenseState.plan,
                                    icon = Icons.Default.CardMembership
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.L, vertical = Spacing.M),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(IconSize.Medium).padding(end = Spacing.M)
                                        )
                                        Text("Status", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    }
                                    PremiumBadge(
                                        text = cloudLicenseState.status,
                                        containerColor = if (cloudLicenseState.status == "ACTIVE") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        contentColor = if (cloudLicenseState.status == "ACTIVE") Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Expiry",
                                    value = cloudLicenseState.expiryDateIso?.take(10) ?: "N/A",
                                    icon = Icons.Default.Event
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Remaining Days",
                                    value = "${cloudLicenseState.remainingDays} Days",
                                    icon = Icons.Default.HourglassEmpty
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "License Key",
                                    value = if (!cloudLicenseState.licenseKey.isNullOrEmpty()) cloudLicenseState.licenseKey!! else "Cloud Verified",
                                    icon = Icons.Default.Key
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ActionRow(
                                    title = "Sync License",
                                    subtitle = cloudLicenseState.syncStatusMessage,
                                    actionLabel = if (isCloudSyncing) "Syncing..." else "Sync Now",
                                    onActionClick = {
                                        scope.launch {
                                            isCloudSyncing = true
                                            try {
                                                cloudLicenseValidator.verifyOrRegisterCloudLicense()
                                            } finally {
                                                isCloudSyncing = false
                                            }
                                        }
                                    },
                                    icon = Icons.Default.Sync
                                )
                            }

                            // 3. GOOGLE DRIVE SECTION
                            SettingsSectionHeader(title = "3. Google Drive")
                            SettingsCard {
                                InformationRow(
                                    title = "Connection Status",
                                    value = if (isUserSignedIn) "Connected" else "Disconnected",
                                    icon = if (isUserSignedIn) Icons.Default.CloudDone else Icons.Default.CloudOff
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Account",
                                    value = currentUserEmail ?: "Not Connected",
                                    icon = Icons.Default.AccountCircle
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Last Backup",
                                    value = lastCloudBackupDate,
                                    icon = Icons.Default.History
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                if (isUserSignedIn) {
                                    ActionRow(
                                        title = "Backup to Drive",
                                        subtitle = lastCloudBackupFileName,
                                        actionLabel = if (isBackupInProgress) "Backing up..." else "Backup",
                                        onActionClick = {
                                            settingsViewModel.performBackupToGoogleDrive { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        icon = Icons.Default.CloudUpload
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ActionRow(
                                        title = "Restore from Drive",
                                        subtitle = "Fetch available cloud backups",
                                        actionLabel = "Restore",
                                        onActionClick = {
                                            settingsViewModel.fetchCloudBackupsList()
                                            isCloudRestoreListDialogOpen = true
                                        },
                                        icon = Icons.Default.CloudDownload
                                    )
                                } else {
                                    ActionRow(
                                        title = "Connect Google Account",
                                        subtitle = "Enable automatic drive backups & sync",
                                        actionLabel = "Connect",
                                        onActionClick = {
                                            settingsViewModel.signIn(context) { success ->
                                                if (success) Toast.makeText(context, "Drive Connected!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        icon = Icons.Default.CloudQueue
                                    )
                                }
                            }

                            // 4. DEVICE SECTION
                            SettingsSectionHeader(title = "4. Device")
                            SettingsCard {
                                InformationRow(
                                    title = "Device Name",
                                    value = android.os.Build.MODEL,
                                    icon = Icons.Default.Smartphone
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Device ID",
                                    value = if (deviceBindingInfo.deviceId.length > 12) deviceBindingInfo.deviceId.take(12) + "..." else deviceBindingInfo.deviceId,
                                    icon = Icons.Default.PhonelinkSetup
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Installation ID",
                                    value = if (deviceBindingInfo.installationId.length > 12) deviceBindingInfo.installationId.take(12) + "..." else deviceBindingInfo.installationId,
                                    icon = Icons.Default.Apps
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Workspace ID",
                                    value = deviceBindingInfo.workspaceId,
                                    icon = Icons.Default.Workspaces
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Fingerprint",
                                    value = if (deviceBindingInfo.deviceFingerprint.length > 12) deviceBindingInfo.deviceFingerprint.take(12) + "..." else deviceBindingInfo.deviceFingerprint,
                                    icon = Icons.Default.Fingerprint
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "App Version",
                                    value = "1.0.0 (Build 100)",
                                    icon = Icons.Default.SystemUpdate
                                )
                            }

                            // 5. BACKUP SECTION
                            SettingsSectionHeader(title = "5. Backup")
                            SettingsCard {
                                InformationRow(
                                    title = "Last Backup",
                                    value = lastBackupDate ?: "Never",
                                    icon = Icons.Default.History
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Backup Size",
                                    value = "Compressed .ipro",
                                    icon = Icons.Default.Storage
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ActionRow(
                                    title = "Create Workspace Backup",
                                    subtitle = "Export full database bundle to .ipro file",
                                    actionLabel = "Export",
                                    onActionClick = { createWorkspaceLauncher.launch("InteriorPro_Workspace.ipro") },
                                    icon = Icons.Default.Download
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ActionRow(
                                    title = "Restore Workspace Backup",
                                    subtitle = "Restore database from .ipro file",
                                    actionLabel = "Import",
                                    onActionClick = { importWorkspaceLauncher.launch(arrayOf("*/*")) },
                                    icon = Icons.Default.Upload
                                )
                            }

                            // 6. SECURITY SECTION
                            SettingsSectionHeader(title = "6. Security")
                            SettingsCard {
                                InformationRow(
                                    title = "License Status",
                                    value = cloudLicenseState.status,
                                    icon = Icons.Default.Security
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Trial Status",
                                    value = if (cloudLicenseState.plan.contains("TRIAL", ignoreCase = true)) "Trial Active" else "Commercial Plan",
                                    icon = Icons.Default.Timer
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Offline Grace",
                                    value = "30 Days Offline Cache",
                                    icon = Icons.Default.WifiOff
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Cloud Sync",
                                    value = cloudLicenseState.syncStatusMessage,
                                    icon = Icons.Default.CloudSync
                                )
                            }

                            // 7. ABOUT SECTION
                            SettingsSectionHeader(title = "7. About InteriorPro ERP")
                            SettingsCard {
                                InformationRow(
                                    title = "App Name",
                                    value = "InteriorPro ERP",
                                    icon = Icons.Default.Business
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Version",
                                    value = "1.0.0",
                                    icon = Icons.Default.Info
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Build Number",
                                    value = "100",
                                    icon = Icons.Default.Build
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Copyright",
                                    value = "© 2026 InteriorPro ERP. All rights reserved.",
                                    icon = Icons.Default.Copyright
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                InformationRow(
                                    title = "Company",
                                    value = "InteriorPro Technologies Pvt. Ltd.",
                                    icon = Icons.Default.Domain
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                PreferenceRow(
                                    title = "Website",
                                    subtitle = website.ifBlank { "www.interiorpro.com" },
                                    icon = Icons.Default.Language,
                                    onClick = { Toast.makeText(context, "Website: ${website.ifBlank { "www.interiorpro.com" }}", Toast.LENGTH_SHORT).show() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                PreferenceRow(
                                    title = "Support Email",
                                    subtitle = "support@interiorpro.com",
                                    icon = Icons.Default.Help,
                                    onClick = { Toast.makeText(context, "Support Email: support@interiorpro.com", Toast.LENGTH_SHORT).show() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                PreferenceRow(
                                    title = "Privacy Policy",
                                    subtitle = "Data security & privacy rules",
                                    icon = Icons.Default.PrivacyTip,
                                    onClick = { Toast.makeText(context, "Privacy Policy: All data stored locally and secured in your Google Drive.", Toast.LENGTH_LONG).show() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                PreferenceRow(
                                    title = "Terms of Service",
                                    subtitle = "Application usage conditions",
                                    icon = Icons.Default.Gavel,
                                    onClick = { Toast.makeText(context, "Terms: InteriorPro Commercial License v1.0", Toast.LENGTH_LONG).show() }
                                )
                            }

                            // ADDITIONAL USEFUL NAVIGATIONS
                            SettingsSectionHeader(title = "Database & Master Configuration")
                            SettingsCard {
                                PreferenceRow(
                                    title = "Database Masters",
                                    subtitle = "Manage materials, finishes, categories, GST rates",
                                    icon = Icons.Default.List,
                                    onClick = onNavigateToMasters
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                PreferenceRow(
                                    title = "Enterprise Sync Dashboard",
                                    subtitle = "Advanced sync health and drive status",
                                    icon = Icons.Default.Dashboard,
                                    onClick = onNavigateToSyncDashboard
                                )
                            }
                        }

                        1 -> {
                            // --- TAB 1: COMPANY PROFILE EDITING ---
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

                            Spacer(modifier = Modifier.height(Spacing.L))

                            SettingsBrandingTab(
                                logoPath = logoPath, onLogoPathChange = { logoPath = it },
                                signaturePath = signaturePath, onSignaturePathChange = { signaturePath = it },
                                companySealPath = companySealPath, onCompanySealPathChange = { companySealPath = it },
                                signatureText = signatureText, onSignatureTextChange = { signatureText = it },
                                logoRefreshKey = logoRefreshKey, onLogoRefreshKeyChange = { logoRefreshKey = it },
                                sigRefreshKey = sigRefreshKey, onSigRefreshKeyChange = { sigRefreshKey = it },
                                sealRefreshKey = sealRefreshKey, onSealRefreshKeyChange = { sealRefreshKey = it }
                            )
                        }

                        2 -> {
                            // --- TAB 2: QUOTATION DEFAULTS ---
                            SettingsQuotationDefaultsTab(
                                defaultGstRateStr = defaultGstRateStr, onDefaultGstRateStrChange = { defaultGstRateStr = it },
                                defaultDiscountStr = defaultDiscountStr, onDefaultDiscountStrChange = { defaultDiscountStr = it },
                                defaultValidityDaysStr = defaultValidityDaysStr, onDefaultValidityDaysStrChange = { defaultValidityDaysStr = it },
                                defaultDeliveryDaysStr = defaultDeliveryDaysStr, onDefaultDeliveryDaysStrChange = { defaultDeliveryDaysStr = it },
                                defaultWarranty = defaultWarranty, onDefaultWarrantyChange = { defaultWarranty = it },
                                defaultDeliveryTime = defaultDeliveryTime, onDefaultDeliveryTimeChange = { defaultDeliveryTime = it },
                                defaultInstallationTime = defaultInstallationTime, onDefaultInstallationTimeChange = { defaultInstallationTime = it },
                                defaultPaymentTerms = defaultPaymentTerms, onDefaultPaymentTermsChange = { defaultPaymentTerms = it },
                                additionalConditions = additionalConditions, onAdditionalConditionsChange = { additionalConditions = it },
                                termsAndConditions = termsAndConditions, onTermsAndConditionsChange = { termsAndConditions = it },
                                paymentTermsExpanded = paymentTermsExpanded, onPaymentTermsExpandedChange = { paymentTermsExpanded = it },
                                paymentTermsMaster = paymentTermsMaster
                            )
                        }

                        3 -> {
                            // --- TAB 3: PDF PREFERENCES ---
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

                        4 -> {
                            // --- TAB 4: CLOUD & SYNC ---
                            SettingsDataAndBackupTab(
                                onCreateWorkspace = { createWorkspaceLauncher.launch("InteriorPro_Workspace.ipro") },
                                onImportWorkspace = { importWorkspaceLauncher.launch(arrayOf("*/*")) },
                                lastBackupDate = lastBackupDate ?: "Never",
                                onNavigateToMasters = onNavigateToMasters,
                                onNavigateToSyncDashboard = onNavigateToSyncDashboard,
                                themeManager = themeManager,
                                onNavigateToAbout = onNavigateToAbout,
                                isUserSignedIn = isUserSignedIn,
                                currentUserEmail = currentUserEmail,
                                currentUserDisplayName = currentUserDisplayName,
                                onSignIn = {
                                    settingsViewModel.signIn(context) { success ->
                                        if (success) Toast.makeText(context, "Google Drive Connected!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onSignOut = {
                                    settingsViewModel.signOut { success ->
                                        if (success) Toast.makeText(context, "Google Drive Disconnected.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                isSignInLoading = isSignInLoading,
                                authErrorMessage = authErrorMessage,
                                lastCloudBackupDate = lastCloudBackupDate,
                                lastCloudBackupFileName = lastCloudBackupFileName,
                                lastCloudBackupStatus = lastCloudBackupStatus,
                                isBackupInProgress = isBackupInProgress,
                                onBackupNowToDrive = {
                                    settingsViewModel.performBackupToGoogleDrive { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                onShowRestoreDriveDialog = {
                                    settingsViewModel.fetchCloudBackupsList()
                                    isCloudRestoreListDialogOpen = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---
    if (isCloudRestoreListDialogOpen) {
        AlertDialog(
            onDismissRequest = { isCloudRestoreListDialogOpen = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Available Google Drive Backups") },
            text = {
                if (isLoadingCloudBackups) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(Spacing.S))
                            Text("Fetching backups from Google Drive...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else if (cloudBackupsListState.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No cloud backups found in Google Drive.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        cloudBackupsListState.forEach { fileInfo ->
                            val dateStr = try {
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(fileInfo.modifiedTime))
                            } catch (e: Exception) {
                                "Unknown date"
                            }
                            val sizeKb = fileInfo.sizeBytes / 1024
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(Spacing.M),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fileInfo.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Date: $dateStr • ${sizeKb} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(
                                        onClick = {
                                            specificRestoreFileId = fileInfo.id
                                            specificRestoreFileName = fileInfo.name
                                            isCloudRestoreListDialogOpen = false
                                            isConfirmSpecificRestoreDialogOpen = true
                                        }
                                    ) {
                                        Text("Restore", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isCloudRestoreListDialogOpen = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (isConfirmSpecificRestoreDialogOpen) {
        AlertDialog(
            onDismissRequest = { isConfirmSpecificRestoreDialogOpen = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Cloud Restore") },
            text = {
                Text("Are you sure you want to restore from cloud backup '$specificRestoreFileName'?\n\nYour existing local database will be safely replaced with the backup data.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isConfirmSpecificRestoreDialogOpen = false
                        settingsViewModel.restoreSpecificBackup(specificRestoreFileId) { success ->
                            if (success) {
                                Toast.makeText(context, "Database successfully restored from Google Drive!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to restore backup from Google Drive.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { isConfirmSpecificRestoreDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsProfileAndBankTab(
    coName: String, onCoNameChange: (String) -> Unit,
    tagline: String, onTaglineChange: (String) -> Unit,
    ownerName: String, onOwnerNameChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    whatsappNumber: String, onWhatsappChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    website: String, onWebsiteChange: (String) -> Unit,
    gstin: String, onGstinChange: (String) -> Unit,
    address: String, onAddressChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    district: String, onDistrictChange: (String) -> Unit,
    state: String, onStateChange: (String) -> Unit,
    pincode: String, onPincodeChange: (String) -> Unit,
    bankName: String, onBankNameChange: (String) -> Unit,
    accountHolderName: String, onAccountHolderNameChange: (String) -> Unit,
    bankAccount: String, onBankAccountChange: (String) -> Unit,
    bankIfsc: String, onBankIfscChange: (String) -> Unit,
    bankBranch: String, onBankBranchChange: (String) -> Unit,
    upiId: String, onUpiIdChange: (String) -> Unit,
    coNameError: String?, phoneError: String?, emailError: String?, gstinError: String?
) {
    SettingsSectionHeader(title = "Company Details")
    SettingsCard {
        Column(modifier = Modifier.padding(Spacing.L), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
            PremiumOutlinedTextField(
                value = coName, onValueChange = onCoNameChange,
                label = "Company Name *", placeholder = "Eg: InteriorPro Services", modifier = Modifier.fillMaxWidth(),
                isError = coNameError != null, errorMessage = coNameError
            )
            PremiumOutlinedTextField(
                value = tagline, onValueChange = onTaglineChange,
                label = "Tagline", modifier = Modifier.fillMaxWidth()
            )
            PremiumOutlinedTextField(
                value = ownerName, onValueChange = onOwnerNameChange,
                label = "Owner / Contact Person", placeholder = "Eg: Mr. Ramesh", modifier = Modifier.fillMaxWidth()
            )
            PremiumOutlinedTextField(
                value = phone, onValueChange = onPhoneChange,
                label = "Phone Number *", placeholder = "Eg: 9876543210", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                isError = phoneError != null, errorMessage = phoneError
            )
            PremiumOutlinedTextField(
                value = whatsappNumber, onValueChange = onWhatsappChange,
                label = "WhatsApp Number", placeholder = "Eg: 9876543210", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            PremiumOutlinedTextField(
                value = email, onValueChange = onEmailChange,
                label = "Email Address *", placeholder = "Eg: contact@company.com", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                isError = emailError != null, errorMessage = emailError
            )
            PremiumOutlinedTextField(
                value = website, onValueChange = onWebsiteChange,
                label = "Website", placeholder = "Eg: www.company.com", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            PremiumOutlinedTextField(
                value = gstin, onValueChange = onGstinChange,
                label = "GSTIN / TAX ID", placeholder = "Eg: 33ABCDE1234F1Z5", modifier = Modifier.fillMaxWidth(),
                isError = gstinError != null, errorMessage = gstinError
            )
        }
    }

    Spacer(modifier = Modifier.height(Spacing.M))
    SettingsSectionHeader(title = "Address")
    SettingsCard {
        Column(modifier = Modifier.padding(Spacing.L), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
            PremiumOutlinedTextField(
                value = address, onValueChange = onAddressChange,
                label = "Street Address", placeholder = "Eg: 123, ABC Street", modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                PremiumOutlinedTextField(
                    value = city, onValueChange = onCityChange,
                    label = "City", modifier = Modifier.weight(1f)
                )
                PremiumOutlinedTextField(
                    value = pincode, onValueChange = onPincodeChange,
                    label = "Pincode", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                PremiumOutlinedTextField(
                    value = district, onValueChange = onDistrictChange,
                    label = "District", modifier = Modifier.weight(1f)
                )
                PremiumOutlinedTextField(
                    value = state, onValueChange = onStateChange,
                    label = "State", modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.M))
    SettingsSectionHeader(title = "Bank Details")
    SettingsCard {
        Column(modifier = Modifier.padding(Spacing.L), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
            PremiumOutlinedTextField(
                value = bankName, onValueChange = onBankNameChange,
                label = "Bank Name", placeholder = "Eg: HDFC Bank", modifier = Modifier.fillMaxWidth()
            )
            PremiumOutlinedTextField(
                value = accountHolderName, onValueChange = onAccountHolderNameChange,
                label = "Account Holder Name", placeholder = "Eg: InteriorPro Services", modifier = Modifier.fillMaxWidth()
            )
            PremiumOutlinedTextField(
                value = bankAccount, onValueChange = onBankAccountChange,
                label = "Account Number", placeholder = "Eg: 1234567890", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                PremiumOutlinedTextField(
                    value = bankIfsc, onValueChange = onBankIfscChange,
                    label = "IFSC Code", placeholder = "Eg: HDFC0001234", modifier = Modifier.weight(1f)
                )
                PremiumOutlinedTextField(
                    value = bankBranch, onValueChange = onBankBranchChange,
                    label = "Branch", placeholder = "Eg: Main Branch", modifier = Modifier.weight(1f)
                )
            }
            PremiumOutlinedTextField(
                value = upiId, onValueChange = onUpiIdChange,
                label = "UPI ID", placeholder = "Eg: interiorpro@upi", modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsQuotationDefaultsTab(
    defaultGstRateStr: String, onDefaultGstRateStrChange: (String) -> Unit,
    defaultDiscountStr: String, onDefaultDiscountStrChange: (String) -> Unit,
    defaultValidityDaysStr: String, onDefaultValidityDaysStrChange: (String) -> Unit,
    defaultDeliveryDaysStr: String, onDefaultDeliveryDaysStrChange: (String) -> Unit,
    defaultWarranty: String, onDefaultWarrantyChange: (String) -> Unit,
    defaultDeliveryTime: String, onDefaultDeliveryTimeChange: (String) -> Unit,
    defaultInstallationTime: String, onDefaultInstallationTimeChange: (String) -> Unit,
    defaultPaymentTerms: String, onDefaultPaymentTermsChange: (String) -> Unit,
    additionalConditions: String, onAdditionalConditionsChange: (String) -> Unit,
    termsAndConditions: String, onTermsAndConditionsChange: (String) -> Unit,
    paymentTermsExpanded: Boolean, onPaymentTermsExpandedChange: (Boolean) -> Unit,
    paymentTermsMaster: List<com.example.data.MasterEntity>
) {
    SettingsSectionHeader(title = "Quotation Defaults")
    SettingsCard {
        Column(modifier = Modifier.padding(Spacing.L), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                PremiumOutlinedTextField(
                    value = defaultGstRateStr, onValueChange = onDefaultGstRateStrChange,
                    label = "Default GST (%)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                PremiumOutlinedTextField(
                    value = defaultDiscountStr, onValueChange = onDefaultDiscountStrChange,
                    label = "Default Discount (%)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
                PremiumOutlinedTextField(
                    value = defaultValidityDaysStr, onValueChange = onDefaultValidityDaysStrChange,
                    label = "Validity (Days)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                PremiumOutlinedTextField(
                    value = defaultDeliveryDaysStr, onValueChange = onDefaultDeliveryDaysStrChange,
                    label = "Delivery (Days)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.M))
    SettingsSectionHeader(title = "Terms & Conditions (PDF)")
    SettingsCard {
        Column(modifier = Modifier.padding(Spacing.L), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
            PremiumOutlinedTextField(
                value = defaultWarranty, onValueChange = onDefaultWarrantyChange,
                label = "Default Warranty", modifier = Modifier.fillMaxWidth()
            )
            PremiumOutlinedTextField(
                value = defaultDeliveryTime, onValueChange = onDefaultDeliveryTimeChange,
                label = "Default Delivery Time", modifier = Modifier.fillMaxWidth()
            )
            PremiumOutlinedTextField(
                value = defaultInstallationTime, onValueChange = onDefaultInstallationTimeChange,
                label = "Default Installation Time", modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = paymentTermsExpanded,
                onExpandedChange = onPaymentTermsExpandedChange,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = defaultPaymentTerms,
                    onValueChange = onDefaultPaymentTermsChange,
                    label = { Text("Default Payment Terms") },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentTermsExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = paymentTermsExpanded,
                    onDismissRequest = { onPaymentTermsExpandedChange(false) }
                ) {
                    if (paymentTermsMaster.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No Payment Terms found (Add in Masters)") },
                            onClick = { onPaymentTermsExpandedChange(false) }
                        )
                    } else {
                        paymentTermsMaster.forEach { term ->
                            DropdownMenuItem(
                                text = { Text(term.name) },
                                onClick = {
                                    onDefaultPaymentTermsChange(term.name)
                                    onPaymentTermsExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }

            PremiumOutlinedTextField(
                value = additionalConditions, onValueChange = onAdditionalConditionsChange,
                label = "Additional Conditions", modifier = Modifier.fillMaxWidth()
            )
            PremiumOutlinedTextField(
                value = termsAndConditions, onValueChange = onTermsAndConditionsChange,
                label = "Custom Terms & Conditions (Multiline)", modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsPdfPreferencesTab(
    pdfShowLogo: Boolean, onPdfShowLogoChange: (Boolean) -> Unit,
    pdfShowGst: Boolean, onPdfShowGstChange: (Boolean) -> Unit,
    pdfShowWebsite: Boolean, onPdfShowWebsiteChange: (Boolean) -> Unit,
    pdfShowWhatsapp: Boolean, onPdfShowWhatsappChange: (Boolean) -> Unit,
    pdfShowValidUntil: Boolean, onPdfShowValidUntilChange: (Boolean) -> Unit,
    pdfShowQrCode: Boolean, onPdfShowQrCodeChange: (Boolean) -> Unit,
    pdfShowBankDetails: Boolean, onPdfShowBankDetailsChange: (Boolean) -> Unit,
    pdfShowAmountInWords: Boolean, onPdfShowAmountInWordsChange: (Boolean) -> Unit,
    pdfShowCompanySeal: Boolean, onPdfShowCompanySealChange: (Boolean) -> Unit,
    pdfShowSignature: Boolean, onPdfShowSignatureChange: (Boolean) -> Unit,
    pdfShowTermsConditions: Boolean, onPdfShowTermsConditionsChange: (Boolean) -> Unit,
    pdfShowPageNumber: Boolean, onPdfShowPageNumberChange: (Boolean) -> Unit
) {
    SettingsSectionHeader(title = "PDF Display Configuration")
    SettingsCard {
        Column(modifier = Modifier.padding(vertical = Spacing.S)) {
            SwitchRow("Company Logo", "Display company logo on top left of PDF", pdfShowLogo, onPdfShowLogoChange, Icons.Default.Business)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("GST / PAN Details", "Show GSTIN and Tax identification", pdfShowGst, onPdfShowGstChange, Icons.Default.Receipt)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("Show Website", "Display company website URL", pdfShowWebsite, onPdfShowWebsiteChange, Icons.Default.Language)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("WhatsApp Number", "Display WhatsApp contact line", pdfShowWhatsapp, onPdfShowWhatsappChange, Icons.Default.PhoneAndroid)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("Valid Until Date", "Show proposal expiration date", pdfShowValidUntil, onPdfShowValidUntilChange, Icons.Default.Event)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("UPI Payment QR", "Embed UPI QR code for direct scanning", pdfShowQrCode, onPdfShowQrCodeChange, Icons.Default.QrCode)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("Bank Account Info", "Include bank account & IFSC block", pdfShowBankDetails, onPdfShowBankDetailsChange, Icons.Default.AccountBalance)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("Amount in Words", "Spell out grand total in Indian Words", pdfShowAmountInWords, onPdfShowAmountInWordsChange, Icons.Default.Description)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("Company Seal", "Render official company seal image", pdfShowCompanySeal, onPdfShowCompanySealChange, Icons.Default.CheckCircle)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("Signature", "Include authorized signature image", pdfShowSignature, onPdfShowSignatureChange, Icons.Default.Create)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("Terms & Conditions", "Append legal terms & conditions footer", pdfShowTermsConditions, onPdfShowTermsConditionsChange, Icons.Default.Gavel)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SwitchRow("Page Numbers", "Show page numbers (Page X of Y)", pdfShowPageNumber, onPdfShowPageNumberChange, Icons.Default.Numbers)
        }
    }
}

@Composable
fun SettingsDataAndBackupTab(
    onCreateWorkspace: () -> Unit,
    onImportWorkspace: () -> Unit,
    lastBackupDate: String,
    onNavigateToMasters: () -> Unit,
    onNavigateToSyncDashboard: () -> Unit,
    themeManager: ThemeManager,
    onNavigateToAbout: () -> Unit,
    isUserSignedIn: Boolean,
    currentUserEmail: String?,
    currentUserDisplayName: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    isSignInLoading: Boolean,
    authErrorMessage: String?,
    lastCloudBackupDate: String = "Never",
    lastCloudBackupFileName: String = "None",
    lastCloudBackupStatus: String = "Idle",
    isBackupInProgress: Boolean = false,
    onBackupNowToDrive: () -> Unit = {},
    onShowRestoreDriveDialog: () -> Unit = {}
) {
    SettingsSectionHeader(title = "Google Drive Integration & Cloud Sync")
    SettingsCard {
        Column(modifier = Modifier.padding(Spacing.L)) {
            if (isUserSignedIn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(Spacing.M))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUserDisplayName ?: "Google Drive Connected",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentUserEmail ?: "Connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PremiumBadge(text = "Connected", containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32))
                }
                Spacer(modifier = Modifier.height(Spacing.M))

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(CornerRadius.Medium),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.M)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Last Backup Date:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(lastCloudBackupDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(Spacing.XXS))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Backup File Name:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(lastCloudBackupFileName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(Spacing.XXS))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Backup Status:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val isSuccess = lastCloudBackupStatus.startsWith("Success")
                            val isFailed = lastCloudBackupStatus.startsWith("Failed")
                            PremiumBadge(
                                text = lastCloudBackupStatus,
                                containerColor = if (isSuccess) Color(0xFFE8F5E9) else if (isFailed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSuccess) Color(0xFF2E7D32) else if (isFailed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.M))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.S)
                ) {
                    PremiumPrimaryButton(
                        onClick = onBackupNowToDrive,
                        enabled = !isBackupInProgress && !isSignInLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isBackupInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(Spacing.XS))
                            Text("Backing up...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(Spacing.XS))
                            Text("Backup Now", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    PremiumOutlinedButton(
                        onClick = onShowRestoreDriveDialog,
                        enabled = !isBackupInProgress && !isSignInLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(Spacing.XS))
                        Text("Restore Backup", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.S))
                Button(
                    onClick = onSignOut,
                    enabled = !isSignInLoading && !isBackupInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.Medium)
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.S))
                    Text("Disconnect Google Account")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(Spacing.M))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("Not Connected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.S))
                Text(
                    text = "Connect your Google Account to enable automatic cloud backups, status monitoring, and cross-device workspace synchronization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (authErrorMessage != null) {
                    Spacer(modifier = Modifier.height(Spacing.S))
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(CornerRadius.Small)) {
                        Row(modifier = Modifier.padding(Spacing.S), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(Spacing.XS))
                            Text(text = authErrorMessage, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.M))
                PremiumPrimaryButton(
                    onClick = onSignIn,
                    enabled = !isSignInLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSignInLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(Spacing.S))
                        Text("Connecting...")
                    } else {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(Spacing.S))
                        Text("Connect Google Account")
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(Spacing.M))
    SettingsSectionHeader(title = "App Preferences & Theme")
    SettingsCard {
        val isDark = themeManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM).value == ThemeMode.DARK
        SwitchRow(
            title = "Dark Theme",
            subtitle = if (isDark) "Dark mode enabled" else "Light mode enabled",
            checked = isDark,
            onCheckedChange = {
                themeManager.setThemeMode(if (isDark) ThemeMode.LIGHT else ThemeMode.DARK)
            },
            icon = Icons.Default.Palette
        )
    }
}

@Composable
fun SettingsBrandingTab(
    logoPath: String, onLogoPathChange: (String) -> Unit,
    signaturePath: String, onSignaturePathChange: (String) -> Unit,
    companySealPath: String, onCompanySealPathChange: (String) -> Unit,
    signatureText: String, onSignatureTextChange: (String) -> Unit,
    logoRefreshKey: Long, onLogoRefreshKeyChange: (Long) -> Unit,
    sigRefreshKey: Long, onSigRefreshKeyChange: (Long) -> Unit,
    sealRefreshKey: Long, onSealRefreshKeyChange: (Long) -> Unit
) {
    val context = LocalContext.current
    
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val path = copyUriToInternalStorage(context, it, "logo.png")
            if (path != null) {
                onLogoPathChange(path)
                onLogoRefreshKeyChange(System.currentTimeMillis())
            }
        }
    }
    
    val signatureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val path = copyUriToInternalStorage(context, it, "signature.png")
            if (path != null) {
                onSignaturePathChange(path)
                onSigRefreshKeyChange(System.currentTimeMillis())
            }
        }
    }
    
    val sealLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val path = copyUriToInternalStorage(context, it, "company_seal.png")
            if (path != null) {
                onCompanySealPathChange(path)
                onSealRefreshKeyChange(System.currentTimeMillis())
            }
        }
    }

    SettingsSectionHeader(title = "Branding Assets & Digital Artifacts")
    SettingsCard {
        Column(modifier = Modifier.padding(Spacing.L), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
            Text("Upload official branding assets for exported PDF quotations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Company Logo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    if (logoPath.isNotEmpty()) {
                        Text("Saved: ${logoPath.takeLast(20)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                PremiumOutlinedButton(onClick = { logoLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.XS))
                    Text("Upload")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Authorized Signature", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    if (signaturePath.isNotEmpty()) {
                        Text("Saved: ${signaturePath.takeLast(20)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                PremiumOutlinedButton(onClick = { signatureLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.XS))
                    Text("Upload")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Company Seal / Stamp", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    if (companySealPath.isNotEmpty()) {
                        Text("Saved: ${companySealPath.takeLast(20)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                PremiumOutlinedButton(onClick = { sealLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.XS))
                    Text("Upload")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            PremiumOutlinedTextField(
                value = signatureText, onValueChange = onSignatureTextChange,
                label = "Signature Sub-text (e.g. Authorized Signatory)", modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
