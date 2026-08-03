package com.example.ui.quotation

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.*
import com.example.pdf.PdfGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.customer.QuickAddCustomerDialog
import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddPhotoAlternate
import java.io.FileOutputStream

// --- Serialization & Deserialization Helpers for Item Specs ---
data class ItemSpecs(
    val width: String = "",
    val height: String = "",
    val depth: String = "",
    val doorType: String = "",
    val finish: String = "",
    val hardware: String = "",
    val brand: String = "",
    val thickness: String = "",
    val colour: String = "",
    val laminateImageUri: String = "",
    val designImageUri: String = "",
    // Material specific additions
    val profileSeries: String = "",
    val profileColour: String = "",
    val glassType: String = "",
    val glassThickness: String = "",
    val acpColour: String = "",
    val grade: String = "",
    val cncDesign: String = ""
)

fun parseItemSpecs(description: String): Pair<String, ItemSpecs> {
    if (!description.contains("|||")) {
        val trimmed = description.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val json = org.json.JSONObject(trimmed)
                val specs = ItemSpecs(
                    width = json.optString("width", ""),
                    height = json.optString("height", ""),
                    depth = json.optString("depth", ""),
                    doorType = json.optString("doorType", ""),
                    finish = json.optString("finish", ""),
                    hardware = json.optString("hardware", ""),
                    brand = json.optString("brand", ""),
                    thickness = json.optString("thickness", ""),
                    colour = json.optString("colour", ""),
                    laminateImageUri = json.optString("laminateImageUri", ""),
                    designImageUri = json.optString("designImageUri", ""),
                    profileSeries = json.optString("profileSeries", ""),
                    profileColour = json.optString("profileColour", ""),
                    glassType = json.optString("glassType", ""),
                    glassThickness = json.optString("glassThickness", ""),
                    acpColour = json.optString("acpColour", ""),
                    grade = json.optString("grade", ""),
                    cncDesign = json.optString("cncDesign", "")
                )
                // Return original description but if it was entirely JSON we just show it empty or a default string.
                // However, they said "Description field displays JSON instead of user description". 
                // That implies we should hide the JSON.
                return Pair("", specs)
            } catch (e: Exception) {
                // ignore
            }
        }
        return Pair(description, ItemSpecs())
    }
    val parts = description.split("|||")
    val userDesc = parts[0].trim()
    val specsJson = parts[1].trim()
    return try {
        val json = org.json.JSONObject(specsJson)
        val specs = ItemSpecs(
            width = json.optString("width", ""),
            height = json.optString("height", ""),
            depth = json.optString("depth", ""),
            doorType = json.optString("doorType", ""),
            finish = json.optString("finish", ""),
            hardware = json.optString("hardware", ""),
            brand = json.optString("brand", ""),
            thickness = json.optString("thickness", ""),
            colour = json.optString("colour", ""),
            laminateImageUri = json.optString("laminateImageUri", ""),
            designImageUri = json.optString("designImageUri", ""),
            profileSeries = json.optString("profileSeries", ""),
            profileColour = json.optString("profileColour", ""),
            glassType = json.optString("glassType", ""),
            glassThickness = json.optString("glassThickness", ""),
            acpColour = json.optString("acpColour", ""),
            grade = json.optString("grade", ""),
                    cncDesign = json.optString("cncDesign", "")
        )
        Pair(userDesc, specs)
    } catch (e: Exception) {
        Pair(userDesc, ItemSpecs())
    }
}

fun serializeItemSpecs(userDesc: String, specs: ItemSpecs): String {
    val json = org.json.JSONObject().apply {
        put("width", specs.width)
        put("height", specs.height)
        put("depth", specs.depth)
        put("doorType", specs.doorType)
        put("finish", specs.finish)
        put("hardware", specs.hardware)
        put("brand", specs.brand)
        put("thickness", specs.thickness)
        put("colour", specs.colour)
        put("laminateImageUri", specs.laminateImageUri)
        put("designImageUri", specs.designImageUri)
        put("profileSeries", specs.profileSeries)
        put("profileColour", specs.profileColour)
        put("glassType", specs.glassType)
        put("glassThickness", specs.glassThickness)
        put("acpColour", specs.acpColour)
        put("grade", specs.grade)
        put("cncDesign", specs.cncDesign)
    }
    return "$userDesc ||| $json"
}

fun parseFieldFromSummary(summary: String, fieldName: String): String {
    val parts = summary.split(",")
    for (part in parts) {
        if (part.contains(fieldName, ignoreCase = true)) {
            val keyVal = part.split(":")
            if (keyVal.size > 1) {
                return keyVal[1].trim()
            }
        }
    }
    return ""
}

fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        // Strip out any path traversal patterns (like "../") by resolving the file name part
        val cleanFileName = File(fileName).name
        val file = File(context.filesDir, cleanFileName)
        FileOutputStream(file).use { outputStream ->
            inputStream.use { input ->
                input.copyTo(outputStream)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

// --- 1. WIZARD STEP INDICATOR ---
@Composable
fun WizardStepIndicator(activeStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (step in 1..3) {
            val isActive = step == activeStep
            val isCompleted = step < activeStep
            val containerColor = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else if (isCompleted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            val contentColor = if (isActive) {
                MaterialTheme.colorScheme.onPrimary
            } else if (isCompleted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Completed",
                        modifier = Modifier.size(16.dp),
                        tint = contentColor
                    )
                } else {
                    Text(
                        text = step.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
            if (step < 3) {
                val lineColor = if (step < activeStep) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
                HorizontalDivider(
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    color = lineColor,
                    thickness = 2.dp
                )
            }
        }
    }
}

// --- 2. COMPACT LIVE SUMMARY BAR ---
@Composable
fun CompactLiveSummary(
    subtotal: Double,
    discount: Double,
    gstAmount: Double,
    grandTotal: Double
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Live Estimation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(grandTotal)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Subtotal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                    Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(subtotal)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                if (discount > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Discount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                        Text("-₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(discount)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (gstAmount > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("GST", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                        Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(gstAmount)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- MAIN SCREEN ---
@Composable
fun NewQuotationScreen(
    quotationViewModel: com.example.ui.quotation.QuotationViewModel,
    customerViewModel: com.example.ui.customer.CustomerViewModel,
    onSuccessReturn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Core Wizard State
    var activeStep by remember { mutableStateOf(1) }
    
    // State collection from ViewModel
    val currentCustomer by quotationViewModel.newQuoteCustomer.collectAsState()
    val siteName by quotationViewModel.newQuoteSiteName.collectAsState()
    val siteAddress by quotationViewModel.newQuoteSiteAddress.collectAsState()
    val currentProjectType by quotationViewModel.newQuoteProjectType.collectAsState()
    val currentCategory by quotationViewModel.newQuoteCategory.collectAsState()
    val currentMaterial by quotationViewModel.newQuoteMaterial.collectAsState()
    val currentFinish by quotationViewModel.newQuoteFinish.collectAsState()
    val currentTemplate by quotationViewModel.newQuoteTemplate.collectAsState()
    val quoteItems by quotationViewModel.newQuoteItems.collectAsState()
    val discount by quotationViewModel.newQuoteDiscount.collectAsState()
    val gstRate by quotationViewModel.newQuoteGstRate.collectAsState()
    val transport by quotationViewModel.newQuoteTransport.collectAsState()
    val installation by quotationViewModel.newQuoteInstallation.collectAsState()
    val extraCharges by quotationViewModel.newQuoteExtraCharges.collectAsState()
    val advance by quotationViewModel.newQuoteAdvance.collectAsState()
    val termsAndConditions by quotationViewModel.newQuoteTerms.collectAsState()
    val warranty by quotationViewModel.newQuoteWarranty.collectAsState()
    val quoteNumber by quotationViewModel.newQuoteNumber.collectAsState()
    
    val subtotal by quotationViewModel.newQuoteSubtotal.collectAsState()
    val gstAmount by quotationViewModel.newQuoteGstAmount.collectAsState()
    val grandTotal by quotationViewModel.newQuoteGrandTotal.collectAsState()

    // Master Dropdown items
    val masterData by quotationViewModel.allMasterData.collectAsState()
    val projectTypes = masterData.filter { it.masterType == "PROJECT_TYPE" }.map { it.name }
    val categories = masterData.filter { it.masterType == "CATEGORY" || it.masterType == "PROJECT_CATEGORY" }.map { it.name }.distinct()
    val materials = masterData.filter { it.masterType == "MATERIAL" || it.masterType == "MATERIAL_TYPE" }.map { it.name }.distinct()
    val finishes = masterData.filter { it.masterType == "FINISH_TYPE" }.map { it.name }
    val templates by quotationViewModel.allTemplates.collectAsState()

    // Dialog state for completed save
    var savedQuotationId by remember { mutableStateOf<Int?>(null) }
    var isQuickAddCustomerOpen by remember { mutableStateOf(false) }

    // Prevent accidental exit using system back button
    var showDiscardDialog by remember { mutableStateOf(false) }

    val performSaveQuotation = {
        val specSummary = buildString {
            quoteItems.forEachIndexed { idx, item ->
                val (userDesc, specs) = parseItemSpecs(item.description)
                appendLine("Item ${idx + 1} [${item.itemName}]:")
                appendLine("  Material: ${item.material}")
                when (item.material) {
                    "Plywood" -> {
                        if (specs.thickness.isNotBlank()) appendLine("  Thickness: ${specs.thickness}")
                        if (specs.grade.isNotBlank()) appendLine("  Grade: ${specs.grade}")
                    }
                    "Aluminium" -> {
                        if (specs.profileSeries.isNotBlank()) appendLine("  Profile Series: ${specs.profileSeries}")
                        if (specs.profileColour.isNotBlank()) appendLine("  Profile Colour: ${specs.profileColour}")
                        if (specs.glassType.isNotBlank()) appendLine("  Glass Type: ${specs.glassType}")
                    }
                    "Glass" -> {
                        if (specs.glassType.isNotBlank()) appendLine("  Glass Type: ${specs.glassType}")
                        if (specs.glassThickness.isNotBlank()) appendLine("  Glass Thickness: ${specs.glassThickness}")
                    }
                    "ACP" -> {
                        if (specs.acpColour.isNotBlank()) appendLine("  ACP Colour: ${specs.acpColour}")
                    }
                    "WPC" -> {
                        if (specs.thickness.isNotBlank()) appendLine("  Thickness: ${specs.thickness}")
                    }
                }
                if (specs.width.isNotBlank() || specs.height.isNotBlank() || specs.depth.isNotBlank()) {
                    val dimStr = buildString {
                        if (specs.width.isNotBlank()) append("W:${specs.width}ft")
                        if (specs.height.isNotBlank()) {
                            if (length > 0) append(" x ")
                            append("H:${specs.height}ft")
                        }
                        if (specs.depth.isNotBlank()) {
                            if (length > 0) append(" x ")
                            append("D:${specs.depth}ft")
                        }
                    }
                    appendLine("  Dimensions: $dimStr")
                }
                appendLine("  Quantity: ${item.quantity} ${item.unit}")
                appendLine()
            }
        }
        quotationViewModel.selectFinish(specSummary)

        // Copy ALL reference design images
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


    BackHandler(enabled = true) {
        if (activeStep > 1) {
            activeStep--
        } else {
            if (currentCustomer != null || quoteItems.isNotEmpty()) {
                showDiscardDialog = true
            } else {
                quotationViewModel.startNewQuotation()
                onSuccessReturn()
            }
        }
    }
    if (showDiscardDialog) {
        com.example.ui.components.PremiumDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = "Discard Quotation?",
            actions = {
                com.example.ui.components.PremiumTextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continue Editing")
                }
                Spacer(modifier = Modifier.width(8.dp))
                com.example.ui.components.PremiumPrimaryButton(
                    onClick = {
                        showDiscardDialog = false
                        quotationViewModel.startNewQuotation()
                        onSuccessReturn()
                    }
                ) {
                    Text("Discard")
                }
            }
        ) {
            Text("Are you sure you want to exit the quotation wizard? All progress on this quotation will be discarded.")
        }
    }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header with Back button if step > 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                IconButton(onClick = {
                    if (activeStep > 1) {
                        activeStep--
                    } else {
                        if (currentCustomer != null || quoteItems.isNotEmpty()) {
                            showDiscardDialog = true
                        } else {
                            onSuccessReturn()
                        }
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = when (activeStep) {
                        1 -> "Project Details"
                        2 -> "Items & Specifications"
                        else -> "Billing & Review"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Step $activeStep of 3",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

            // Step Progress Bar
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (step in 1..3) {
                    val isActive = step == activeStep
                    val isCompleted = step < activeStep
                    val containerColor = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else if (isCompleted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
                    val contentColor = if (isActive) {
                MaterialTheme.colorScheme.onPrimary
            } else if (isCompleted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Completed",
                        modifier = Modifier.size(16.dp),
                        tint = contentColor
                    )
                } else {
                    Text(
                        text = step.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
                } // CLOSE BOX HERE
                
                if (step < 2) {
                    val lineColor = if (step < activeStep) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    HorizontalDivider(
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        color = lineColor,
                        thickness = 2.dp
                    )
                }
            }
        }

        // Central Switch Content
        Box(modifier = Modifier.weight(1f)) {
            when (activeStep) {
                    1 -> WizardStepDetails(
                        customerViewModel = customerViewModel,
                        quotationViewModel = quotationViewModel,
                        onOpenQuickAdd = { isQuickAddCustomerOpen = true }
                    )
                    2 -> WizardStepItems(
                        quotationViewModel = quotationViewModel,
                        quoteItems = quoteItems,
                        currentMaterial = quotationViewModel.newQuoteMaterial.collectAsState().value,
                        currentFinish = quotationViewModel.newQuoteFinish.collectAsState().value,
                        currentProjectType = quotationViewModel.newQuoteProjectType.collectAsState().value
                    )
                    
                    else -> WizardStepReview(
                        quotationViewModel = quotationViewModel,
                        quoteNumber = quoteNumber,
                        customerName = currentCustomer?.customerName ?: "Unknown",
                        customerPhone = currentCustomer?.mobileNumber ?: "",
                        itemsCount = quoteItems.size,
                        subtotal = subtotal,
                        discount = discount,
                        gstAmount = gstAmount,
                        grandTotal = grandTotal,
                        onSave = performSaveQuotation
                    )
            }
        }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeStep > 1) {
                    OutlinedButton(
                        onClick = { activeStep-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back")

                    }
                }
                val nextButtonWeight = if (activeStep > 1) 1f else 2f
                val step1Error = when {
                    currentCustomer == null -> "Select a customer to continue"
                    siteName.isBlank() -> "Enter Site Name to continue"
                    siteAddress.isBlank() -> "Enter Site Address to continue"
                    else -> null
                }
                
                val step2Error = when {
                    quoteItems.isEmpty() -> "Add at least one item to continue"
                    else -> null
                }
                
                val step3Error = when {
                    discount > subtotal -> "Discount cannot exceed subtotal"
                    gstRate < 0 -> "GST rate cannot be negative"
                    transport < 0 -> "Transport cannot be negative"
                    installation < 0 -> "Installation cannot be negative"
                    extraCharges < 0 -> "Extra charges cannot be negative"
                    advance < 0 -> "Advance cannot be negative"
                    else -> null
                }
                
                val validationError = when (activeStep) {
                    1 -> step1Error
                    2 -> step2Error
                    3 -> step3Error
                    else -> null
                }
                
                val isNextEnabled = validationError == null
                Column(modifier = Modifier.weight(nextButtonWeight), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (validationError != null) {
                        Text(
                            text = validationError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    com.example.ui.components.PremiumPrimaryButton(
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
                            .height(48.dp)
                    ) {
                        Text(text = if (activeStep == 1) "Next to Items" else if (activeStep == 2) "Next to Review" else "Save Quotation")
                        if (activeStep < 3) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    // Quick Add Customer Inline Dialog
    if (isQuickAddCustomerOpen) {
        QuickAddCustomerDialog(
            onDismiss = { isQuickAddCustomerOpen = false },
            onSave = { newCustomer ->
                customerViewModel.saveCustomer(newCustomer) { savedId ->
                    quotationViewModel.selectCustomer(newCustomer.copy(customerId = savedId))
                    isQuickAddCustomerOpen = false
                }
            }
        )
    }

    // Success dialog shown when Saved
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
                        com.example.ui.components.PremiumPrimaryButton(
                            onClick = {
                                scope.launch {
                                    val pdfFile = com.example.utils.ShareManager.generateQuotationPdf(context, quotationViewModel.repository, id)
                                    com.example.utils.ShareManager.openOrViewPdf(context, pdfFile)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
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
                                .height(44.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share PDF")
                        }
                    }
                }
            },
            confirmButton = {
                com.example.ui.components.PremiumPrimaryButton(
                    onClick = {
                        savedQuotationId = null
                        quotationViewModel.startNewQuotation()
                        onSuccessReturn()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        )
    }
}

// --- STEP 1: PROJECT DETAILS ---
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WizardStepDetails(
    customerViewModel: com.example.ui.customer.CustomerViewModel,
    quotationViewModel: com.example.ui.quotation.QuotationViewModel,
    onOpenQuickAdd: () -> Unit
) {
    val customers by customerViewModel.customers.collectAsState()
    val searchQuery by customerViewModel.searchQuery.collectAsState()
    val selectedCustomer by quotationViewModel.newQuoteCustomer.collectAsState()

    val siteName by quotationViewModel.newQuoteSiteName.collectAsState()
    val siteAddress by quotationViewModel.newQuoteSiteAddress.collectAsState()
    val projectName by quotationViewModel.newQuoteProjectName.collectAsState()
    val dateMillis by quotationViewModel.newQuoteDate.collectAsState()
    val validityDays by quotationViewModel.newQuoteValidityDays.collectAsState()
    
    val currentProjectType by quotationViewModel.newQuoteProjectType.collectAsState()
    val currentCategory by quotationViewModel.newQuoteCategory.collectAsState()
    val currentMaterial by quotationViewModel.newQuoteMaterial.collectAsState()
    val currentFinish by quotationViewModel.newQuoteFinish.collectAsState()
    
    val allMasterData by quotationViewModel.allMasterData.collectAsState()
    
    val projectTypes = allMasterData.filter { it.masterType == "PROJECT_TYPE" }.map { it.name }.ifEmpty { listOf("Modular Kitchen", "Wardrobe", "Living Room TV Unit", "Full Home Interior") }
    val categories = allMasterData.filter { it.masterType == "CATEGORY" || it.masterType == "PROJECT_CATEGORY" }.map { it.name }.distinct().ifEmpty { listOf("Premium", "Standard", "Economy") }
    val materials = allMasterData.filter { it.masterType == "MATERIAL" || it.masterType == "MATERIAL_TYPE" }.map { it.name }.distinct().ifEmpty { listOf("BWP Plywood", "MDF (Exterior Grade)", "HDF", "Particle Board", "Aluminium Section Framework") }
    val finishes = allMasterData.filter { it.masterType == "FINISH_TYPE" }.map { it.name }.ifEmpty { listOf("Laminate", "Acrylic", "PU Paint", "Veneer", "Glass") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- CUSTOMER SELECTION ---
        if (selectedCustomer == null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Customer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    com.example.ui.components.PremiumOutlinedTextField(
                        value = searchQuery,
                        onValueChange = { customerViewModel.searchQuery.value = it },
                        label = "Search Customer (Name, Phone...)",
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { customerViewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    val displayList = customers.take(4)
                    if (displayList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No customers found. Click 'Add Customer Inline' below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayList.forEach { cust ->
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { quotationViewModel.selectCustomer(cust) },
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = cust.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(text = "Phone: ${cust.mobileNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "Select", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onOpenQuickAdd,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Customer (Inline)")
                    }
                }
            }
        } else {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = selectedCustomer!!.customerName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Phone: ${selectedCustomer!!.mobileNumber}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(
                        onClick = { quotationViewModel.clearCustomer() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Change", fontSize = 11.sp)
                    }
                }
            }

            // --- SITE & PROJECT DETAILS ---
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Project & Site Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    com.example.ui.components.PremiumOutlinedTextField(
                        value = projectName,
                        onValueChange = { quotationViewModel.updateProjectName(it) },
                        label = "Project Name",
                        placeholder = "Eg: Modular Kitchen",
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(dateMillis))
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = dateStr,
                        onValueChange = { },
                        label = "Quotation Date",
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) }
                    )

                    com.example.ui.components.PremiumOutlinedTextField(
                        value = validityDays.toString(),
                        onValueChange = { quotationViewModel.updateValidityDays(it.toIntOrNull() ?: 30) },
                        label = "Validity (Days)",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    com.example.ui.components.PremiumOutlinedTextField(
                        value = siteName,
                        onValueChange = { quotationViewModel.updateSiteDetails(it, siteAddress) },
                        label = "Site Name *",
                        placeholder = "Eg: Green Villa",
                        modifier = Modifier.fillMaxWidth()
                    )

                    com.example.ui.components.PremiumOutlinedTextField(
                        value = siteAddress,
                        onValueChange = { quotationViewModel.updateSiteDetails(siteName, it) },
                        label = "Site Address *",
                        placeholder = "Eg: 123, ABC Street, Near Landmark, City",
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }


            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- STEP 3: QUOTATION ITEMS LIST & DIALOGS ---
@Composable
fun WizardStepItems(
    quotationViewModel: com.example.ui.quotation.QuotationViewModel,
    quoteItems: List<QuotationItem>,
    currentMaterial: String,
    currentFinish: String,
    currentProjectType: String
) {
    var showItemConfigDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showSuggestionsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toolbar actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            com.example.ui.components.PremiumPrimaryButton(
                onClick = {
                    editingItemIndex = null
                    showItemConfigDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Item", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { showSuggestionsDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Suggestions", fontSize = 13.sp)
            }
        }
        if (quoteItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LibraryAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Your quotation is empty.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Click 'Add Item' or load suggestions to begin.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quoteItems.size, key = { quoteItems[it].id }) { index ->
                    val item = quoteItems[index]
                    QuotationItemCard(
                        item = item,
                        index = index,
                        modifier = Modifier.animateItem(),
                        onEdit = {
                            editingItemIndex = index
                            showItemConfigDialog = true
                        },
                        onDuplicate = { quotationViewModel.duplicateQuoteItem(index) },
                        onMoveUp = if (index > 0) { { quotationViewModel.moveQuoteItemUp(index) } } else null,
                        onMoveDown = if (index < quoteItems.size - 1) { { quotationViewModel.moveQuoteItemDown(index) } } else null,
                        onDelete = { quotationViewModel.removeQuoteItem(index) }
                    )
                }
            }
        }
    }

    if (showItemConfigDialog) {
        val masterDataVal = quotationViewModel.allMasterData.collectAsState().value
        val finishes = masterDataVal
            .filter { it.masterType == "FINISH_TYPE" }
            .map { it.name }
        ItemConfigDialog(
            itemIndex = editingItemIndex,
            currentItems = quoteItems,
            onDismiss = { showItemConfigDialog = false },
            onSave = { updatedOrNewItem ->
                if (editingItemIndex == null) {
                    quotationViewModel.addQuoteItem(updatedOrNewItem)
                } else {
                    quotationViewModel.updateQuoteItem(editingItemIndex!!, updatedOrNewItem)
                }
                showItemConfigDialog = false
            },
            calculatePreview = { w, h, d, q, u, r -> quotationViewModel.previewItemCalculation(w, h, d, q, u, r) },
            finishes = finishes,
            allMasterData = masterDataVal,
            projectType = currentProjectType,
            category = currentFinish
        )
    }
}

@Composable
fun SpecTagChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SpecDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.PremiumDropdown(
        value = value,
        onValueChange = onValueChange,
        label = label,
        options = options,
        modifier = modifier
    )
}

fun isDimensionUnit(unit: String): Boolean {
    val u = unit.trim().lowercase(Locale.US)
    return u.contains("sq") || u.contains("sft") || u.contains("rft") || u.contains("r.ft") || u.contains("run") || u.contains("feet") || u.contains("meter") || u.contains("mtr") || u.contains("cu") || u.contains("cft") || u.contains("cum")
}

fun isAreaUnit(unit: String): Boolean {
    val u = unit.trim().lowercase(Locale.US)
    return u.contains("sq") || u.contains("sft")
}

// Smart Product Template System
data class DimensionPreset(
    val label: String,
    val width: String,
    val height: String,
    val depth: String
)

data class InteriorItemTemplate(
    val name: String,
    val category: String,
    val material: String,
    val grade: String = "",
    val finish: String = "",
    val thickness: String = "",
    val unit: String = "Sq.Ft",
    val suggestedHardware: List<String> = emptyList(),
    val defaultNotes: String = "",
    val dimensionPresets: List<DimensionPreset> = emptyList()
)

fun getBuiltInTemplates(): List<InteriorItemTemplate> {
    return listOf(
        InteriorItemTemplate(
            name = "Wardrobe",
            category = "Wardrobes",
            material = "Plywood",
            grade = "BWP",
            finish = "High Gloss Laminate",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Soft Close Hinges", "Telescopic Channels", "Handles", "Magnetic Catchers", "Adjustable Legs"),
            defaultNotes = "Premium grade BWP wardrobe with Laminate finish, soft close hinges and premium handles.",
            dimensionPresets = listOf(
                DimensionPreset("6’ × 7’", "72", "84", "24"),
                DimensionPreset("7’ × 7’", "84", "84", "24"),
                DimensionPreset("8’ × 7’", "96", "84", "24")
            )
        ),
        InteriorItemTemplate(
            name = "Modular Kitchen",
            category = "Modular Kitchen",
            material = "Plywood",
            grade = "BWP",
            finish = "Acrylic finish",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Tandem Box", "Lift-up Stay", "Corner Basket", "Bottle Pullout", "Cutlery Tray"),
            defaultNotes = "Modern Modular Kitchen with BWR/BWP plywood carcasses, premium acrylic shutters, and tandem drawers.",
            dimensionPresets = listOf(
                DimensionPreset("Straight (8’ L)", "96", "34", "24"),
                DimensionPreset("L Shape (10’ × 6’)", "192", "34", "24"),
                DimensionPreset("U Shape (8’ × 8’ × 6’)", "264", "34", "24"),
                DimensionPreset("Island (6’ × 3’)", "72", "34", "36")
            )
        ),
        InteriorItemTemplate(
            name = "TV Unit",
            category = "Living Room",
            material = "MDF",
            grade = "Standard MDF",
            finish = "PU Paint",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Cable Manager", "LED Profile", "Wall Hanging Brackets"),
            defaultNotes = "Sleek wall-mounted TV Unit with PU Painted finish and provision for concealed LED profile lighting.",
            dimensionPresets = listOf(
                DimensionPreset("Compact (5’ × 4’)", "60", "48", "16"),
                DimensionPreset("Standard (6’ × 5’)", "72", "60", "16"),
                DimensionPreset("Grand (8’ × 6’)", "96", "72", "18")
            )
        ),
        InteriorItemTemplate(
            name = "Crockery Unit",
            category = "Dining Room",
            material = "Plywood",
            grade = "BWR",
            finish = "Laminate",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Soft Close Hinges", "Magnetic Catchers", "Glass Shelf Brackets", "LED Profile"),
            defaultNotes = "Elegant Crockery Unit with glass shutters, LED profiles, and premium drawers.",
            dimensionPresets = listOf(
                DimensionPreset("Slim (3’ × 7’)", "36", "84", "16"),
                DimensionPreset("Standard (4’ × 7’)", "48", "84", "16"),
                DimensionPreset("Wide (6’ × 7’)", "72", "84", "18")
            )
        ),
        InteriorItemTemplate(
            name = "Loft",
            category = "Bedroom",
            material = "Plywood",
            grade = "MR",
            finish = "Laminate",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Gas Springs", "Hinges", "Handles"),
            defaultNotes = "Bedroom loft extension over wardrobe, using MR grade plywood and laminate shutters.",
            dimensionPresets = listOf(
                DimensionPreset("6’ L × 2’ H", "72", "24", "24"),
                DimensionPreset("8’ L × 2’ H", "96", "24", "24"),
                DimensionPreset("10’ L × 2’ H", "120", "24", "24")
            )
        ),
        InteriorItemTemplate(
            name = "Shoe Rack",
            category = "Foyer",
            material = "Plywood",
            grade = "Commercial Ply",
            finish = "Laminate",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Shoe Rack Pivot Drawer Fittings", "Handles", "Air Vents"),
            defaultNotes = "Functional Shoe Rack with multi-level inclined shelves, ventilation grilles, and cushioned seating top.",
            dimensionPresets = listOf(
                DimensionPreset("Compact (2’ × 3’)", "24", "36", "12"),
                DimensionPreset("Standard (3’ × 3’)", "36", "36", "14"),
                DimensionPreset("Large (4’ × 4’)", "48", "48", "14")
            )
        ),
        InteriorItemTemplate(
            name = "Study Table",
            category = "Kids Room",
            material = "MDF",
            grade = "HDMR",
            finish = "Laminate",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Keyboard Tray", "Telescopic Drawer Runners", "Wire Manager Grommets"),
            defaultNotes = "Ergonomic Study Table with smooth edge-banded desktop, cable wire manager, and file storage drawer.",
            dimensionPresets = listOf(
                DimensionPreset("Compact (3’ × 2.5’)", "36", "30", "24"),
                DimensionPreset("Standard (4’ × 2.5’)", "48", "30", "24"),
                DimensionPreset("Executive (5’ × 2.5’)", "60", "30", "30")
            )
        ),
        InteriorItemTemplate(
            name = "Office Table",
            category = "Office",
            material = "Particle Board",
            grade = "Prelam",
            finish = "Laminate",
            thickness = "25 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Central Lock System", "Cable Manager Spine", "Soft Closing Drawer Slides"),
            defaultNotes = "Heavy duty executive Office Table with 25mm thick prelam board, side credenza, and multi-lock drawers.",
            dimensionPresets = listOf(
                DimensionPreset("Manager (5’ × 2.5’)", "60", "30", "30"),
                DimensionPreset("Director (6’ × 3’)", "72", "30", "36"),
                DimensionPreset("Conference (8’ × 4’)", "96", "30", "48")
            )
        ),
        InteriorItemTemplate(
            name = "Reception Counter",
            category = "Commercial",
            material = "Plywood",
            grade = "BWR",
            finish = "Acrylic",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Keyboard Tray", "Glass Top Spacers", "LED Strip Profile", "Lockable Cash Drawer"),
            defaultNotes = "Gleaming Reception Desk with dual-level counter, acrylic finish frontage, accent LED lighting, and cash drawers.",
            dimensionPresets = listOf(
                DimensionPreset("Straight (5’ L)", "60", "42", "24"),
                DimensionPreset("Curved (6’ L)", "72", "42", "30"),
                DimensionPreset("L-Shape (6’ × 4’)", "72", "42", "48")
            )
        ),
        InteriorItemTemplate(
            name = "Bed Cot",
            category = "Bedroom",
            material = "Plywood",
            grade = "Commercial Ply",
            finish = "Laminate",
            thickness = "18 mm",
            unit = "Piece",
            suggestedHardware = listOf("Hydraulic Bed Lift Mechanism", "Corner Brackets", "Headboard Cushion Fittings"),
            defaultNotes = "Spacious Bed Cot with premium hydraulic storage lift-up mechanism and soft upholstered headboard.",
            dimensionPresets = listOf(
                DimensionPreset("Single (3’ × 6’)", "36", "18", "72"),
                DimensionPreset("Queen Size (5’ × 6.5’)", "60", "18", "78"),
                DimensionPreset("King Size (6’ × 6.5’)", "72", "18", "78")
            )
        ),
        InteriorItemTemplate(
            name = "Vanity Unit",
            category = "Bathroom",
            material = "Plywood",
            grade = "Marine Ply",
            finish = "PU Paint",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Rust-proof SS Hinges", "Soft Close Runners", "Concealed L-brackets"),
            defaultNotes = "Waterproof Bathroom Vanity Unit using 100% Marine Grade Plywood, premium PU Paint finish, and stainless steel rust-proof hinges.",
            dimensionPresets = listOf(
                DimensionPreset("Compact (2’ × 1.5’ H)", "24", "18", "20"),
                DimensionPreset("Standard (3’ × 2’ H)", "36", "24", "22"),
                DimensionPreset("Double Sink (5’ × 2’ H)", "60", "24", "22")
            )
        ),
        InteriorItemTemplate(
            name = "Wall Panelling",
            category = "Living Room",
            material = "MDF",
            grade = "HDF",
            finish = "Veneer",
            thickness = "12 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Clips", "Adhesive", "LED Strip Profile"),
            defaultNotes = "Decorative Wall Panelling with rich wood veneer flutes or acoustic charcoal slats and warm lighting.",
            dimensionPresets = listOf(
                DimensionPreset("Partial Wall (4’ × 8’)", "48", "96", "2"),
                DimensionPreset("Full Accent Wall (8’ × 9’)", "96", "108", "2"),
                DimensionPreset("TV Backdrop (6’ × 7’)", "72", "84", "3")
            )
        ),
        InteriorItemTemplate(
            name = "False Ceiling",
            category = "Ceiling",
            material = "PVC Board",
            grade = "Standard",
            finish = "Matt",
            thickness = "12 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("GI Channels", "Suspended Hanger Wires", "LED COB Spotlight Clips"),
            defaultNotes = "Seamless Gypsum / PVC False Ceiling with indirect cove light borders and recessed spotlight provisioning.",
            dimensionPresets = listOf(
                DimensionPreset("Room Border (10’ × 10’)", "120", "120", "6"),
                DimensionPreset("Standard Grid (12’ × 12’)", "144", "144", "6"),
                DimensionPreset("Lobby Ceiling (15’ × 6’)", "180", "72", "6")
            )
        ),
        InteriorItemTemplate(
            name = "Aluminium Partition",
            category = "Office",
            material = "Aluminium",
            grade = "Standard",
            finish = "Anodized",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Floor Spring", "D-Handle", "Door Closer", "Profile Glazing Gaskets"),
            defaultNotes = "Sturdy commercial Aluminium Partition framed in Anodized Silver/Black, including 5mm glass infills.",
            dimensionPresets = listOf(
                DimensionPreset("Small Partition (4’ × 8’)", "48", "96", "2"),
                DimensionPreset("Office Cubicle (6’ × 8’)", "72", "96", "2"),
                DimensionPreset("Main Cabin (10’ × 8’)", "120", "96", "2.5")
            )
        ),
        InteriorItemTemplate(
            name = "Aluminium Sliding Door",
            category = "Living Room",
            material = "Aluminium",
            grade = "Slim",
            finish = "Powder Coating",
            thickness = "18 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Heavy-duty Nylon Rollers", "Touch Lock", "Interlock Rails", "Wool Pile Weatherstripping"),
            defaultNotes = "Slim-line powder coated 3-track Aluminium Sliding Door with premium double-glazed acoustic glass panels.",
            dimensionPresets = listOf(
                DimensionPreset("2-Track Small (5’ × 7’)", "60", "84", "3"),
                DimensionPreset("3-Track Standard (8’ × 7’)", "96", "84", "4"),
                DimensionPreset("Balcony Jumbo (10’ × 8’)", "120", "96", "4.5")
            )
        ),
        InteriorItemTemplate(
            name = "Glass Partition",
            category = "Bathroom",
            material = "Glass",
            grade = "Toughened",
            finish = "Frosted",
            thickness = "10 mm",
            unit = "Sq.Ft",
            suggestedHardware = listOf("Shower Hinge", "SS Glass Connectors", "Water Barrier Profile", "Towel Bar Handle"),
            defaultNotes = "Minimalist floor-to-ceiling Frameless Toughened Glass Partition with frosted stripes or sandblasted film.",
            dimensionPresets = listOf(
                DimensionPreset("Shower Partition (3’ × 7’)", "36", "84", "0.5"),
                DimensionPreset("Office Glass Wall (6’ × 8’)", "72", "96", "0.5"),
                DimensionPreset("Entrance Glazing (8’ × 8’)", "96", "96", "0.5")
            )
        )
    )
}

fun getCustomItemTemplates(masterData: List<com.example.data.MasterEntity>): List<InteriorItemTemplate> {
    return masterData.filter { it.masterType == "ITEM_TEMPLATE" }.mapNotNull { md ->
        try {
            val name = md.name
            val extraJson = md.description
            if (extraJson.startsWith("{") && extraJson.endsWith("}")) {
                val json = org.json.JSONObject(extraJson)
                val cat = json.optString("category", "General")
                val mat = json.optString("material", "Plywood")
                val grd = json.optString("grade", "")
                val fin = json.optString("finish", "")
                val thk = json.optString("thickness", "")
                val unt = json.optString("unit", "Sq.Ft")
                val hwStr = json.optString("hardware", "")
                val hwList = if (hwStr.isBlank()) emptyList() else hwStr.split(",").map { it.trim() }
                val dNotes = json.optString("notes", "")
                
                val presets = mutableListOf<DimensionPreset>()
                val presetsArr = json.optJSONArray("presets")
                if (presetsArr != null) {
                    for (i in 0 until presetsArr.length()) {
                        val pObj = presetsArr.getJSONObject(i)
                        presets.add(
                            DimensionPreset(
                                label = pObj.optString("label", "Preset"),
                                width = pObj.optString("width", ""),
                                height = pObj.optString("height", ""),
                                depth = pObj.optString("depth", "")
                            )
                        )
                    }
                }
                InteriorItemTemplate(
                    name = name,
                    category = cat,
                    material = mat,
                    grade = grd,
                    finish = fin,
                    thickness = thk,
                    unit = unt,
                    suggestedHardware = hwList,
                    defaultNotes = dNotes,
                    dimensionPresets = presets
                )
            } else {
                InteriorItemTemplate(
                    name = name,
                    category = "General",
                    material = "Plywood",
                    unit = "Sq.Ft"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// Helper functions for Material Intelligence
fun resolveMaterialType(material: String): String {
    val mLower = material.lowercase(Locale.US)
    return when {
        mLower.contains("plywood") || mLower.contains("ply") -> "plywood"
        mLower.contains("mdf") || mLower.contains("hdhmr") || mLower.contains("hdf") -> "mdf"
        mLower.contains("particle") -> "particle"
        mLower.contains("acp") -> "acp"
        mLower.contains("aluminium") || mLower.contains("aluminum") -> "aluminium"
        mLower.contains("glass") -> "glass"
        mLower.contains("wpc") -> "wpc"
        mLower.contains("pvc") -> "pvc"
        mLower.contains("blockboard") || mLower.contains("block board") -> "blockboard"
        mLower.contains("edge band") || mLower.contains("edgeband") -> "edgeband"
        mLower.contains("hinge") -> "hinges"
        mLower.contains("handle") -> "handles"
        mLower.contains("profile") -> "profile"
        else -> "other"
    }
}

data class MaterialRule(
    val material: String,
    val allowedGrades: List<String>,
    val allowedFinishes: List<String>,
    val allowedThicknesses: List<String>,
    val defaultGrade: String = "",
    val defaultFinish: String = "",
    val defaultThickness: String = "",
    val recommendedUnit: String = "Sq.Ft",
    val recommendedHardware: List<String> = emptyList(),
    val recommendedBrand: String = ""
)

fun getMaterialRules(allMasterData: List<com.example.data.MasterEntity>): List<MaterialRule> {
    val defaultRules = listOf(
        MaterialRule(
            material = "Plywood",
            allowedGrades = listOf("MR", "BWR", "BWP", "Marine Ply", "Commercial Ply"),
            allowedFinishes = listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane"),
            allowedThicknesses = listOf("6 mm", "9 mm", "12 mm", "16 mm", "18 mm", "25 mm"),
            defaultGrade = "BWP",
            defaultFinish = "Laminate",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft",
            recommendedHardware = listOf("Soft Close Hinges", "Telescopic Channels", "Handles", "Magnetic Catchers"),
            recommendedBrand = "CenturyPly"
        ),
        MaterialRule(
            material = "BWP Plywood",
            allowedGrades = listOf("MR", "BWR", "BWP", "Marine Ply", "Commercial Ply"),
            allowedFinishes = listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane"),
            allowedThicknesses = listOf("6 mm", "9 mm", "12 mm", "16 mm", "18 mm", "25 mm"),
            defaultGrade = "BWP",
            defaultFinish = "Laminate",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft",
            recommendedHardware = listOf("Soft Close Hinges", "Telescopic Channels", "Handles", "Magnetic Catchers"),
            recommendedBrand = "CenturyPly"
        ),
        MaterialRule(
            material = "MDF",
            allowedGrades = listOf("Standard MDF", "HDMR", "HDF", "Exterior MDF"),
            allowedFinishes = listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane"),
            allowedThicknesses = listOf("6 mm", "9 mm", "12 mm", "17 mm", "18 mm", "25 mm"),
            defaultGrade = "HDMR",
            defaultFinish = "Laminate",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft",
            recommendedHardware = listOf("Cable Manager", "LED Profile", "Wall Hanging Brackets")
        ),
        MaterialRule(
            material = "MDF (Exterior Grade)",
            allowedGrades = listOf("Standard MDF", "HDMR", "HDF", "Exterior MDF"),
            allowedFinishes = listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane"),
            allowedThicknesses = listOf("6 mm", "9 mm", "12 mm", "17 mm", "18 mm", "25 mm"),
            defaultGrade = "HDMR",
            defaultFinish = "Laminate",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft",
            recommendedHardware = listOf("Cable Manager", "LED Profile", "Wall Hanging Brackets")
        ),
        MaterialRule(
            material = "HDF",
            allowedGrades = listOf("Standard MDF", "HDMR", "HDF", "Exterior MDF"),
            allowedFinishes = listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane"),
            allowedThicknesses = listOf("6 mm", "9 mm", "12 mm", "17 mm", "18 mm", "25 mm"),
            defaultGrade = "HDF",
            defaultFinish = "Laminate",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft",
            recommendedHardware = listOf("Cable Manager", "LED Profile", "Wall Hanging Brackets")
        ),
        MaterialRule(
            material = "Particle Board",
            allowedGrades = listOf("Standard / Plain", "Pre-Laminated", "Moisture Resistant"),
            allowedFinishes = listOf("Laminate", "Veneer", "PU"),
            allowedThicknesses = listOf("6 mm", "8 mm", "9 mm", "12 mm", "15 mm", "17 mm", "18 mm", "25 mm"),
            defaultGrade = "Pre-Laminated",
            defaultFinish = "",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "ACP",
            allowedGrades = listOf("Interior Grade", "Exterior Grade"),
            allowedFinishes = listOf("Matt", "Gloss", "PVDF", "Metallic"),
            allowedThicknesses = listOf("3 mm", "4 mm", "6 mm"),
            defaultGrade = "Exterior Grade",
            defaultFinish = "Matt",
            defaultThickness = "4 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "Aluminium Composite Panel (ACP)",
            allowedGrades = listOf("Interior Grade", "Exterior Grade"),
            allowedFinishes = listOf("Matt", "Gloss", "PVDF", "Metallic"),
            allowedThicknesses = listOf("3 mm", "4 mm", "6 mm"),
            defaultGrade = "Exterior Grade",
            defaultFinish = "Matt",
            defaultThickness = "4 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "Aluminium",
            allowedGrades = listOf("Slim", "Standard", "Heavy", "Modular"),
            allowedFinishes = listOf("Powder Coating", "Anodized", "PVDF", "Matt", "Gloss"),
            allowedThicknesses = listOf("1 mm", "1.5 mm", "2 mm", "3 mm"),
            defaultGrade = "Standard",
            defaultFinish = "Powder Coating",
            defaultThickness = "1.5 mm",
            recommendedUnit = "R.Ft",
            recommendedHardware = listOf("Nylon Rollers", "Floor Springs", "D-Handles")
        ),
        MaterialRule(
            material = "Aluminium Section Framework",
            allowedGrades = listOf("Slim", "Standard", "Heavy", "Modular"),
            allowedFinishes = listOf("Powder Coating", "Anodized", "PVDF", "Matt", "Gloss"),
            allowedThicknesses = listOf("1 mm", "1.5 mm", "2 mm", "3 mm"),
            defaultGrade = "Standard",
            defaultFinish = "Powder Coating",
            defaultThickness = "1.5 mm",
            recommendedUnit = "R.Ft",
            recommendedHardware = listOf("Nylon Rollers", "Floor Springs", "D-Handles")
        ),
        MaterialRule(
            material = "Glass",
            allowedGrades = listOf("Clear", "Frosted", "Toughened", "Lacquered", "Fluted", "Laminated"),
            allowedFinishes = listOf("Plain", "Tinted", "Etched", "Back-Painted"),
            allowedThicknesses = listOf("5 mm", "8 mm", "10 mm", "12 mm"),
            defaultGrade = "Toughened",
            defaultFinish = "Plain",
            defaultThickness = "8 mm",
            recommendedUnit = "Sq.Ft",
            recommendedHardware = listOf("Shower Hinges", "SS Connectors", "Water Barriers")
        ),
        MaterialRule(
            material = "WPC",
            allowedGrades = listOf("Standard", "High Density"),
            allowedFinishes = listOf("Plain", "PVC Laminate", "PU Paint", "Acrylic"),
            allowedThicknesses = listOf("6 mm", "12 mm", "18 mm", "25 mm", "28 mm"),
            defaultGrade = "High Density",
            defaultFinish = "Plain",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "PVC Board",
            allowedGrades = listOf("Standard", "Premium"),
            allowedFinishes = listOf("Plain", "PVC Laminate"),
            allowedThicknesses = listOf("6 mm", "12 mm", "18 mm"),
            defaultGrade = "Premium",
            defaultFinish = "Plain",
            defaultThickness = "18 mm",
            recommendedUnit = "Sq.Ft"
        ),
        MaterialRule(
            material = "Blockboard",
            allowedGrades = listOf("MR", "BWP"),
            allowedFinishes = listOf("Laminate", "Veneer", "PU"),
            allowedThicknesses = listOf("19 mm", "25 mm"),
            defaultGrade = "BWP",
            defaultFinish = "Laminate",
            defaultThickness = "19 mm",
            recommendedUnit = "Sq.Ft"
        )
    )

    val customRules = allMasterData.filter { it.masterType == "MATERIAL_RULE" }.mapNotNull { md ->
        try {
            val matName = md.name
            val json = org.json.JSONObject(md.description)
            
            val allowedGradesArr = json.optJSONArray("allowedGrades")
            val allowedGrades = if (allowedGradesArr != null) {
                (0 until allowedGradesArr.length()).map { allowedGradesArr.getString(it) }
            } else {
                emptyList()
            }
            val allowedFinishesArr = json.optJSONArray("allowedFinishes")
            val allowedFinishes = if (allowedFinishesArr != null) {
                (0 until allowedFinishesArr.length()).map { allowedFinishesArr.getString(it) }
            } else {
                emptyList()
            }
            val allowedThicknessesArr = json.optJSONArray("allowedThicknesses")
            val allowedThicknesses = if (allowedThicknessesArr != null) {
                (0 until allowedThicknessesArr.length()).map { allowedThicknessesArr.getString(it) }
            } else {
                emptyList()
            }
            val recommendedHardwareArr = json.optJSONArray("recommendedHardware")
            val recommendedHardware = if (recommendedHardwareArr != null) {
                (0 until recommendedHardwareArr.length()).map { recommendedHardwareArr.getString(it) }
            } else {
                emptyList()
            }
            MaterialRule(
                material = matName,
                allowedGrades = allowedGrades,
                allowedFinishes = allowedFinishes,
                allowedThicknesses = allowedThicknesses,
                defaultGrade = json.optString("defaultGrade", ""),
                defaultFinish = json.optString("defaultFinish", ""),
                defaultThickness = json.optString("defaultThickness", ""),
                recommendedUnit = json.optString("recommendedUnit", "Sq.Ft"),
                recommendedHardware = recommendedHardware,
                recommendedBrand = json.optString("recommendedBrand", "")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    return customRules + defaultRules
}

fun getGradesForMaterial(material: String, allMasterData: List<com.example.data.MasterEntity> = emptyList()): List<String> {
    val rules = getMaterialRules(allMasterData)
    val matched = rules.find { it.material.equals(material, ignoreCase = true) || resolveMaterialType(it.material) == resolveMaterialType(material) }
    if (matched != null && matched.allowedGrades.isNotEmpty()) {
        return matched.allowedGrades
    }
    return when (resolveMaterialType(material)) {
        "plywood" -> listOf("MR", "BWR", "BWP", "Marine Ply", "Commercial Ply")
        "mdf" -> listOf("Standard MDF", "HDMR", "HDF", "Exterior MDF")
        "particle" -> listOf("Standard / Plain", "Pre-Laminated", "Moisture Resistant")
        "acp" -> listOf("Interior Grade", "Exterior Grade")
        "aluminium" -> listOf("Slim", "Standard", "Heavy", "Modular")
        "glass" -> listOf("Clear", "Frosted", "Toughened", "Lacquered", "Fluted", "Laminated")
        "wpc" -> listOf("Standard", "High Density")
        "pvc" -> listOf("Standard", "Premium")
        "blockboard" -> listOf("MR", "BWP")
        else -> emptyList()
    }
}

fun getFinishesForMaterial(material: String, grade: String, masterFinishes: List<String>, allMasterData: List<com.example.data.MasterEntity> = emptyList()): List<String> {
    val mLower = resolveMaterialType(material)
    val gLower = grade.lowercase(java.util.Locale.US)
    if (mLower == "particle" && (gLower.contains("pre-laminated") || gLower.contains("prelam"))) {
        return emptyList()
    }

    val rules = getMaterialRules(allMasterData)
    val matched = rules.find { it.material.equals(material, ignoreCase = true) || resolveMaterialType(it.material) == resolveMaterialType(material) }
    if (matched != null && matched.allowedFinishes.isNotEmpty()) {
        return matched.allowedFinishes
    }
    return when (resolveMaterialType(material)) {
        "plywood" -> listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane")
        "aluminium" -> listOf("Powder Coating", "Anodized", "PVDF", "Matt", "Gloss")
        "glass" -> listOf("Plain", "Tinted", "Etched", "Back-Painted")
        "mdf" -> listOf("Laminate", "Veneer", "PU", "Acrylic", "Membrane")
        "particle" -> listOf("Laminate", "Veneer", "PU")
        "wpc" -> listOf("Plain", "PVC Laminate", "PU Paint", "Acrylic")
        "pvc" -> listOf("Plain", "PVC Laminate")
        "blockboard" -> listOf("Laminate", "Veneer", "PU")
        "other" -> masterFinishes.ifEmpty { listOf("Laminate", "PU", "Acrylic") }
        else -> emptyList()
    }
}

fun getThicknessOptionsForMaterial(material: String, allMasterData: List<com.example.data.MasterEntity> = emptyList()): List<String> {
    val rules = getMaterialRules(allMasterData)
    val matched = rules.find { it.material.equals(material, ignoreCase = true) || resolveMaterialType(it.material) == resolveMaterialType(material) }
    if (matched != null && matched.allowedThicknesses.isNotEmpty()) {
        return matched.allowedThicknesses
    }
    return when (resolveMaterialType(material)) {
        "plywood" -> listOf("6 mm", "9 mm", "12 mm", "16 mm", "18 mm", "25 mm")
        "acp" -> listOf("3 mm", "4 mm", "6 mm")
        "glass" -> listOf("5 mm", "8 mm", "10 mm", "12 mm")
        "mdf" -> listOf("6 mm", "9 mm", "12 mm", "17 mm", "18 mm", "25 mm")
        "particle" -> listOf("6 mm", "8 mm", "9 mm", "12 mm", "15 mm", "17 mm", "18 mm", "25 mm")
        "wpc" -> listOf("6 mm", "12 mm", "18 mm", "25 mm", "28 mm")
        "pvc" -> listOf("6 mm", "12 mm", "18 mm")
        "blockboard" -> listOf("19 mm", "25 mm")
        else -> emptyList()
    }
}

fun getRecommendedUnitForMaterial(material: String, allMasterData: List<com.example.data.MasterEntity> = emptyList()): String {
    val rules = getMaterialRules(allMasterData)
    val matched = rules.find { it.material.equals(material, ignoreCase = true) || resolveMaterialType(it.material) == resolveMaterialType(material) }
    if (matched != null) {
        return matched.recommendedUnit
    }
    return when (resolveMaterialType(material)) {
        "plywood", "mdf", "particle", "acp", "glass" -> "Sq.Ft"
        "edgeband", "profile", "aluminium" -> "R.Ft"
        "hinges", "handles", "other" -> "Piece"
        else -> "Piece"
    }
}

fun getRecommendedHardware(itemName: String, projectType: String = "", category: String = ""): List<String> {
    val combined = "$itemName $projectType $category".lowercase(Locale.US)
    return when {
        combined.contains("wardrobe") -> listOf("Soft Close Hinges", "Telescopic Channels", "Handles", "Magnetic Catchers", "Adjustable Legs")
        combined.contains("kitchen") -> listOf("Tandem Box", "Lift-up Stay", "Corner Basket", "Bottle Pullout", "Cutlery Tray")
        combined.contains("tv unit") || combined.contains("tv-unit") || combined.contains("television") -> listOf("Cable Manager", "LED Profile", "Wall Hanging Brackets")
        else -> emptyList()
    }
}

// --- ITEM CONFIGURATION FORM DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemConfigDialog(
    itemIndex: Int?,
    currentItems: List<QuotationItem>,
    onDismiss: () -> Unit,
    onSave: (QuotationItem) -> Unit,
    calculatePreview: (String, String, String, Double, String, Double) -> Pair<Double, Double> = { _, _, _, _, _, _ -> Pair(0.0, 0.0) },
    finishes: List<String> = emptyList(),
    allMasterData: List<com.example.data.MasterEntity> = emptyList(),
    projectType: String = "",
    category: String = ""
) {
    val context = LocalContext.current

    // Properties
    var itemName by remember { mutableStateOf("") }
    var userDescription by remember { mutableStateOf("") }
    var selectedTemplateName by remember { mutableStateOf("") }

    // Material selection
    var material by remember { mutableStateOf("Plywood") }
    var finish by remember { mutableStateOf("") }

    val dbMaterials = allMasterData.filter { it.masterType == "MATERIAL" || it.masterType == "MATERIAL_TYPE" }.map { it.name }.distinct()
    val defaultMaterials = listOf("Plywood", "Particle Board", "MDF", "HDHMR", "WPC", "Aluminium", "ACP", "Glass", "PVC Board", "Hardware")
    val materialsList = (defaultMaterials + dbMaterials).distinct()

    // Material specific properties
    var profileSeries by remember { mutableStateOf("") }
    var profileColour by remember { mutableStateOf("") }
    var glassType by remember { mutableStateOf("") }
    var glassThickness by remember { mutableStateOf("") }
    var acpColour by remember { mutableStateOf("") }
    var thickness by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var hardware by remember { mutableStateOf("") }
    var cncDesign by remember { mutableStateOf("") }

    // Common properties
    var widthStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var depthStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Sq.Ft") }
    var quantityStr by remember { mutableStateOf("1.0") }
    var rateStr by remember { mutableStateOf("") }

    var designPath by remember { mutableStateOf("") }

    val designLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val copied = copyUriToInternalStorage(context, it, "temp_des_${System.currentTimeMillis()}.jpg")
            if (copied != null) {
                if (designPath.isNotEmpty() && designPath != copied) {
                    val oldFile = java.io.File(context.filesDir, java.io.File(designPath).name)
                    if (oldFile.exists() && oldFile.name.startsWith("temp_des_")) {
                        oldFile.delete()
                    }
                }
                designPath = copied

            }
        }
    }
    LaunchedEffect(itemIndex) {
        if (itemIndex != null && itemIndex in currentItems.indices) {
            val existing = currentItems[itemIndex]
            itemName = existing.itemName
            unit = existing.unit
            rateStr = existing.rate.toString()

            val (desc, specs) = parseItemSpecs(existing.description)
            userDescription = desc
            material = existing.material
            finish = specs.finish.ifBlank { existing.finish }

            profileSeries = specs.profileSeries
            profileColour = specs.profileColour
            glassType = specs.glassType
            glassThickness = specs.glassThickness
            acpColour = specs.acpColour
            thickness = specs.thickness
            grade = specs.grade
            cncDesign = specs.cncDesign
            brand = specs.brand
            hardware = specs.hardware
            widthStr = specs.width
            heightStr = specs.height
            depthStr = specs.depth
            designPath = specs.designImageUri
            val displayQty = if (existing.rawQuantity > 0.0) existing.rawQuantity else existing.quantity
            quantityStr = if (displayQty % 1.0 == 0.0) displayQty.toInt().toString() else displayQty.toString()

            val builtIn = getBuiltInTemplates()
            val custom = getCustomItemTemplates(allMasterData)
            val allItemTemplates = builtIn + custom
            if (allItemTemplates.any { it.name.equals(existing.itemName, ignoreCase = true) }) {
                selectedTemplateName = allItemTemplates.first { it.name.equals(existing.itemName, ignoreCase = true) }.name
                selectedTemplateName = ""

    // Calculations
            }
        }
    }
    val wFeet = com.example.engine.QuotationCalculationEngine.parseDimensionToFeet(widthStr)
    val hFeet = com.example.engine.QuotationCalculationEngine.parseDimensionToFeet(heightStr)
    val dFeet = com.example.engine.QuotationCalculationEngine.parseDimensionToFeet(depthStr)
    
    val qtyVal = quantityStr.toDoubleOrNull() ?: 0.0
    val rateVal = rateStr.toDoubleOrNull() ?: 0.0
    val uLower = unit.trim().lowercase(Locale.US)
    
    val isVolumeBased = uLower.contains("cu") || uLower.contains("cubic") || uLower.contains("cft") || uLower.contains("cum")
    val isAreaBased = !isVolumeBased && (uLower.contains("sq") || uLower.contains("sft") || uLower.contains("square"))
    val isLinearBased = !isVolumeBased && !isAreaBased && (uLower.contains("rft") || uLower.contains("run") || uLower.contains("meter") || uLower.contains("mtr") || uLower == "r.m" || uLower == "rm")
    val isCountBased = uLower == "nos" || uLower == "pcs" || uLower.contains("nos") || uLower.contains("pcs") || uLower.contains("set")
    val isLumpSum = uLower == "lumpsum" || uLower.contains("lump") || uLower == "l.s" || uLower == "ls"

    val previewValues = calculatePreview(widthStr, heightStr, depthStr, qtyVal, unit, rateVal)
    val totalBillableQty = previewValues.first
    val calculatedAmount = previewValues.second

    com.example.ui.components.PremiumDialog(
        onDismissRequest = onDismiss,
        title = if (itemIndex == null) "Configure Item" else "Edit Item",
        modifier = Modifier.fillMaxWidth(),
        actions = {
            com.example.ui.components.PremiumTextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
            Spacer(modifier = Modifier.width(8.dp))
            com.example.ui.components.PremiumPrimaryButton(
                onClick = {
                    if (itemName.isBlank()) {
                        Toast.makeText(context, "Item Name is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (itemName.isBlank()) {
                        Toast.makeText(context, "Item Name is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (qtyVal < 0 || rateVal < 0) {
                        Toast.makeText(context, "Values cannot be negative", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (qtyVal == 0.0) {
                        Toast.makeText(context, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (material.isBlank()) {
                        Toast.makeText(context, "Material is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    val currentGradeOpts = getGradesForMaterial(material, allMasterData)
                    if (currentGradeOpts.isNotEmpty() && grade.isBlank()) {
                        Toast.makeText(context, "Grade/Type is required for $material", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    val currentFinishOpts = getFinishesForMaterial(material, grade, finishes, allMasterData)
                    if (currentFinishOpts.isNotEmpty() && finish.isBlank()) {
                        Toast.makeText(context, "Finish Type is required for $material", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    val currentThicknessOpts = getThicknessOptionsForMaterial(material, allMasterData)
                    if (currentThicknessOpts.isNotEmpty() && thickness.isBlank()) {
                        Toast.makeText(context, "Thickness is required for $material", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (wFeet < 0 || hFeet < 0 || dFeet < 0) {
                        Toast.makeText(context, "Dimensions cannot be negative", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (isAreaBased && (wFeet <= 0.0 || hFeet <= 0.0)) {
                        Toast.makeText(context, "Width and Height are required for $unit", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (isLinearBased && wFeet <= 0.0) {
                        Toast.makeText(context, "Width (Length) is required for $unit", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (isVolumeBased && (wFeet <= 0.0 || hFeet <= 0.0 || dFeet <= 0.0)) {
                        Toast.makeText(context, "Width, Height, and Depth are required for $unit", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    val specs = ItemSpecs(
                        width = widthStr.trim(),
                        height = heightStr.trim(),
                        depth = depthStr.trim(),
                        doorType = "",
                        finish = finish.trim(),
                        hardware = hardware.trim(),
                        brand = brand.trim(),
                        thickness = thickness.trim(),
                        colour = "",
                        laminateImageUri = "",
                        designImageUri = designPath,
                        profileSeries = profileSeries.trim(),
                        profileColour = profileColour.trim(),
                        glassType = glassType.trim(),
                        glassThickness = glassThickness.trim(),
                        acpColour = acpColour.trim(),
                        grade = grade.trim(),
                        cncDesign = cncDesign.trim()
                    )

                    val finalDesc = serializeItemSpecs(userDescription.trim(), specs)
                    val item = QuotationItem(
                        id = if (itemIndex != null) currentItems[itemIndex].id else 0,
                        quotationId = 0,
                        itemName = itemName.trim(),
                        description = finalDesc,
                        material = material,
                        finish = finish.trim(),
                        quantity = qtyVal,
                        unit = unit,
                        rate = rateVal,
                        amount = calculatedAmount
                    )
                    onSave(item)
                }
            ) { Text("Save Item", fontWeight = FontWeight.Bold) }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -- Smart Product Template Library Section --
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = "Smart Template",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Smart Product Template",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    
                    }
                    Text(
                        text = "Select an interior template to auto-configure materials, grades, finishes, hardware, and pricing unit in seconds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val builtInTemplates = getBuiltInTemplates()
                    val customTemplates = getCustomItemTemplates(allMasterData)
                    val allItemTemplates = builtInTemplates + customTemplates
                    val templateNames = listOf("Custom (No Template)") + allItemTemplates.map { it.name }

                    com.example.ui.components.PremiumDropdown(
                        label = "Select Interior Template",
                        value = if (selectedTemplateName.isEmpty()) "Custom (No Template)" else selectedTemplateName,
                        options = templateNames,
                        onValueChange = { tName ->
                            if (tName == "Custom (No Template)") {
                                selectedTemplateName = ""
                            } else {
                                val selectedTemp = allItemTemplates.find { it.name == tName }
                                if (selectedTemp != null) {
                                    selectedTemplateName = selectedTemp.name
                                    itemName = selectedTemp.name
                                    material = selectedTemp.material
                                    grade = selectedTemp.grade
                                    finish = selectedTemp.finish
                                    thickness = selectedTemp.thickness
                                    unit = selectedTemp.unit
                                    hardware = selectedTemp.suggestedHardware.joinToString(", ")
                                    if (userDescription.isBlank()) {
                                        userDescription = selectedTemp.defaultNotes
                                    }
                                }
                            }
                        }
                    )

                    // Dimension Presets
                    val activeTemplate = allItemTemplates.find { it.name == selectedTemplateName }
                    if (activeTemplate != null && activeTemplate.dimensionPresets.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Smart Dimension Presets (Optional):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                activeTemplate.dimensionPresets.forEach { preset ->
                                    val isSelected = widthStr == preset.width && heightStr == preset.height && depthStr == preset.depth
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            widthStr = preset.width
                                            heightStr = preset.height
                                            depthStr = preset.depth
                                        },
                                        label = { Text(preset.label) }
                                    )
                                }
                                FilterChip(
                                    selected = widthStr.isEmpty() && heightStr.isEmpty(),
                                    onClick = {
                                        widthStr = ""
                                        heightStr = ""
                                    },
                                    label = { Text("Custom") }
                                )

                            }
                        }
                    }
                }
            }
            HorizontalDivider()

            // -- 1. Basic Information --
            Text("Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            com.example.ui.components.PremiumOutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = "Item Name *"
            )

            HorizontalDivider()

            // -- 2. Material & Finish --
            Text("Material & Finish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            com.example.ui.components.PremiumDropdown(
                label = "Material Type *",
                value = material,
                options = materialsList,
                onValueChange = { m ->
                    val rules = getMaterialRules(allMasterData)
                    val matchedRule = rules.find { it.material.equals(m, ignoreCase = true) || resolveMaterialType(it.material) == resolveMaterialType(m) }
                    
                    material = m
                    unit = matchedRule?.recommendedUnit ?: getRecommendedUnitForMaterial(m, allMasterData)
                    
                    // Auto-update grade
                    val allowedGrades = getGradesForMaterial(m, allMasterData)
                    if (matchedRule != null && matchedRule.defaultGrade.isNotBlank() && allowedGrades.contains(matchedRule.defaultGrade)) {
                        grade = matchedRule.defaultGrade
                    } else if (!allowedGrades.contains(grade)) {
                        grade = allowedGrades.firstOrNull() ?: ""
                    }
                    
                    // Auto-update finish
                    val allowedFinishes = getFinishesForMaterial(m, grade, finishes, allMasterData)
                    if (matchedRule != null && matchedRule.defaultFinish.isNotBlank() && allowedFinishes.contains(matchedRule.defaultFinish)) {
                        finish = matchedRule.defaultFinish
                    } else if (!allowedFinishes.contains(finish)) {
                        finish = allowedFinishes.firstOrNull() ?: ""
                    }
                    
                    // Auto-update thickness
                    val allowedThicknesses = getThicknessOptionsForMaterial(m, allMasterData)
                    if (matchedRule != null && matchedRule.defaultThickness.isNotBlank() && allowedThicknesses.contains(matchedRule.defaultThickness)) {
                        thickness = matchedRule.defaultThickness
                    } else if (!allowedThicknesses.contains(thickness)) {
                        thickness = allowedThicknesses.firstOrNull() ?: ""
                    }
                    
                    // Auto-update brand (optional)
                    if (matchedRule != null && matchedRule.recommendedBrand.isNotBlank()) {
                        brand = matchedRule.recommendedBrand
                    } else {
                        brand = ""
                    }
                    
                    // Auto-update hardware
                    if (matchedRule != null && matchedRule.recommendedHardware.isNotEmpty()) {
                        val suggested = getRecommendedHardware(itemName, projectType, category)
                        hardware = if (suggested.isNotEmpty()) suggested.joinToString(", ") else matchedRule.recommendedHardware.joinToString(", ")
                    }
                    profileSeries = ""
                    profileColour = ""
                    glassType = ""
                    glassThickness = ""
                    acpColour = ""
                    cncDesign = ""
                }
            )

            val mLower = material.lowercase(Locale.US)
            val gradeOptions = getGradesForMaterial(material, allMasterData)
            val finishOptions = getFinishesForMaterial(material, grade, finishes, allMasterData)
            val thicknessOptions = getThicknessOptionsForMaterial(material, allMasterData)

            // Dynamic Grade / Type Dropdown
            if (gradeOptions.isNotEmpty()) {
                val gradeLabel = when {
                    mLower.contains("aluminium") || mLower.contains("aluminum") -> "Profile Type *"
                    mLower.contains("glass") -> "Glass Type *"
                    else -> "Grade *"
                }
                com.example.ui.components.PremiumDropdown(
                    label = gradeLabel,
                    value = grade,
                    options = gradeOptions,
                    onValueChange = { 
                        grade = it 
                        val newFinishes = getFinishesForMaterial(material, it, finishes, allMasterData)
                        if (finish.isNotBlank() && !newFinishes.contains(finish)) {
                            finish = newFinishes.firstOrNull() ?: ""
                        }
                    }
                )
                
                // Smart Warning for Grade Compatibility
                val isGradeValid = grade.isBlank() || gradeOptions.contains(grade)
                if (!isGradeValid) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = "Incompatible", tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "Incompatible Selection: '$grade' is not applicable for $material.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = "Please select a valid option below:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                gradeOptions.forEach { opt ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { 
                                            grade = opt 
                                            val newFinishes = getFinishesForMaterial(material, opt, finishes, allMasterData)
                                            if (finish.isNotBlank() && !newFinishes.contains(finish)) {
                                                finish = newFinishes.firstOrNull() ?: ""
                                            }
                                        },
                                        label = { Text(opt) }
                                    )

            // Dynamic Finish Dropdown
                                }
                            }
                        }
                    }
                }
            }
            if (finishOptions.isNotEmpty()) {
                com.example.ui.components.PremiumDropdown(
                    label = "Finish Type *",
                    value = finish,
                    options = finishOptions,
                    onValueChange = { finish = it }
                )
                
                // Smart Warning for Finish Compatibility
                val isFinishValid = finish.isBlank() || finishOptions.contains(finish)
                if (!isFinishValid) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = "Incompatible", tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "Incompatible Finish: '$finish' is not applicable for $material.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = "Please select a valid finish below:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                finishOptions.forEach { opt ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { finish = opt },
                                        label = { Text(opt) }
                                    )

            // Dynamic Thickness Dropdown
                                }
                            }
                        }
                    }
                }
            }
            if (thicknessOptions.isNotEmpty()) {
                com.example.ui.components.PremiumDropdown(
                    label = "Thickness *",
                    value = thickness,
                    options = thicknessOptions,
                    onValueChange = { thickness = it }
                )
                
                // Smart Warning for Thickness Compatibility
                val isThicknessValid = thickness.isBlank() || thicknessOptions.contains(thickness)
                if (!isThicknessValid) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = "Incompatible", tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "Incompatible Thickness: '$thickness' is not applicable for $material.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = "Please select a valid thickness below:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                thicknessOptions.forEach { opt ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { thickness = opt },
                                        label = { Text(opt) }
                                    )

            // Material specific extra details (CNC Options for MDF etc.)
                                }
                            }
                        }
                    }
                }
            }
            if (mLower.contains("mdf")) {
                com.example.ui.components.PremiumDropdown(label = "CNC Options", value = cncDesign, options = listOf("None", "Simple Groove", "Complex Pattern", "Jali Design"), onValueChange = { cncDesign = it })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumDropdown(label = "Profile Series", value = profileSeries, options = listOf("18x40 Series", "20x45 Series", "45x45 Series", "Slim Line", "Heavy Duty"), onValueChange = { profileSeries = it }, modifier = Modifier.weight(1f))
                    com.example.ui.components.PremiumDropdown(label = "Profile Colour", value = profileColour, options = listOf("Anodized Silver", "Champagne Gold", "Rose Gold", "Charcoal Grey", "Matt Black"), onValueChange = { profileColour = it }, modifier = Modifier.weight(1f))

            // Recommended Hardware input with Suggestion Badges / Chips
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.ui.components.PremiumOutlinedTextField(
                    value = hardware,
                    onValueChange = { hardware = it },
                    label = "Recommended Hardware"
                )
                
                val recHardwares = getRecommendedHardware(itemName, projectType, category)
                if (recHardwares.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Hardware Suggestions (Optional - Click to add):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recHardwares.forEach { sug ->
                                AssistChip(
                                    onClick = {
                                        val current = hardware.trim()
                                        if (current.isEmpty()) {
                                            hardware = sug
                                        } else {
                                            val parts = current.split(",").map { it.trim() }
                                            if (!parts.any { it.equals(sug, ignoreCase = true) }) {
                                                hardware = "$current, $sug"
                                            }
                                        }
                                    },
                                    label = { Text(sug) }
                                )

            // Material Intelligence Live Summary Card
                            }
                        }
                    }
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Material Intelligence - Live Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                    
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Material", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                            Text(material.ifBlank { "—" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        if (grade.isNotBlank()) {
                            val gradeLbl = when {
                                mLower.contains("aluminium") || mLower.contains("aluminum") -> "Profile Type *"
                                mLower.contains("glass") -> "Glass Type *"
                                else -> "Grade *"
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(gradeLbl, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                                Text(grade, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)

                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        if (finish.isNotBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Finish", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                                Text(finish, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        if (thickness.isNotBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Thickness", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                                Text(thickness, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)

                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pricing Unit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                            Text(unit.ifBlank { "—" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        if (hardware.isNotBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Recommended Hardware", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                                Text(hardware, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)

                            }
                        }
                    }
                }
            }
            HorizontalDivider()

            // -- 3. Dimensions --
            Text(if (isCountBased || isLumpSum) "Dimensions (Optional)" else "Dimensions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 340.dp
                if (isNarrow) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        com.example.ui.components.PremiumOutlinedTextField(
                            value = widthStr,
                            onValueChange = { widthStr = it },
                            label = if (isLinearBased) "Width / Length" + if (!isCountBased && !isLumpSum) " *" else " (Opt)" else "Width" + if (!isCountBased && !isLumpSum) " *" else " (Opt)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth()
                        )
                        com.example.ui.components.PremiumOutlinedTextField(
                            value = heightStr,
                            onValueChange = { heightStr = it },
                            label = "Height" + if (isAreaBased || isVolumeBased) " *" else " (Opt)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth()
                        )
                        com.example.ui.components.PremiumOutlinedTextField(
                            value = depthStr,
                            onValueChange = { depthStr = it },
                            label = "Depth" + if (isVolumeBased) " *" else " (Opt)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            com.example.ui.components.PremiumOutlinedTextField(
                                value = widthStr,
                                onValueChange = { widthStr = it },
                                label = if (isLinearBased) "Width / Length" + if (!isCountBased && !isLumpSum) " *" else " (Opt)" else "Width" + if (!isCountBased && !isLumpSum) " *" else " (Opt)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.weight(1f)
                            )
                            com.example.ui.components.PremiumOutlinedTextField(
                                value = heightStr,
                                onValueChange = { heightStr = it },
                                label = "Height" + if (isAreaBased || isVolumeBased) " *" else " (Opt)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            com.example.ui.components.PremiumOutlinedTextField(
                                value = depthStr,
                                onValueChange = { depthStr = it },
                                label = "Depth" + if (isVolumeBased) " *" else " (Opt)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            HorizontalDivider()

            // -- 4. Pricing --
            Text("Pricing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumDropdown(
                        label = "Unit *",
                        value = unit,
                        options = listOf("Sq.Ft", "Sq.M", "R.Ft", "Meter", "Cu.Ft", "Cu.M", "Nos", "Pcs", "Lumpsum", "Set"),
                        onValueChange = { newUnit ->
                            unit = newUnit
                            val newULower = newUnit.trim().lowercase(Locale.US)
                            val newVol = newULower.contains("cu") || newULower.contains("cubic") || newULower.contains("cft") || newULower.contains("cum")
                            val newArea = !newVol && (newULower.contains("sq") || newULower.contains("sft") || newULower.contains("square"))
                            val newLinear = !newVol && !newArea && (newULower.contains("rft") || newULower.contains("run") || newULower.contains("meter") || newULower.contains("mtr") || newULower == "r.m" || newULower == "rm")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    com.example.ui.components.PremiumOutlinedTextField(value = quantityStr, onValueChange = { quantityStr = it }, label = "Qty *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumOutlinedTextField(value = rateStr, onValueChange = { rateStr = it }, label = "Rate *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Smart Live Cost & Specs Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Auto-Calculated",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Template", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(selectedTemplateName.ifBlank { "None (Custom)" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Material", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(material.ifBlank { "—" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)

                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Finish", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(finish.ifBlank { "—" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dimensions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            val dimText = listOfNotNull(
                                if (widthStr.isNotBlank()) "W: $widthStr" else null,
                                if (heightStr.isNotBlank()) "H: $heightStr" else null,
                                if (depthStr.isNotBlank()) "D: $depthStr" else null
                            ).joinToString(" × ")
                            Text(dimText.ifBlank { "—" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)

                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        val billableLabel = when {
                            isAreaBased -> "Billable Area"
                            isLinearBased -> "Billable Length"
                            isVolumeBased -> "Billable Volume"
                            isCountBased -> "Billable Count"
                            else -> "Billable Quantity"
                        }
                        val unitDisplayLabel = when {
                            uLower.contains("sq.m") || uLower.contains("sqm") -> "Sq.M"
                            uLower.contains("sq") || uLower.contains("sft") -> "Sq.Ft"
                            uLower.contains("meter") || uLower.contains("mtr") -> "Meter"
                            uLower.contains("rft") || uLower.contains("run") -> "R.Ft"
                            uLower.contains("cu.m") || uLower.contains("cum") -> "Cu.M"
                            uLower.contains("cu") || uLower.contains("cft") -> "Cu.Ft"
                            uLower.contains("pcs") -> "Pcs"
                            uLower.contains("nos") -> "Nos"
                            else -> unit
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(billableLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(String.format(Locale.US, "%.2f %s", totalBillableQty, unitDisplayLabel), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quantity & Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text("$qtyVal (Qty) × ₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(rateVal)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)

                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Estimated Amount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "₹${com.example.utils.CurrencyFormatter.formatIndianCurrencyStrict(calculatedAmount)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                    }
                }
            }
            HorizontalDivider()

            // -- 6. Reference Image --
            Text("Reference Image", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            if (designPath.isNotEmpty() && java.io.File(context.filesDir, java.io.File(designPath).name).exists()) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(java.io.File(context.filesDir, java.io.File(designPath).name))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Design Preview",
                    modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(if (designPath.isNotEmpty()) "Image Attached" else "No Image Attached", color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.example.ui.components.PremiumSecondaryButton(
                    onClick = { designLauncher.launch("image/*") },
                    modifier = Modifier.width(150.dp)
                ) { Text(if (designPath.isNotEmpty()) "Change Image" else "Attach Image", fontWeight = FontWeight.SemiBold) }

            }
            HorizontalDivider()

            // -- 7. Notes --
            Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            com.example.ui.components.PremiumOutlinedTextField(
                value = userDescription,
                onValueChange = { userDescription = it },
                label = "Description / Additional Notes (Optional)",
                singleLine = false,
                minLines = 3
            )
        }
    }
}
// --- STEP 4: TAXES, WARRANTIES & TERMS ---
@Composable
fun WizardStepTaxesTerms(
    discount: Double,
    gstRate: Double,
    warranty: String,
    terms: String,
    masterWarranties: List<String>,
    onDiscountChange: (Double) -> Unit,
    onGstChange: (Double) -> Unit,
    onWarrantyChange: (String) -> Unit,
    onTermsChange: (String) -> Unit,
    onNext: () -> Unit
) {
    var discountStr by remember { mutableStateOf(if (discount > 0) discount.toString() else "") }
    var gstRateStr by remember { mutableStateOf(gstRate.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Discount & Taxes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Flat Discount
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = discountStr,
                        onValueChange = {
                            discountStr = it
                            onDiscountChange(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = "Flat Discount (₹)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    // GST Rate
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = gstRateStr,
                        onValueChange = {
                            gstRateStr = it
                            onGstChange(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = "GST Rate (%)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Warranty & Support Limits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Warranty dropdown
                com.example.ui.components.PremiumDropdown(
                    value = warranty.ifEmpty { "Select warranty limit" },
                    onValueChange = onWarrantyChange,
                    label = "Warranty",
                    options = if (masterWarranties.isEmpty()) listOf("1 Year Warranty", "3 Years Warranty", "5 Years Warranty", "No Warranty") else masterWarranties,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quotation Terms & Conditions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                com.example.ui.components.PremiumOutlinedTextField(
                    value = terms,
                    onValueChange = onTermsChange,
                    label = "Terms & Conditions",
                    placeholder = "Leave blank to use default Company Terms & Conditions",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }
    }
}

// --- STEP 5: REVIEW & SAVE ---
@Composable
fun WizardStepReview(
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
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Billing Adjustments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it; quotationViewModel.setDiscount(it.toDoubleOrNull() ?: 0.0) },
                        label = "Discount Amount (₹)",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = gstRateStr,
                        onValueChange = { gstRateStr = it; quotationViewModel.setGstRate(it.toDoubleOrNull() ?: 0.0) },
                        label = "GST Rate (%)",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = transportStr,
                        onValueChange = { transportStr = it; quotationViewModel.setTransport(it.toDoubleOrNull() ?: 0.0) },
                        label = "Transport (₹)",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = installationStr,
                        onValueChange = { installationStr = it; quotationViewModel.setInstallation(it.toDoubleOrNull() ?: 0.0) },
                        label = "Installation (₹)",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = extraStr,
                        onValueChange = { extraStr = it; quotationViewModel.setExtraCharges(it.toDoubleOrNull() ?: 0.0) },
                        label = "Extra Charges (₹)",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    com.example.ui.components.PremiumOutlinedTextField(
                        value = roundOffStr,
                        onValueChange = { roundOffStr = it; quotationViewModel.setRoundOff(it.toDoubleOrNull() ?: 0.0) },
                        label = "Round Off (₹)",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
                
                com.example.ui.components.PremiumOutlinedTextField(
                    value = advanceStr,
                    onValueChange = { advanceStr = it; quotationViewModel.setAdvance(it.toDoubleOrNull() ?: 0.0) },
                    label = "Advance Received (₹)",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }

        // Summary
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
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
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Terms & Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                com.example.ui.components.PremiumOutlinedTextField(
                    value = customerNotes,
                    onValueChange = { quotationViewModel.setCustomerNotes(it) },
                    label = "Customer Notes (Visible on PDF)",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                com.example.ui.components.PremiumOutlinedTextField(
                    value = internalNotes,
                    onValueChange = { quotationViewModel.setInternalNotes(it) },
                    label = "Internal Notes (Private)",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                com.example.ui.components.PremiumOutlinedTextField(
                    value = terms,
                    onValueChange = { quotationViewModel.setTerms(it) },
                    label = "Terms & Conditions",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                com.example.ui.components.PremiumOutlinedTextField(
                    value = warranty,
                    onValueChange = { quotationViewModel.setWarranty(it) },
                    label = "Warranty Terms",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                com.example.ui.components.PremiumOutlinedTextField(
                    value = quotationViewModel.newQuoteDeliveryTime.collectAsState().value,
                    onValueChange = { quotationViewModel.setDeliveryTime(it) },
                    label = "Delivery Time",
                    modifier = Modifier.fillMaxWidth()
                )

                com.example.ui.components.PremiumOutlinedTextField(
                    value = quotationViewModel.newQuoteInstallationTime.collectAsState().value,
                    onValueChange = { quotationViewModel.setInstallationTime(it) },
                    label = "Installation Time",
                    modifier = Modifier.fillMaxWidth()
                )

                com.example.ui.components.PremiumOutlinedTextField(
                    value = quotationViewModel.newQuotePaymentTerms.collectAsState().value,
                    onValueChange = { quotationViewModel.setPaymentTerms(it) },
                    label = "Payment Terms",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                com.example.ui.components.PremiumOutlinedTextField(
                    value = quotationViewModel.newQuoteAdditionalConditions.collectAsState().value,
                    onValueChange = { quotationViewModel.setAdditionalConditions(it) },
                    label = "Additional Conditions",
                    modifier = Modifier.fillMaxWidth(),
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
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Quotation", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
fun QuotationItemCard(
    item: QuotationItem,
    index: Int,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val (userDesc, specs) = parseItemSpecs(item.description)
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (userDesc.isNotBlank() && !userDesc.startsWith("ItemSpecs") && !userDesc.startsWith("ItemDescription")) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userDesc,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                
                    }
                }
                Text(
                    text = "₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(item.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Right
                )
            
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.material.isNotBlank()) {
                    SpecRow("Material", item.material)
                
                }
                val finish = if (item.material == "Aluminium") {
                    specs.profileColour.trim()
                } else if (item.material == "ACP") {
                    specs.acpColour.trim()
                } else {
                    item.finish
                }
                if (finish.isNotBlank()) {
                    SpecRow("Finish", finish)
                
                }
                if (specs.thickness.isNotBlank()) {
                    SpecRow("Thickness", specs.thickness)
                
                }
                if (specs.width.isNotBlank() && specs.height.isNotBlank()) {
                    val sizeLabel = buildString {
                        append(specs.width)
                        append("' × ")
                        append(specs.height)
                        append("'")
                        if (specs.depth.isNotBlank()) {
                            append(" × ")
                            append(specs.depth)
                            append("'")
                        }
                    }
                    SpecRow("Size", sizeLabel)
                }
                if (specs.doorType.isNotBlank()) {
                    SpecRow("Door Type", specs.doorType)
                }
                if (specs.hardware.isNotBlank()) {
                    SpecRow("Hardware", specs.hardware)
                }
                if (specs.glassType.isNotBlank()) {
                    SpecRow("Glass", specs.glassType)
                
                }
                val rawQty = if (item.rawQuantity > 0.0) item.rawQuantity else item.quantity
                val qtyLabel = if (rawQty % 1.0 == 0.0) rawQty.toInt().toString() else rawQty.toString()
                
                if (item.unit == "LUMPSUM" || item.unit == "NOS" || item.unit == "PCS") {
                    SpecRow("Qty", "$qtyLabel ${item.unit}")
                } else {
                    SpecRow("Qty", qtyLabel)
                    val billableQty = if (item.billableQuantity > 0.0) item.billableQuantity else item.quantity
                    val billableLabel = if (billableQty % 1.0 == 0.0) billableQty.toInt().toString() else billableQty.toString()
                    val billablePrefix = when (item.unit) {
                        "SQ_FT", "SQ_M" -> "Area"
                        "CU_FT", "CU_M" -> "Volume"
                        "R_FT", "METER" -> "Length"
                        else -> "Billable Qty"
                    }
                    SpecRow(billablePrefix, "$billableLabel ${item.unit}")
                }
                
                SpecRow("Rate", "₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(item.rate)}")
            
            }
            if (specs.laminateImageUri.isNotBlank() || specs.designImageUri.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (specs.laminateImageUri.isNotBlank()) {
                        Column {
                            Text("Laminate Preview", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(java.io.File(androidx.compose.ui.platform.LocalContext.current.filesDir, java.io.File(specs.laminateImageUri).name))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Laminate Preview",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                    if (specs.designImageUri.isNotBlank()) {
                        Column {
                            Text("Design Ref", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(java.io.File(androidx.compose.ui.platform.LocalContext.current.filesDir, java.io.File(specs.designImageUri).name))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Design Preview",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
            
                        }
                    }
                }
            }
            if (onEdit != null || onDelete != null || onDuplicate != null || onMoveUp != null || onMoveDown != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move Up", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move Down", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Item", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (onDuplicate != null) {
                        IconButton(onClick = onDuplicate, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate Item", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label :",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(85.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
