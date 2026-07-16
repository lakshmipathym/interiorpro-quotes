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

        // Backup & Restore Card
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
                        imageVector = Icons.Filled.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data Backup & Restore",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "InteriorPro Quotes operates 100% offline. Create secure JSON-based backups to safeguard your data or migrate to another Android device.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Last Backup: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = lastBackupDate ?: "Never",
                        fontSize = 12.sp,
                        color = if (lastBackupDate != null && lastBackupDate != "Never") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            createBackupLauncher.launch("InteriorProQuotes_Backup.json")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
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
