package com.example.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: com.example.ui.settings.SettingsViewModel,
    companyViewModel: com.example.ui.company.CompanyViewModel,
    onNavigateToMasters: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val companyProfile by companyViewModel.companyProfile.collectAsState()

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

    // Terms
    var termsAndConditions by remember { mutableStateOf("") }
    var defaultWarranty by remember { mutableStateOf("") }
    var defaultDeliveryTime by remember { mutableStateOf("") }
    var defaultInstallationTime by remember { mutableStateOf("") }
    var defaultPaymentTerms by remember { mutableStateOf("") }
    var defaultQuoteValidity by remember { mutableStateOf("") }
    var additionalConditions by remember { mutableStateOf("") }

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

    // Sprint 3 State tracking
    val syncState by settingsViewModel.syncState.collectAsState()
    val isUserSignedIn by settingsViewModel.isUserSignedIn.collectAsState()
    val currentUserEmail by settingsViewModel.currentUserEmail.collectAsState()
    val currentUserDisplayName by settingsViewModel.currentUserDisplayName.collectAsState()

    var cloudBackupsList by remember { mutableStateOf<List<com.example.core.drive.DriveFileInfo>>(emptyList()) }
    var isRefreshingCloudList by remember { mutableStateOf(false) }

    var workspacePreviewState by remember { mutableStateOf<com.example.core.backup.WorkspacePreview?>(null) }
    var isConfirmWorkspaceImportDialogOpen by remember { mutableStateOf(false) }

    var isConfirmSpecificRestoreDialogOpen by remember { mutableStateOf(false) }
    var specificRestoreFileId by remember { mutableStateOf("") }
    var specificRestoreFileName by remember { mutableStateOf("") }

    LaunchedEffect(isUserSignedIn) {
        if (isUserSignedIn) {
            try {
                cloudBackupsList = settingsViewModel.listCloudBackups()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Backup & Restore activity result launchers
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
                        Toast.makeText(context, "Failed to save backup file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val jsonContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }
                    if (jsonContent != null) {
                        val isValid = settingsViewModel.validateBackupData(jsonContent)
                        if (isValid) {
                            pendingRestoreJson = jsonContent
                            isConfirmRestoreDialogOpen = true
                        } else {
                            Toast.makeText(context, "Invalid backup file structure or corrupted data.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to read backup file.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error reading backup: ${e.message}", Toast.LENGTH_LONG).show()
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
                    if (file != null && file.exists()) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.write(file.readBytes())
                            }
                            tempFile.delete()
                            Toast.makeText(context, "Workspace exported successfully!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export writing failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Workspace packaging failed.", Toast.LENGTH_LONG).show()
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
                try {
                    val tempFile = File(context.cacheDir, "temp_import.ipro")
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    settingsViewModel.verifyAndPreviewWorkspaceBundle(tempFile) { preview ->
                        tempFile.delete()
                        if (preview.isValid) {
                            workspacePreviewState = preview
                            isConfirmWorkspaceImportDialogOpen = true
                        } else {
                            Toast.makeText(context, "Verification failed: ${preview.errorReason}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Launchers for choosing images
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedPath = copyUriToInternalStorage(context, it, "company_logo.png")
            if (copiedPath != null) {
                logoPath = copiedPath
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
            if (!validateSignatureImage(context, it)) {
                Toast.makeText(context, "Invalid Signature: Please upload a horizontally cropped signature image, not a square or vertical photo.", Toast.LENGTH_LONG).show()
                return@let
            }
            val copiedPath = copyUriToInternalStorage(context, it, "auth_signature.png")
            if (copiedPath != null) {
                signaturePath = copiedPath
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
                Toast.makeText(context, "Company Seal updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to copy seal image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load initial values when companyProfile loads
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
            signatureText = it.signatureText

            // Module 3 Fields Loading
            tagline = it.tagline
            companySealPath = it.companySealPath
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

    fun validateEmail(mail: String): Boolean {
        if (mail.trim().isEmpty()) return true
        return com.example.utils.ValidationManager.isValidEmail(mail)
    }

    fun validateGst(gst: String): Boolean {
        if (gst.trim().isEmpty()) return true
        return com.example.utils.ValidationManager.isValidGstin(gst)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Company Profile Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Set up your business identity, document images, default parameters, and payment information. All settings are saved offline.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // SECTION 1: Company Information
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Company Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Logo Picker Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { logoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoPath.isNotEmpty() && File(logoPath).exists()) {
                            AsyncImage(
                                model = File(logoPath),
                                contentDescription = "Company Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Add Logo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("Logo", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text("Company Logo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Appears at the header of all generated PDFs.", fontSize = 11.sp, color = Color.Gray)
                        if (logoPath.isNotEmpty()) {
                            Row {
                                TextButton(onClick = { logoPickerLauncher.launch("image/*") }) {
                                    Text("Replace Logo", fontSize = 12.sp)
                                }
                                TextButton(onClick = { logoPath = "" }) {
                                    Text("Delete Logo", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Company Name field
                OutlinedTextField(
                    value = coName,
                    onValueChange = {
                        coName = it
                        coNameError = if (it.trim().isEmpty()) "Company Name is required" else null
                    },
                    label = { Text("Company Name *") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    isError = coNameError != null,
                    supportingText = { coNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Business Tagline
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Business Tagline (Optional)") },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    placeholder = { Text("e.g. Premium Interior Designing Solutions") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Owner Name
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Representative / Owner Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Phone Number
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        phoneError = if (it.trim().isEmpty()) "Phone Number is required" else null
                    },
                    label = { Text("Phone Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    isError = phoneError != null,
                    supportingText = { phoneError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Email Address
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = if (!validateEmail(it)) "Invalid Email Address" else null
                    },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // GST Number
                OutlinedTextField(
                    value = gstin,
                    onValueChange = {
                        gstin = it
                        gstinError = if (!validateGst(it)) "Invalid GSTIN format (15 Alphanumeric)" else null
                    },
                    label = { Text("GST Number") },
                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    isError = gstinError != null,
                    supportingText = { gstinError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Website
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                    placeholder = { Text("e.g. www.interiorpro.com") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Address fields
                Text("Address Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street Address") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pincode,
                        onValueChange = { pincode = it },
                        label = { Text("Pincode") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("District") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // SECTION 2: Document Settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Documents",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Signature Box
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Signature Image", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { signaturePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (signaturePath.isNotEmpty() && File(signaturePath).exists()) {
                                AsyncImage(
                                    model = File(signaturePath),
                                    contentDescription = "Signature Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                               )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Edit, contentDescription = "Add Signature", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Add Sig", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (signaturePath.isNotEmpty()) {
                            TextButton(onClick = { signaturePath = "" }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            }
                        }
                    }

                    // Company Seal Box (Future Ready)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Company Seal", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { companySealPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (companySealPath.isNotEmpty() && File(companySealPath).exists()) {
                                AsyncImage(
                                    model = File(companySealPath),
                                    contentDescription = "Seal Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = "Add Seal", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Seal (Future)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (companySealPath.isNotEmpty()) {
                            TextButton(onClick = { companySealPath = "" }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = signatureText,
                    onValueChange = { signatureText = it },
                    label = { Text("Signature Representative Label") },
                    placeholder = { Text("e.g. Proprietor / Authorized Signatory") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // SECTION 3: Quotation Defaults
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quotation Defaults",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = defaultGstRateStr,
                        onValueChange = { defaultGstRateStr = it },
                        label = { Text("Default GST %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = defaultDiscountStr,
                        onValueChange = { defaultDiscountStr = it },
                        label = { Text("Default Discount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = defaultValidityDaysStr,
                        onValueChange = { defaultValidityDaysStr = it },
                        label = { Text("Validity (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = defaultDeliveryDaysStr,
                        onValueChange = { defaultDeliveryDaysStr = it },
                        label = { Text("Est. Delivery Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // SECTION 4: Terms & Conditions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Terms & Conditions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "These values will dynamically print in the PDF Terms & Conditions section.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = defaultWarranty,
                    onValueChange = { defaultWarranty = it },
                    label = { Text("Default Warranty") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = defaultDeliveryTime,
                    onValueChange = { defaultDeliveryTime = it },
                    label = { Text("Default Delivery Time") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = defaultInstallationTime,
                    onValueChange = { defaultInstallationTime = it },
                    label = { Text("Default Installation Time") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = defaultPaymentTerms,
                    onValueChange = { defaultPaymentTerms = it },
                    label = { Text("Default Payment Terms") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = defaultQuoteValidity,
                    onValueChange = { defaultQuoteValidity = it },
                    label = { Text("Default Quote Validity") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = additionalConditions,
                    onValueChange = { additionalConditions = it },
                    label = { Text("Default Additional Conditions") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = termsAndConditions,
                    onValueChange = { termsAndConditions = it },
                    label = { Text("Custom Terms & Conditions (Multiline)") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // SECTION 5: Bank & Payment Details (Actively Used for PDF Printing)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bank & Payment Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = accountHolderName,
                    onValueChange = { accountHolderName = it },
                    label = { Text("Account Holder Name") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bankAccount,
                    onValueChange = { bankAccount = it },
                    label = { Text("Account Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bankIfsc,
                        onValueChange = { bankIfsc = it },
                        label = { Text("IFSC Code") },
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = bankBranch,
                        onValueChange = { bankBranch = it },
                        label = { Text("Branch") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (For QR Payments)") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // SECTION 6: PDF Preferences (Settings)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PDF Preferences & Document Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        PreferenceSwitchRow(
                            title = "Company Logo",
                            checked = pdfShowLogo,
                            onCheckedChange = { pdfShowLogo = it; pdfPrefs.edit().putBoolean("pdf_show_logo", it).apply() },
                            icon = Icons.Default.Business
                        )
                        PreferenceSwitchRow(
                            title = "GST / PAN Details",
                            checked = pdfShowGst,
                            onCheckedChange = { pdfShowGst = it; pdfPrefs.edit().putBoolean("pdf_show_gst", it).apply() },
                            icon = Icons.Default.Receipt
                        )
                        PreferenceSwitchRow(
                            title = "Show Website",
                            checked = pdfShowWebsite,
                            onCheckedChange = { pdfShowWebsite = it; pdfPrefs.edit().putBoolean("pdf_show_website", it).apply() },
                            icon = Icons.Default.Language
                        )
                        PreferenceSwitchRow(
                            title = "WhatsApp Number",
                            checked = pdfShowWhatsapp,
                            onCheckedChange = { pdfShowWhatsapp = it; pdfPrefs.edit().putBoolean("pdf_show_whatsapp", it).apply() },
                            icon = Icons.Default.PhoneAndroid
                        )
                        PreferenceSwitchRow(
                            title = "Valid Until Date",
                            checked = pdfShowValidUntil,
                            onCheckedChange = { pdfShowValidUntil = it; pdfPrefs.edit().putBoolean("pdf_show_valid_until", it).apply() },
                            icon = Icons.Default.Event
                        )
                        PreferenceSwitchRow(
                            title = "UPI Payment QR",
                            checked = pdfShowQrCode,
                            onCheckedChange = { pdfShowQrCode = it; pdfPrefs.edit().putBoolean("pdf_show_qr_code", it).apply() },
                            icon = Icons.Default.QrCode
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        PreferenceSwitchRow(
                            title = "Bank Account Info",
                            checked = pdfShowBankDetails,
                            onCheckedChange = { pdfShowBankDetails = it; pdfPrefs.edit().putBoolean("pdf_show_bank_details", it).apply() },
                            icon = Icons.Default.AccountBalance
                        )
                        PreferenceSwitchRow(
                            title = "Amount in Words",
                            checked = pdfShowAmountInWords,
                            onCheckedChange = { pdfShowAmountInWords = it; pdfPrefs.edit().putBoolean("pdf_show_amount_in_words", it).apply() },
                            icon = Icons.Default.Description
                        )
                        PreferenceSwitchRow(
                            title = "Company Seal",
                            checked = pdfShowCompanySeal,
                            onCheckedChange = { pdfShowCompanySeal = it; pdfPrefs.edit().putBoolean("pdf_show_company_seal", it).apply() },
                            icon = Icons.Default.CheckCircle
                        )
                        PreferenceSwitchRow(
                            title = "Signature Image",
                            checked = pdfShowSignature,
                            onCheckedChange = { pdfShowSignature = it; pdfPrefs.edit().putBoolean("pdf_show_signature", it).apply() },
                            icon = Icons.Default.Edit
                        )
                        PreferenceSwitchRow(
                            title = "Terms & Conditions",
                            checked = pdfShowTermsConditions,
                            onCheckedChange = { pdfShowTermsConditions = it; pdfPrefs.edit().putBoolean("pdf_show_terms_conditions", it).apply() },
                            icon = Icons.Default.ListAlt
                        )
                        PreferenceSwitchRow(
                            title = "Footer Page Numbers",
                            checked = pdfShowPageNumber,
                            onCheckedChange = { pdfShowPageNumber = it; pdfPrefs.edit().putBoolean("pdf_show_page_number", it).apply() },
                            icon = Icons.Default.List
                        )
                    }
                }
            }
        }

        // SAVE BUTTON
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
                    companyViewModel.saveCompanyProfile(
                        CompanyProfile(
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
                            logoPath = logoPath.trim(),
                            signaturePath = signaturePath.trim(),
                            signatureText = signatureText.trim(),
                            
                            // Module 3 additions saved
                            tagline = tagline.trim(),
                            companySealPath = companySealPath.trim(),
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
                    )
                    Toast.makeText(context, "Company profile saved offline successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please fix all validation errors before saving.", Toast.LENGTH_LONG).show()
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Profile Configurations", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Manage Masters Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Master Data Management",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "Configure default materials, spec tables, pricing ranges, and parameters used globally across quotation profiles.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { onNavigateToMasters() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Masters", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- SPRINT 3: ENTERPRISE SMART SYNC DASHBOARD & BACKUP CONSOLE ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Dashboard Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cloud Sync & Auto Backup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "Enterprise-grade secure synchronization with your private Google Drive folder. Sync quotations, clients, and assets automatically.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Connection and Account Status Row
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUserSignedIn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isUserSignedIn) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = if (isUserSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isUserSignedIn) "Google Account Connected" else "Account Disconnected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isUserSignedIn) (currentUserEmail ?: "Connected") else "Sync is disabled until connected",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Button(
                            onClick = {
                                if (isUserSignedIn) {
                                    settingsViewModel.signOut {
                                        Toast.makeText(context, "Account disconnected.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    settingsViewModel.signIn { success ->
                                        if (success) {
                                            Toast.makeText(context, "Successfully connected!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Connection failed or cancelled.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isUserSignedIn) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (isUserSignedIn) "Disconnect" else "Connect",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Sync Status block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (statusText, statusColor, showProgress) = when (syncState) {
                            is com.example.core.sync.SyncState.Idle -> Triple("Idle (Up to date)", MaterialTheme.colorScheme.outline, false)
                            is com.example.core.sync.SyncState.Syncing,
                            is com.example.core.sync.SyncState.Uploading,
                            is com.example.core.sync.SyncState.Downloading -> Triple("Syncing with cloud...", MaterialTheme.colorScheme.primary, true)
                            is com.example.core.sync.SyncState.Success -> Triple("Sync Completed", MaterialTheme.colorScheme.primary, false)
                            is com.example.core.sync.SyncState.Failed -> Triple("Sync Failed", MaterialTheme.colorScheme.error, false)
                            is com.example.core.sync.SyncState.Conflict -> Triple("Conflict Detected", MaterialTheme.colorScheme.error, false)
                            is com.example.core.sync.SyncState.WaitingForInternet -> Triple("Waiting for Internet", MaterialTheme.colorScheme.tertiary, false)
                            is com.example.core.sync.SyncState.NotConnected -> Triple("Not Connected", MaterialTheme.colorScheme.outline, false)
                            else -> Triple("Offline Mode", MaterialTheme.colorScheme.outline, false)
                        }
                        
                        if (showProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = statusColor)
                        } else {
                            Icon(
                                imageVector = if (syncState is com.example.core.sync.SyncState.Failed) Icons.Filled.ErrorOutline else Icons.Filled.Info,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sync Status: $statusText",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }

                    Text(
                        text = "v${settingsViewModel.deviceManager.getAppVersion()} (${settingsViewModel.deviceManager.getDatabaseVersion()})",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-Backup Policy Segment
                Text(
                    text = "Automatic Backup Policy:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                var activePolicy by remember { mutableStateOf(settingsViewModel.getAutoBackupPolicy()) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val policies = listOf(
                        "MANUAL" to "Manual",
                        "ON_SAVE" to "On Save",
                        "DAILY" to "Daily",
                        "WEEKLY" to "Weekly"
                    )
                    policies.forEach { (key, label) ->
                        val isSelected = activePolicy == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    activePolicy = key
                                    settingsViewModel.setAutoBackupPolicy(key)
                                    Toast.makeText(context, "Auto backup scheduled: $label", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Metadata block
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Device ID", fontSize = 11.sp, color = Color.Gray)
                            Text(settingsViewModel.deviceManager.getDeviceName(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Last Sync", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = if (settingsViewModel.deviceManager.getLastSyncTime() > 0) {
                                    java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(settingsViewModel.deviceManager.getLastSyncTime()))
                                } else "Never",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Stored Backups", fontSize = 11.sp, color = Color.Gray)
                            Text("${cloudBackupsList.size} snapshot(s)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sync controls button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isUserSignedIn) {
                                Toast.makeText(context, "Please connect Google Account first", Toast.LENGTH_SHORT).show()
                            } else {
                                settingsViewModel.triggerSync { result ->
                                    if (result is com.example.core.sync.SyncResult.Success) {
                                        Toast.makeText(context, "Sync completed successfully!", Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            cloudBackupsList = settingsViewModel.listCloudBackups()
                                        }
                                    } else if (result is com.example.core.sync.SyncResult.Failure) {
                                        Toast.makeText(context, "Sync failed: ${result.reason}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (!isUserSignedIn) {
                                Toast.makeText(context, "Please connect Google Account first", Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    isRefreshingCloudList = true
                                    cloudBackupsList = settingsViewModel.listCloudBackups()
                                    isRefreshingCloudList = false
                                    Toast.makeText(context, "Backup list refreshed!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isRefreshingCloudList) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh List", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Divider
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(16.dp))

                // --- BACKUP SNAPSHOT HISTORY LIST ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stored Cloud Backups", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!isUserSignedIn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Connect Google Account to view history list", fontSize = 11.sp, color = Color.Gray)
                    }
                } else if (cloudBackupsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRefreshingCloudList) "Loading backups..." else "No stored cloud backups found.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cloudBackupsList.take(5).forEach { file ->
                            val formattedDate = try {
                                val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(file.modifiedTime))
                            } catch (e: Exception) {
                                "Unknown Date"
                            }
                            
                            val sizeInKb = file.sizeBytes / 1024
                            val fileDeviceName = file.metadata["deviceName"] ?: "Unknown Device"
                            val fileAppVersion = file.metadata["appVersion"] ?: "1.5"
                            val fileDbVersion = file.metadata["databaseVersion"] ?: "6"
                            val isCurrentDevice = fileDeviceName == settingsViewModel.deviceManager.getDeviceName()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formattedDate,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isCurrentDevice) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("This Device", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$fileDeviceName • v$fileAppVersion • Schema v$fileDbVersion ($sizeInKb KB)",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                
                                TextButton(
                                    onClick = {
                                        specificRestoreFileId = file.id
                                        specificRestoreFileName = formattedDate
                                        isConfirmSpecificRestoreDialogOpen = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Divider
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(16.dp))

                // --- WORKSPACE EXPORT & IMPORT (OFFLINE ZIP CLOUD/SECURE EXCHANGES) ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.BusinessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Workspace Bundle (.ipro)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "A Workspace Bundle packages your entire offline database, company logos, and designer signatures into a single AES-256 encrypted file. Essential for full offline migrations.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            createWorkspaceLauncher.launch("InteriorPro_Workspace.ipro")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Workspace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            importWorkspaceLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Workspace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // --- CONFIRM RESTORE BACKUP DIALOG ---
    if (isConfirmRestoreDialogOpen) {
        AlertDialog(
            onDismissRequest = { 
                isConfirmRestoreDialogOpen = false 
                pendingRestoreJson = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirm Data Restore",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "WARNING: Restoring will completely overwrite and replace all current local data (Quotations, Customers, Masters, Profiles, and Configurations)!",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This operation is irreversible and all existing data will be lost. Do you want to proceed with the restore?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backupText = pendingRestoreJson
                        isConfirmRestoreDialogOpen = false
                        pendingRestoreJson = ""
                        
                        settingsViewModel.importBackupData(backupText) { success ->
                            if (success) {
                                Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Restore failed. Please make sure the backup file is not modified.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Overwrite & Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isConfirmRestoreDialogOpen = false
                        pendingRestoreJson = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- CONFIRM WORKSPACE IMPORT PREVIEW DIALOG ---
    if (isConfirmWorkspaceImportDialogOpen && workspacePreviewState != null) {
        val preview = workspacePreviewState!!
        AlertDialog(
            onDismissRequest = { 
                isConfirmWorkspaceImportDialogOpen = false
                workspacePreviewState = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Workspace Import", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "WARNING: Importing this workspace will completely overwrite and replace all current local data (Quotations, Customers, Clients, Masters, Profiles, and physical assets such as logos and signatures)!",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Workspace Summary:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Business Name: ${preview.companyName}", fontSize = 12.sp)
                            Text("Quotations count: ${preview.quotationCount}", fontSize = 12.sp)
                            Text("Customers count: ${preview.customerCount}", fontSize = 12.sp)
                            Text("Clients count: ${preview.clientCount}", fontSize = 12.sp)
                            Text("Logo asset: ${if (preview.hasLogo) "Included" else "None"}", fontSize = 12.sp)
                            Text("Signature asset: ${if (preview.hasSignature) "Included" else "None"}", fontSize = 12.sp)
                            Text("Company Seal asset: ${if (preview.hasSeal) "Included" else "None"}", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This operation is irreversible. All current data will be deleted. Do you want to proceed?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rawJson = preview.rawJsonText
                        isConfirmWorkspaceImportDialogOpen = false
                        workspacePreviewState = null
                        
                        settingsViewModel.importWorkspaceBundle(rawJson) { success ->
                                                            if (success) {
                                                                Toast.makeText(context, "Workspace successfully restored!", Toast.LENGTH_LONG).show()
                                                            } else {
                                                                Toast.makeText(context, "Workspace import failed.", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Overwrite & Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isConfirmWorkspaceImportDialogOpen = false
                        workspacePreviewState = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- CONFIRM SPECIFIC RESTORE FROM HISTORY DIALOG ---
    if (isConfirmSpecificRestoreDialogOpen) {
        AlertDialog(
            onDismissRequest = { 
                isConfirmSpecificRestoreDialogOpen = false
                specificRestoreFileId = ""
                specificRestoreFileName = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore History Point", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to restore the backup: $specificRestoreFileName?",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will overwrite your current local database and images with this historic snapshot. This cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileId = specificRestoreFileId
                        isConfirmSpecificRestoreDialogOpen = false
                        specificRestoreFileId = ""
                        specificRestoreFileName = ""
                        
                        settingsViewModel.restoreSpecificBackup(fileId) { success ->
                                                            if (success) {
                                                                Toast.makeText(context, "Successfully restored selected snapshot!", Toast.LENGTH_LONG).show()
                                                            } else {
                                                                Toast.makeText(context, "Failed to restore chosen snapshot.", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore Snapshot")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isConfirmSpecificRestoreDialogOpen = false
                        specificRestoreFileId = ""
                        specificRestoreFileName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { outputStream ->
            inputStream.use { input ->
                input.copyTo(outputStream)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun validateSignatureImage(context: Context, uri: Uri): Boolean {
    return try {
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { inputStream ->
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
        }
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) {
            false
        } else {
            val aspectRatio = width.toFloat() / height.toFloat()
            // Signatures are horizontal. Standard photos are typically square/vertical.
            // A ratio threshold of 1.3 is standard and robust.
            aspectRatio >= 1.3f
        }
    } catch (e: Exception) {
        false
    }
}

@Composable
fun PreferenceSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.5f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}
