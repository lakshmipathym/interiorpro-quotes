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
    val grade: String = ""
)

fun parseItemSpecs(description: String): Pair<String, ItemSpecs> {
    if (!description.contains("|||")) {
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
            grade = json.optString("grade", "")
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
        for (step in 1..5) {
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

            if (step < 5) {
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
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
    val currentProjectType by quotationViewModel.newQuoteProjectType.collectAsState()
    val currentCategory by quotationViewModel.newQuoteCategory.collectAsState()
    val currentMaterial by quotationViewModel.newQuoteMaterial.collectAsState()
    val currentFinish by quotationViewModel.newQuoteFinish.collectAsState()
    val currentTemplate by quotationViewModel.newQuoteTemplate.collectAsState()
    val quoteItems by quotationViewModel.newQuoteItems.collectAsState()
    val discount by quotationViewModel.newQuoteDiscount.collectAsState()
    val gstRate by quotationViewModel.newQuoteGstRate.collectAsState()
    val termsAndConditions by quotationViewModel.newQuoteTerms.collectAsState()
    val warranty by quotationViewModel.newQuoteWarranty.collectAsState()
    val quoteNumber by quotationViewModel.newQuoteNumber.collectAsState()
    
    val subtotal by quotationViewModel.newQuoteSubtotal.collectAsState()
    val gstAmount by quotationViewModel.newQuoteGstAmount.collectAsState()
    val grandTotal by quotationViewModel.newQuoteGrandTotal.collectAsState()

    // Master Dropdown items
    val masterData by quotationViewModel.allMasterData.collectAsState()
    val projectTypes = masterData.filter { it.type == "PROJECT_TYPE" }.map { it.value }
    val categories = masterData.filter { it.type == "CATEGORY" }.map { it.value }
    val materials = masterData.filter { it.type == "MATERIAL" }.map { it.value }
    val finishes = masterData.filter { it.type == "FINISH_TYPE" }.map { it.value }
    val templates by quotationViewModel.allTemplates.collectAsState()

    // Dialog state for completed save
    var savedQuotationId by remember { mutableStateOf<Int?>(null) }
    var isQuickAddCustomerOpen by remember { mutableStateOf(false) }

    // Prevent accidental exit using system back button
    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (activeStep > 1) {
            activeStep--
        } else {
            showDiscardDialog = true
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Quotation?") },
            text = { Text("Are you sure you want to exit the quotation wizard? All progress on this quotation will be discarded.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        quotationViewModel.startNewQuotation()
                        onSuccessReturn()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continue Editing")
                }
            }
        )
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
                if (activeStep > 1) {
                    IconButton(onClick = { activeStep-- }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = when (activeStep) {
                        1 -> "Configure Quotation"
                        else -> "Review & Save"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Step $activeStep of 2",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            // Step Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (step in 1..2) {
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
                    1 -> WizardStepCustomer(
                        customerViewModel = customerViewModel,
                        quotationViewModel = quotationViewModel,
                        quoteItems = quoteItems,
                        onOpenQuickAdd = { isQuickAddCustomerOpen = true }
                    )
                    
                    else -> WizardStepReview(
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
                        onSave = {
                            // Consolidate spec summary string from items
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

                            // Copy reference design image
                            val firstItemWithDesign = quoteItems.firstOrNull { 
                                val (_, specs) = parseItemSpecs(it.description)
                                specs.designImageUri.isNotBlank()
                            }
                            firstItemWithDesign?.let { item ->
                                val (_, specs) = parseItemSpecs(item.description)
                                val file = File(specs.designImageUri)
                                if (file.exists()) {
                                    try {
                                        val destFile = File(context.filesDir, "design_${quoteNumber.replace("/", "_")}.jpg")
                                        file.copyTo(destFile, overwrite = true)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            // Save
                            quotationViewModel.saveQuotation { id ->
                                savedQuotationId = id
                            }
                        }
                    )
                }
            }

            // Persistent bottom navigation footer
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
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back")
                    }
                }

                val nextButtonWeight = if (activeStep > 1) 1f else 2f
                val isNextEnabled = when (activeStep) {
                    1 -> currentCustomer != null && quoteItems.isNotEmpty()
                    else -> true
                }

                Button(
                    onClick = {
                        when (activeStep) {
                            1 -> activeStep = 2
                            else -> { /* Handled inside WizardStepReview save callback */ }
                        }
                    },
                    enabled = isNextEnabled,
                    modifier = Modifier
                        .weight(nextButtonWeight)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (activeStep < 2) "Next to Review" else "Save Quotation")
                    if (activeStep < 2) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null)
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
                }
                isQuickAddCustomerOpen = false
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
                    // Quotation Number Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
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
                            Icon(Icons.Filled.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
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

// --- STEP 1: CUSTOMER SELECTION & QUOTATION ITEMS ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WizardStepCustomer(
    customerViewModel: com.example.ui.customer.CustomerViewModel,
    quotationViewModel: com.example.ui.quotation.QuotationViewModel,
    quoteItems: List<QuotationItem>,
    onOpenQuickAdd: () -> Unit
) {
    val customers by customerViewModel.customers.collectAsState()
    val searchQuery by customerViewModel.searchQuery.collectAsState()
    val selectedCustomer by quotationViewModel.newQuoteCustomer.collectAsState()

    var siteName by remember { mutableStateOf("") }
    var siteAddress by remember { mutableStateOf("") }

    // Sync site state with selected customer
    LaunchedEffect(selectedCustomer) {
        selectedCustomer?.let {
            siteName = it.siteLocation
            siteAddress = it.address
        }
    }

    var showItemConfigDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedCustomer == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Customer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Search Box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { customerViewModel.searchQuery.value = it },
                            label = { Text("Search Customer (Name, Phone...)") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { customerViewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Customers list
                        val displayList = customers.take(4) // Show top 4 matching
                        if (displayList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
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
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                quotationViewModel.selectCustomer(cust)
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = cust.customerName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Phone: ${cust.mobileNumber}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Filled.AddCircle,
                                                contentDescription = "Select",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Inline Add Customer Button
                        Button(
                            onClick = onOpenQuickAdd,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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
                // Selected Customer Elegantly
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedCustomer!!.customerName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Phone: ${selectedCustomer!!.mobileNumber}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { quotationViewModel.startNewQuotation() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Change", fontSize = 11.sp)
                        }
                    }
                }

                // Site Details Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Site Location Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = siteName,
                            onValueChange = {
                                siteName = it
                                selectedCustomer?.let { cust ->
                                    val updated = cust.copy(siteLocation = it)
                                    quotationViewModel.selectCustomer(updated)
                                    customerViewModel.updateCustomer(updated)
                                }
                            },
                            label = { Text("Site Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = siteAddress,
                            onValueChange = {
                                siteAddress = it
                                selectedCustomer?.let { cust ->
                                    val updated = cust.copy(address = it)
                                    quotationViewModel.selectCustomer(updated)
                                    customerViewModel.updateCustomer(updated)
                                }
                            },
                            label = { Text("Site Address *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2
                        )
                    }
                }

                // Clean Item List Title & Content
                Text(
                    text = "Quotation Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (quoteItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.LibraryAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No items added yet.",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Use the '+' button below to add your first item.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        quoteItems.forEachIndexed { index, item ->
                            val (userDesc, specs) = parseItemSpecs(item.description)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.itemName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (userDesc.isNotBlank()) {
                                                Text(
                                                    text = userDesc,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Text(
                                            text = "₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(item.amount)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Specs chips row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Material Badge
                                        if (item.material.isNotBlank()) {
                                            SpecTagChip(label = item.material, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                        }
                                        if (specs.width.isNotBlank() && specs.height.isNotBlank()) {
                                            val sizeLabel = if (specs.depth.isNotBlank()) {
                                                "${specs.width}x${specs.height}x${specs.depth} Ft"
                                            } else {
                                                "${specs.width}x${specs.height} Ft"
                                            }
                                            SpecTagChip(label = sizeLabel, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                        }
                                        
                                        // Material specific specs
                                        when (item.material) {
                                            "Plywood" -> {
                                                if (specs.thickness.isNotBlank()) {
                                                    SpecTagChip(label = "Thick: ${specs.thickness}", color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                                }
                                                if (specs.grade.isNotBlank()) {
                                                    SpecTagChip(label = "Grade: ${specs.grade}", color = MaterialTheme.colorScheme.surfaceVariant)
                                                }
                                            }
                                            "Aluminium" -> {
                                                if (specs.profileSeries.isNotBlank()) {
                                                    SpecTagChip(label = "Series: ${specs.profileSeries}", color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                                }
                                                if (specs.profileColour.isNotBlank()) {
                                                    SpecTagChip(label = "Color: ${specs.profileColour}", color = MaterialTheme.colorScheme.surfaceVariant)
                                                }
                                                if (specs.glassType.isNotBlank()) {
                                                    SpecTagChip(label = "Glass: ${specs.glassType}", color = MaterialTheme.colorScheme.surfaceVariant)
                                                }
                                            }
                                            "Glass" -> {
                                                if (specs.glassType.isNotBlank()) {
                                                    SpecTagChip(label = "Type: ${specs.glassType}", color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                                }
                                                if (specs.glassThickness.isNotBlank()) {
                                                    SpecTagChip(label = "Thick: ${specs.glassThickness}", color = MaterialTheme.colorScheme.surfaceVariant)
                                                }
                                            }
                                            "ACP" -> {
                                                if (specs.acpColour.isNotBlank()) {
                                                    SpecTagChip(label = "Color: ${specs.acpColour}", color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                                }
                                            }
                                            "WPC" -> {
                                                if (specs.thickness.isNotBlank()) {
                                                    SpecTagChip(label = "Thick: ${specs.thickness}", color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                                }
                                            }
                                        }
                                    }

                                    if (specs.designImageUri.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column {
                                            Text("Reference Image", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            AsyncImage(
                                                model = File(specs.designImageUri),
                                                contentDescription = "Reference Preview",
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.outlineVariant),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Column {
                                                Text("Quantity", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${item.quantity} ${item.unit}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Column {
                                                Text("Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(item.rate)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    editingItemIndex = index
                                                    showItemConfigDialog = true
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit Item", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }

                                            IconButton(
                                                onClick = { quotationViewModel.duplicateQuoteItem(index) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate Item", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                            }

                                            IconButton(
                                                onClick = { quotationViewModel.removeQuoteItem(index) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
        }

        // Floating Action Button overlay for adding item directly
        if (selectedCustomer != null) {
            FloatingActionButton(
                onClick = {
                    editingItemIndex = null
                    showItemConfigDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Item")
            }
        }
    }

    if (showItemConfigDialog) {
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
            }
        )
    }
}

// --- STEP 2: PROJECT SELECTION ---
@Composable
fun WizardStepConfig(
    templates: List<QuotationTemplate>,
    projectTypes: List<String>,
    materials: List<String>,
    currentTemplate: QuotationTemplate?,
    currentMaterial: String,
    currentProjectType: String,
    onSelectTemplate: (QuotationTemplate?) -> Unit,
    onSelectMaterial: (String) -> Unit,
    onSelectProjectType: (String) -> Unit
) {
    var expandedTemplate by remember { mutableStateOf(false) }
    var expandedProjectType by remember { mutableStateOf(false) }
    var expandedMaterial by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Project Scope",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Project Preset (Optional)
                Column {
                    Text(
                        "Project Preset (Optional)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currentTemplate?.name ?: "No Preset Selected (Custom)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expandedTemplate = true }
                        )
                        DropdownMenu(
                            expanded = expandedTemplate,
                            onDismissRequest = { expandedTemplate = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("No Preset (Custom from scratch)") },
                                onClick = {
                                    onSelectTemplate(null)
                                    expandedTemplate = false
                                }
                            )
                            templates.forEach { tmpl ->
                                DropdownMenuItem(
                                    text = { Text(tmpl.name) },
                                    onClick = {
                                        onSelectTemplate(tmpl)
                                        expandedTemplate = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Product Type (Mandatory)
                Column {
                    Text(
                        "Product Type *",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val displayVal = currentProjectType.ifEmpty { "Select Product Type" }
                        OutlinedTextField(
                            value = displayVal,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expandedProjectType = true }
                        )
                        DropdownMenu(
                            expanded = expandedProjectType,
                            onDismissRequest = { expandedProjectType = false }
                        ) {
                            val list = if (projectTypes.isEmpty()) listOf("Modular Kitchen", "Wardrobe", "Living Room TV Unit", "Full Home Interior") else projectTypes
                            list.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        onSelectProjectType(type)
                                        expandedProjectType = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Material Type (Mandatory)
                Column {
                    Text(
                        "Material Type *",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val displayVal = currentMaterial.ifEmpty { "Select Material Type" }
                        OutlinedTextField(
                            value = displayVal,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expandedMaterial = true }
                        )
                        DropdownMenu(
                            expanded = expandedMaterial,
                            onDismissRequest = { expandedMaterial = false }
                        ) {
                            val list = if (materials.isEmpty()) listOf("BWP Plywood", "MDF (Exterior Grade)", "HDF", "Particle Board", "Aluminium Section Framework") else materials
                            list.forEach { mat ->
                                DropdownMenuItem(
                                    text = { Text(mat) },
                                    onClick = {
                                        onSelectMaterial(mat)
                                        expandedMaterial = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
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
            Button(
                onClick = {
                    editingItemIndex = null
                    showItemConfigDialog = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Item", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = { showSuggestionsDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
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
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.LibraryAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your quotation is empty.",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Click 'Add Item' or load suggestions to begin.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quoteItems.size) { index ->
                    val item = quoteItems[index]
                    val (userDesc, specs) = parseItemSpecs(item.description)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.itemName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (userDesc.isNotBlank()) {
                                        Text(
                                            text = userDesc,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    text = "₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(item.amount)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Specs chips row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (specs.width.isNotBlank() && specs.height.isNotBlank()) {
                                    val sizeLabel = if (specs.depth.isNotBlank()) {
                                        "${specs.width}x${specs.height}x${specs.depth} Ft"
                                    } else {
                                        "${specs.width}x${specs.height} Ft"
                                    }
                                    SpecTagChip(label = sizeLabel, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                }
                                if (specs.doorType.isNotBlank()) {
                                    SpecTagChip(label = specs.doorType, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                }
                                if (specs.finish.isNotBlank()) {
                                    SpecTagChip(label = specs.finish, color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                }
                                if (specs.hardware.isNotBlank()) {
                                    SpecTagChip(label = specs.hardware, color = MaterialTheme.colorScheme.surfaceVariant)
                                }
                            }

                            // Visual Previews (Laminate & Design Reference Images)
                            if (specs.laminateImageUri.isNotBlank() || specs.designImageUri.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (specs.laminateImageUri.isNotBlank()) {
                                        Column {
                                            Text("Laminate Preview", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            AsyncImage(
                                                model = File(specs.laminateImageUri),
                                                contentDescription = "Laminate Preview",
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.outlineVariant),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    if (specs.designImageUri.isNotBlank()) {
                                        Column {
                                            Text("Design Ref", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            AsyncImage(
                                                model = File(specs.designImageUri),
                                                contentDescription = "Design Preview",
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.outlineVariant),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text("Quantity", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${item.quantity} ${item.unit}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Column {
                                        Text("Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(item.rate)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            editingItemIndex = index
                                            showItemConfigDialog = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit Item", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { quotationViewModel.removeQuoteItem(index) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Item Configuration / Input Dialog
    if (showItemConfigDialog) {
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
            }
        )
    }

    // Smart Suggestions Dialog
    if (showSuggestionsDialog) {
        SmartSuggestionsDialog(
            selectedProduct = currentProjectType,
            selectedMaterial = currentMaterial,
            selectedFinish = currentFinish,
            specificationSummary = "", // simplified
            onAddItems = { suggestions ->
                suggestions.forEach { quotationViewModel.addQuoteItem(it) }
            },
            onDismiss = { showSuggestionsDialog = false }
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
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun calculateQuantity(width: Double, height: Double, qtyCount: Double, unit: String): Double {
    val u = unit.trim().lowercase(Locale.US)
    return when {
        u == "nos" || u.contains("nos") || u == "pcs" || u.contains("piece") -> {
            qtyCount
        }
        u == "sq.ft" || u.contains("sq.ft") || u.contains("sqft") || u == "sft" -> {
            width * height * qtyCount
        }
        u == "running feet" || u.contains("run") || u.contains("rft") || u == "meter" || u.contains("meter") || u == "mtr" || u.contains("mtr") -> {
            width * qtyCount
        }
        u == "sq.m" || u.contains("sq.m") || u.contains("sqm") || u.contains("square meter") -> {
            width * height * 0.09290304 * qtyCount
        }
        u == "bundle" || u.contains("bundle") -> {
            qtyCount
        }
        u == "sheet" || u.contains("sheet") -> {
            qtyCount
        }
        u == "kg" || u.contains("kg") || u.contains("kilogram") -> {
            qtyCount
        }
        else -> {
            qtyCount
        }
    }
}

fun isDimensionUnit(unit: String): Boolean {
    val u = unit.trim().lowercase(Locale.US)
    return u.contains("sq") || u.contains("sft") || u.contains("rft") || u.contains("run") || u.contains("feet") || u.contains("meter") || u.contains("mtr")
}

fun isAreaUnit(unit: String): Boolean {
    val u = unit.trim().lowercase(Locale.US)
    return u.contains("sq") || u.contains("sft")
}

// --- ITEM CONFIGURATION FORM DIALOG ---
@Composable
fun ItemConfigDialog(
    itemIndex: Int?,
    currentItems: List<QuotationItem>,
    onDismiss: () -> Unit,
    onSave: (QuotationItem) -> Unit
) {
    val context = LocalContext.current

    // Properties
    var itemName by remember { mutableStateOf("") }
    var userDescription by remember { mutableStateOf("") }
    
    // Material selection
    var material by remember { mutableStateOf("Plywood") }
    
    // Material specific properties
    var profileSeries by remember { mutableStateOf("") }
    var profileColour by remember { mutableStateOf("") }
    var glassType by remember { mutableStateOf("") }
    var glassThickness by remember { mutableStateOf("") }
    var acpColour by remember { mutableStateOf("") }
    var thickness by remember { mutableStateOf("") } // Used for Plywood & WPC
    var grade by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var hardware by remember { mutableStateOf("") }
    
    // Common properties
    var widthStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var depthStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Sq.Ft") }
    var quantityStr by remember { mutableStateOf("1.0") }
    var rateStr by remember { mutableStateOf("") }
    
    // Reference design image path
    var designPath by remember { mutableStateOf("") }

    // Launcher for reference image
    val designLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val copied = copyUriToInternalStorage(context, it, "temp_des_${System.currentTimeMillis()}.jpg")
            copied?.let { designPath = it }
        }
    }

    // Load values if we are editing an existing item
    LaunchedEffect(itemIndex) {
        if (itemIndex != null && itemIndex in currentItems.indices) {
            val existing = currentItems[itemIndex]
            itemName = existing.itemName
            unit = existing.unit
            rateStr = existing.rate.toString()
            material = if (existing.material.isNotBlank()) existing.material else "Plywood"

            val (desc, specs) = parseItemSpecs(existing.description)
            userDescription = desc
            
            profileSeries = specs.profileSeries
            profileColour = specs.profileColour
            glassType = specs.glassType
            glassThickness = specs.glassThickness
            acpColour = specs.acpColour
            thickness = specs.thickness
            grade = specs.grade
            brand = specs.brand
            hardware = specs.hardware
            
            widthStr = specs.width
            heightStr = specs.height
            depthStr = specs.depth
            designPath = specs.designImageUri

            // Reconstruct the Number of Units / count from the saved total quantity
            val w = specs.width.toDoubleOrNull() ?: 1.0
            val h = specs.height.toDoubleOrNull() ?: 1.0
            val uLower = existing.unit.trim().lowercase(Locale.US)
            val numUnits = when {
                uLower == "sq.ft" || uLower.contains("sq.ft") || uLower.contains("sqft") || uLower == "sft" -> {
                    val area = w * h
                    if (area > 0) existing.quantity / area else existing.quantity
                }
                uLower == "running feet" || uLower.contains("run") || uLower.contains("rft") -> {
                    if (w > 0) existing.quantity / w else existing.quantity
                }
                uLower == "sq.m" || uLower.contains("sq.m") || uLower.contains("sqm") || uLower.contains("square meter") -> {
                    val areaM = w * h * 0.09290304
                    if (areaM > 0) existing.quantity / areaM else existing.quantity
                }
                else -> {
                    existing.quantity
                }
            }
            quantityStr = if (numUnits % 1.0 == 0.0) {
                String.format(Locale.US, "%.0f", numUnits)
            } else {
                String.format(Locale.US, "%.2f", numUnits)
            }
        }
    }

    // Unused recompute function
    fun recomputeQuantity() {
        // Calculation logic is now fully dynamic and verified on save
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (itemIndex == null) "Configure New Item" else "Edit Item Config",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Form organized in logical visual sections
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // --- SECTION 1: ITEM INFORMATION ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Item Information", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            // Item Name
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("Item Name * (e.g. Wardrobe, Loft, TV Unit)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Description / Notes
                            OutlinedTextField(
                                value = userDescription,
                                onValueChange = { userDescription = it },
                                label = { Text("Item Custom Notes / Description (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // --- SECTION 2: MATERIAL DETAILS ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Material Details", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            // Material Selector Dropdown
                            var expandedMaterial by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = material,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Material *") },
                                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { expandedMaterial = true })
                                 DropdownMenu(expanded = expandedMaterial, onDismissRequest = { expandedMaterial = false }) {
                                     listOf("Plywood", "Particle Board", "MDF", "HDHMR", "WPC", "Aluminium", "ACP", "Glass", "PVC Board", "Hardware").forEach { m ->
                                         DropdownMenuItem(text = { Text(m) }, onClick = {
                                             material = m
                                             // Reset all material-specific properties immediately
                                             thickness = ""
                                             grade = ""
                                             profileSeries = ""
                                             profileColour = ""
                                             glassType = ""
                                             glassThickness = ""
                                             acpColour = ""
                                             brand = ""
                                             hardware = ""
                                             expandedMaterial = false
                                         })
                                     }
                                 }
                             }

                             // Dynamic fields based on selected material
                             val mLower = material.lowercase(Locale.US)
                             when {
                                 mLower.contains("plywood") -> {
                                     SpecDropdownField(
                                         value = thickness,
                                         onValueChange = { thickness = it },
                                         label = "Thickness *",
                                         options = listOf("6 mm", "8 mm", "12 mm", "16 mm", "18 mm", "25 mm")
                                     )
                                     SpecDropdownField(
                                         value = grade,
                                         onValueChange = { grade = it },
                                         label = "Grade *",
                                         options = listOf("MR", "BWR", "BWP", "Marine")
                                     )
                                 }
                                 mLower.contains("particle") -> {
                                     SpecDropdownField(
                                         value = thickness,
                                         onValueChange = { thickness = it },
                                         label = "Thickness *",
                                         options = listOf("9 mm", "12 mm", "15 mm", "18 mm", "25 mm")
                                     )
                                     SpecDropdownField(
                                         value = grade,
                                         onValueChange = { grade = it },
                                         label = "Grade (Optional)",
                                         options = listOf("None", "Standard", "Premium", "Pre-laminated")
                                     )
                                 }
                                 mLower.contains("mdf") -> {
                                     SpecDropdownField(
                                         value = thickness,
                                         onValueChange = { thickness = it },
                                         label = "Thickness *",
                                         options = listOf("6 mm", "8 mm", "12 mm", "15 mm", "18 mm", "25 mm")
                                     )
                                 }
                                 mLower.contains("hdhmr") -> {
                                     SpecDropdownField(
                                         value = thickness,
                                         onValueChange = { thickness = it },
                                         label = "Thickness *",
                                         options = listOf("6 mm", "8 mm", "12 mm", "16 mm", "18 mm", "25 mm")
                                     )
                                 }
                                 mLower.contains("wpc") -> {
                                     SpecDropdownField(
                                         value = thickness,
                                         onValueChange = { thickness = it },
                                         label = "Thickness *",
                                         options = listOf("6 mm", "8 mm", "12 mm", "15 mm", "18 mm", "25 mm")
                                     )
                                 }
                                 mLower.contains("pvc") -> {
                                     SpecDropdownField(
                                         value = thickness,
                                         onValueChange = { thickness = it },
                                         label = "Thickness *",
                                         options = listOf("6 mm", "8 mm", "12 mm", "15 mm", "18 mm", "25 mm")
                                     )
                                 }
                                 mLower.contains("aluminium") -> {
                                     Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                         SpecDropdownField(
                                             value = profileSeries,
                                             onValueChange = { profileSeries = it },
                                             label = "Profile Series *",
                                             options = listOf("18x40 Series", "20x45 Series", "45x45 Series", "Slim Line", "Heavy Duty"),
                                             modifier = Modifier.weight(1f)
                                         )
                                         SpecDropdownField(
                                             value = profileColour,
                                             onValueChange = { profileColour = it },
                                             label = "Profile Colour *",
                                             options = listOf("Anodized Silver", "Champagne Gold", "Rose Gold", "Charcoal Grey", "Matt Black", "Glossy White"),
                                             modifier = Modifier.weight(1f)
                                         )
                                     }
                                     SpecDropdownField(
                                         value = glassType,
                                         onValueChange = { glassType = it },
                                         label = "Glass Type (Optional)",
                                         options = listOf("None", "Clear Glass", "Frosted Glass", "Tinted Glass", "Fluted Glass")
                                     )
                                 }
                                 mLower.contains("glass") -> {
                                     Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                         SpecDropdownField(
                                             value = glassType,
                                             onValueChange = { glassType = it },
                                             label = "Glass Type *",
                                             options = listOf("Clear Glass", "Frosted Glass", "Tinted Glass", "Fluted Glass", "Lacquered Glass"),
                                             modifier = Modifier.weight(1f)
                                         )
                                         SpecDropdownField(
                                             value = glassThickness,
                                             onValueChange = { glassThickness = it },
                                             label = "Glass Thickness *",
                                             options = listOf("4 mm", "5 mm", "6 mm", "8 mm", "10 mm", "12 mm"),
                                             modifier = Modifier.weight(1f)
                                         )
                                     }
                                 }
                                 mLower.contains("acp") -> {
                                     SpecDropdownField(
                                         value = acpColour,
                                         onValueChange = { acpColour = it },
                                         label = "ACP Colour *",
                                         options = listOf("Pure White", "Silver Metallic", "Glossy Red", "Charcoal Grey", "Champagne Gold", "Wood Grain Black")
                                     )
                                 }
                                 mLower.contains("hardware") -> {
                                     Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                         OutlinedTextField(
                                             value = brand,
                                             onValueChange = { brand = it },
                                             label = { Text("Brand *") },
                                             modifier = Modifier.weight(1f),
                                             shape = RoundedCornerShape(12.dp)
                                         )
                                         OutlinedTextField(
                                             value = hardware,
                                             onValueChange = { hardware = it },
                                             label = { Text("Model *") },
                                             modifier = Modifier.weight(1f),
                                             shape = RoundedCornerShape(12.dp)
                                         )
                                     }
                                 }
                             }
                        }
                    }

                    // --- SECTION 3: DIMENSIONS & AREA ---
                    val showDim = isDimensionUnit(unit)
                    val showArea = isAreaUnit(unit)
                    if (showDim || showArea) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Dimensions (Feet)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (showDim) {
                                        // Width (Mandatory)
                                        OutlinedTextField(
                                            value = widthStr,
                                            onValueChange = { widthStr = it },
                                            label = { Text("Width *") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                    if (showArea) {
                                        // Height (Mandatory)
                                        OutlinedTextField(
                                            value = heightStr,
                                            onValueChange = { heightStr = it },
                                            label = { Text("Height *") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                    if (showDim) {
                                        // Depth (Optional)
                                        OutlinedTextField(
                                            value = depthStr,
                                            onValueChange = { depthStr = it },
                                            label = { Text("Depth") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }

                                if (showArea) {
                                    // Read-only Area Calculation Banner
                                    val w = widthStr.toDoubleOrNull() ?: 0.0
                                    val h = heightStr.toDoubleOrNull() ?: 0.0
                                    val calculatedArea = w * h
                                    val areaStr = String.format(Locale.US, "%.2f", calculatedArea)

                                    val qtyCountForPreview = quantityStr.toDoubleOrNull() ?: 1.0
                                    val calculatedQty = calculateQuantity(w, h, qtyCountForPreview, unit)
                                    val qtyFormatted = if (calculatedQty % 1.0 == 0.0) {
                                        String.format(Locale.US, "%.0f", calculatedQty)
                                    } else {
                                        String.format(Locale.US, "%.2f", calculatedQty)
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AspectRatio,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Calculated Area",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                    )
                                                    Text(
                                                        text = "$areaStr Sq.ft",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Total Qty ($unit)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = qtyFormatted,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- SECTION 4: PRICING & QUANTITY ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pricing", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            SpecDropdownField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = "Unit *",
                                options = listOf("Sq.Ft", "Nos", "Running Feet", "Sq.M", "Bundle", "Sheet", "Kg")
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Quantity (Mandatory)
                                OutlinedTextField(
                                    value = quantityStr,
                                    onValueChange = { quantityStr = it },
                                    label = { Text("Quantity *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Rate per unit (Mandatory)
                                OutlinedTextField(
                                    value = rateStr,
                                    onValueChange = { rateStr = it },
                                    label = { Text("Rate (₹/$unit) *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // --- SECTION 5: REFERENCE IMAGE ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reference Image (Optional)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clickable { designLauncher.launch("image/*") },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (designPath.isNotBlank()) {
                                        Box {
                                            AsyncImage(
                                                model = File(designPath),
                                                contentDescription = "Design Preview",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            IconButton(
                                                onClick = { designPath = "" },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Pick Reference Image", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Footer Save Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (itemName.isBlank()) {
                                Toast.makeText(context, "Please enter an Item Name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val isDim = isDimensionUnit(unit)
                            val isArea = isAreaUnit(unit)
                            
                            val widthVal = if (isDim) {
                                val w = widthStr.toDoubleOrNull()
                                if (w == null || w <= 0.0) {
                                    Toast.makeText(context, "Width is required and must be greater than 0 for $unit unit", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                w
                            } else {
                                1.0
                            }

                            val heightVal = if (isArea) {
                                val h = heightStr.toDoubleOrNull()
                                if (h == null || h <= 0.0) {
                                    Toast.makeText(context, "Height is required and must be greater than 0 for $unit unit", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                h
                            } else {
                                1.0
                            }

                            val qtyCount = quantityStr.toDoubleOrNull()
                            if (qtyCount == null || qtyCount <= 0.0) {
                                Toast.makeText(context, "Please enter a valid Quantity", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val rate = rateStr.toDoubleOrNull()
                            if (rate == null || rate <= 0.0) {
                                Toast.makeText(context, "Please enter a valid Rate", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Dynamic Material specific validations
                            val mLowerValid = material.lowercase(Locale.US)
                            when {
                                mLowerValid.contains("plywood") -> {
                                    if (thickness.isBlank() || grade.isBlank()) {
                                        Toast.makeText(context, "Thickness and Grade are required for Plywood", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                mLowerValid.contains("particle") -> {
                                    if (thickness.isBlank()) {
                                        Toast.makeText(context, "Thickness is required for Particle Board", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                mLowerValid.contains("mdf") -> {
                                    if (thickness.isBlank()) {
                                        Toast.makeText(context, "Thickness is required for MDF", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                mLowerValid.contains("hdhmr") -> {
                                    if (thickness.isBlank()) {
                                        Toast.makeText(context, "Thickness is required for HDHMR", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                mLowerValid.contains("wpc") -> {
                                    if (thickness.isBlank()) {
                                        Toast.makeText(context, "Thickness is required for WPC", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                mLowerValid.contains("pvc") -> {
                                    if (thickness.isBlank()) {
                                        Toast.makeText(context, "Thickness is required for PVC Board", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                mLowerValid.contains("aluminium") -> {
                                    if (profileSeries.isBlank() || profileColour.isBlank()) {
                                        Toast.makeText(context, "Profile Series and Colour are required for Aluminium", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                mLowerValid.contains("acp") -> {
                                    if (acpColour.isBlank()) {
                                        Toast.makeText(context, "ACP Colour is required for ACP", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                mLowerValid.contains("glass") -> {
                                    if (glassType.isBlank() || glassThickness.isBlank()) {
                                        Toast.makeText(context, "Glass Type and Thickness are required for Glass", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                                mLowerValid.contains("hardware") -> {
                                    if (brand.isBlank() || hardware.isBlank()) {
                                        Toast.makeText(context, "Brand and Model are required for Hardware", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }
                            }

                            // Use our precise calculateQuantity function
                            val qty = calculateQuantity(widthVal, heightVal, qtyCount, unit)

                            val specsObj = ItemSpecs(
                                width = if (isDim) widthStr else "",
                                height = if (isArea) heightStr else "",
                                depth = if (isDim) depthStr else "",
                                thickness = if (mLowerValid.contains("plywood") || mLowerValid.contains("wpc") || mLowerValid.contains("particle") || mLowerValid.contains("mdf") || mLowerValid.contains("hdhmr") || mLowerValid.contains("pvc")) thickness else "",
                                profileSeries = if (mLowerValid.contains("aluminium")) profileSeries else "",
                                profileColour = if (mLowerValid.contains("aluminium")) profileColour else "",
                                glassType = if (mLowerValid.contains("aluminium") || mLowerValid.contains("glass")) glassType else "",
                                glassThickness = if (mLowerValid.contains("glass")) glassThickness else "",
                                acpColour = if (mLowerValid.contains("acp")) acpColour else "",
                                designImageUri = designPath,
                                grade = if (mLowerValid.contains("plywood") || mLowerValid.contains("particle")) grade else "",
                                brand = if (mLowerValid.contains("hardware")) brand else "",
                                hardware = if (mLowerValid.contains("hardware")) hardware else ""
                            )

                            val serializedDesc = serializeItemSpecs(userDescription, specsObj)

                            val finalItem = QuotationItem(
                                id = if (itemIndex != null && itemIndex in currentItems.indices) currentItems[itemIndex].id else 0,
                                quotationId = if (itemIndex != null && itemIndex in currentItems.indices) currentItems[itemIndex].quotationId else 0,
                                itemName = itemName,
                                description = serializedDesc,
                                material = material,
                                finish = "",
                                quantity = qty,
                                unit = unit,
                                rate = rate,
                                amount = qty * rate
                            )

                            onSave(finalItem)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Item")
                    }
                }
            }
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
    var expandedWarranty by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = {
                            discountStr = it
                            onDiscountChange(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Flat Discount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // GST Rate
                    OutlinedTextField(
                        value = gstRateStr,
                        onValueChange = {
                            gstRateStr = it
                            onGstChange(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("GST Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Warranty & Support Limits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Warranty dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = warranty.ifEmpty { "Select warranty limit" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Warranty") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedWarranty = true }
                    )
                    DropdownMenu(expanded = expandedWarranty, onDismissRequest = { expandedWarranty = false }) {
                        val displayWarranties = if (masterWarranties.isEmpty()) listOf("1 Year Warranty", "3 Years Warranty", "5 Years Warranty", "No Warranty") else masterWarranties
                        displayWarranties.forEach { w ->
                            DropdownMenuItem(text = { Text(w) }, onClick = {
                                onWarrantyChange(w)
                                expandedWarranty = false
                            })
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quotation Terms & Conditions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = terms,
                    onValueChange = onTermsChange,
                    label = { Text("Terms & Conditions") },
                    placeholder = { Text("Leave blank to use default Company Terms & Conditions") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

// --- STEP 2: REVIEW & SAVE ---
@Composable
fun WizardStepReview(
    quoteNumber: String,
    customerName: String,
    customerPhone: String,
    customerAddress: String,
    siteLocation: String,
    itemsCount: Int,
    subtotal: Double,
    discount: Double,
    gstRate: Double,
    gstAmount: Double,
    grandTotal: Double,
    terms: String,
    warranty: String,
    onDiscountChange: (Double) -> Unit,
    onGstRateChange: (Double) -> Unit,
    onTermsChange: (String) -> Unit,
    onWarrantyChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var discountStr by remember { mutableStateOf(if (discount > 0) discount.toString() else "") }
    var gstRateStr by remember { mutableStateOf(gstRate.toString()) }
    var expandedWarranty by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Review Hero Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Review Quotation Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Verify all specs and numbers before saving",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Customer & Location details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Customer & Location details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ReviewRow(label = "Quotation Number", value = quoteNumber, isPrimary = true)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                ReviewRow(label = "Customer Name", value = customerName)
                ReviewRow(label = "Customer Phone", value = customerPhone)
                if (siteLocation.isNotBlank()) {
                    ReviewRow(label = "Site Name", value = siteLocation)
                }
                if (customerAddress.isNotBlank()) {
                    ReviewRow(label = "Site Address", value = customerAddress)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ReviewRow(label = "Total Configured Items", value = "$itemsCount item(s)", isPrimary = true)
            }
        }

        // Taxes & Discounts section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Discount & Taxes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Flat Discount
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = {
                            discountStr = it
                            onDiscountChange(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Flat Discount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // GST Rate
                    OutlinedTextField(
                        value = gstRateStr,
                        onValueChange = {
                            gstRateStr = it
                            onGstRateChange(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("GST Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Warranty dropdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Warranty & Support Limits",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = warranty.ifEmpty { "Select warranty limit" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Warranty") },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedWarranty = true }
                    )
                    DropdownMenu(
                        expanded = expandedWarranty,
                        onDismissRequest = { expandedWarranty = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        listOf("1 Year Warranty", "3 Years Warranty", "5 Years Warranty", "10 Years Warranty", "No Warranty").forEach { w ->
                            DropdownMenuItem(text = { Text(w) }, onClick = {
                                onWarrantyChange(w)
                                expandedWarranty = false
                            })
                        }
                    }
                }
            }
        }

        // Terms & Conditions text area
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quotation Terms & Conditions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                OutlinedTextField(
                    value = terms,
                    onValueChange = onTermsChange,
                    label = { Text("Terms & Conditions") },
                    placeholder = { Text("Leave blank to use default Company Terms & Conditions") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Financials Review
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pricing Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ReviewRow(label = "Subtotal", value = "₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(subtotal)}")
                
                if (discount > 0) {
                    ReviewRow(
                        label = "Flat Discount", 
                        value = "-₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(discount)}", 
                        valueColor = MaterialTheme.colorScheme.error
                    )
                }
                
                if (gstAmount > 0) {
                    ReviewRow(label = "GST ($gstRate%)", value = "₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(gstAmount)}")
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grand Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "₹${com.example.utils.CurrencyFormatter.formatIndianCurrency(grandTotal)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Save Action Button
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save & Generate Quotation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ReviewRow(
    label: String, 
    value: String, 
    isPrimary: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isPrimary) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}

// --- SMART SUGGESTIONS DIALOG ---
@Composable
fun SmartSuggestionsDialog(
    selectedProduct: String,
    selectedMaterial: String,
    selectedFinish: String,
    specificationSummary: String = "",
    onAddItems: (List<QuotationItem>) -> Unit,
    onDismiss: () -> Unit
) {
    data class SuggestionTemplate(
        val name: String,
        val desc: String,
        val unit: String,
        val baseQty: Double,
        val rate: Double
    )

    val suggestions = remember(selectedProduct, selectedMaterial, selectedFinish, specificationSummary) {
        val prod = selectedProduct.lowercase()
        val mat = selectedMaterial
        val fin = selectedFinish
        
        val widthVal = parseFieldFromSummary(specificationSummary, "Width").replace("Ft", "").trim().toDoubleOrNull()
        val heightVal = parseFieldFromSummary(specificationSummary, "Height").replace("Ft", "").trim().toDoubleOrNull()
        val qtyMultiplier = parseFieldFromSummary(specificationSummary, "Quantity").toDoubleOrNull() ?: 1.0

        fun computeQty(baseQty: Double, unit: String, itemName: String): Double {
            val uLower = unit.lowercase()
            val nameLower = itemName.lowercase()
            val computed = when {
                uLower.contains("sq") -> {
                    val w = widthVal ?: when {
                        prod.contains("wardrobe") -> 5.0
                        prod.contains("kitchen") -> 10.0
                        prod.contains("tv") || prod.contains("unit") -> 6.0
                        prod.contains("crockery") -> 4.0
                        prod.contains("vanity") -> 3.0
                        prod.contains("partition") -> 6.0
                        prod.contains("loft") -> 12.0
                        else -> 6.0
                    }
                    val h = heightVal ?: when {
                        prod.contains("wardrobe") -> 8.0
                        prod.contains("tv") || prod.contains("unit") -> 5.0
                        prod.contains("partition") -> 8.0
                        else -> 1.0
                    }
                    if (nameLower.contains("shelving") || nameLower.contains("shelves")) {
                        w * h * 0.3 * qtyMultiplier
                    } else {
                        w * h * qtyMultiplier
                    }
                }
                uLower.contains("rft") || uLower.contains("running") -> {
                    val w = widthVal ?: when {
                        prod.contains("wardrobe") -> 5.0
                        prod.contains("kitchen") -> 10.0
                        prod.contains("tv") || prod.contains("unit") -> 6.0
                        prod.contains("crockery") -> 4.0
                        prod.contains("vanity") -> 3.0
                        prod.contains("partition") -> 6.0
                        prod.contains("loft") -> 12.0
                        else -> 6.0
                    }
                    w * qtyMultiplier
                }
                else -> {
                    baseQty * qtyMultiplier
                }
            }
            return Math.round(computed * 100.0) / 100.0
        }

        val templates = when {
            prod.contains("wardrobe") -> listOf(
                SuggestionTemplate("Carcass (18mm)", "Standard wardrobe carcass", "Sq.Ft", 40.0, 1100.0),
                SuggestionTemplate("Shutters / Doors", "Wardrobe shutters with handles", "Sq.Ft", 40.0, 850.0),
                SuggestionTemplate("Internal Shelving", "Internal partitioning shelves", "Sq.Ft", 12.0, 350.0),
                SuggestionTemplate("Hanging Rod", "Aluminium oval hanging rod", "Nos", 2.0, 250.0),
                SuggestionTemplate("Soft Close Hinges", "Hafele/Hettich hinges pack", "Nos", 8.0, 180.0),
                SuggestionTemplate("Drawer Channels", "Telescopic drawer channels", "Sets", 3.0, 450.0),
                SuggestionTemplate("Loft Units", "Top loft storage units", "Nos", 1.0, 4500.0)
            )
            prod.contains("kitchen") -> listOf(
                SuggestionTemplate("Base Cabinets Carcass", "Waterproof plywood/Aluminium base units", "Rft (Running Foot)", 10.0, 1850.0),
                SuggestionTemplate("Wall Cabinets Carcass", "Top wall storage units", "Rft (Running Foot)", 10.0, 1250.0),
                SuggestionTemplate("Kitchen Shutters", "Postform/Acrylic finished shutters", "Rft (Running Foot)", 10.0, 950.0),
                SuggestionTemplate("Modular Baskets Pack", "Stainless steel pullout baskets", "Sets", 1.0, 12500.0),
                SuggestionTemplate("Pneumatic Lift-ups", "For wall cabinet lift doors", "Nos", 2.0, 650.0),
                SuggestionTemplate("Profile Handles", "Gola profile handle system", "Meters", 20.0, 220.0)
            )
            prod.contains("tv") || prod.contains("unit") -> listOf(
                SuggestionTemplate("Back Panel Ply/Mica", "Veneer/Laminate decorative backing", "Sq.Ft", 32.0, 450.0),
                SuggestionTemplate("Base Drawer Console", "Low-height drawer units", "Rft (Running Foot)", 6.0, 1200.0),
                SuggestionTemplate("Glass Shelves", "Floating glass/ply shelves", "Pcs", 2.0, 800.0),
                SuggestionTemplate("LED Profiling", "Warm-white backlight strip & profile", "Sets", 1.0, 2500.0)
            )
            prod.contains("crockery") -> listOf(
                SuggestionTemplate("Carcass Cabinets", "Core framework storage", "Nos", 1.0, 8500.0),
                SuggestionTemplate("Fluted Glass Shutters", "Profile glass shutters", "Nos", 2.0, 2800.0),
                SuggestionTemplate("Spot Lights", "Warm led spotlight inserts", "Nos", 4.0, 350.0)
            )
            prod.contains("vanity") -> listOf(
                SuggestionTemplate("Vanity Under-Sink Cabinet", "Moisture resistant carcass", "Nos", 1.0, 5500.0),
                SuggestionTemplate("Mirror Frame Unit", "Matching finish wood frame mirror", "Nos", 1.0, 2200.0)
            )
            prod.contains("partition") -> listOf(
                SuggestionTemplate("Partition Main Framework", "Hardwood/Aluminium room divider frame", "Sq.Ft", 50.0, 380.0),
                SuggestionTemplate("Glass/Laminate Inserts", "Decorative panel inserts", "Sq.Ft", 50.0, 250.0)
            )
            prod.contains("loft") -> listOf(
                SuggestionTemplate("Loft Framing", "Internal frame support structure", "Rft (Running Foot)", 12.0, 450.0),
                SuggestionTemplate("Loft Shutters", "Hinged loft doors", "Rft (Running Foot)", 12.0, 650.0)
            )
            else -> listOf(
                SuggestionTemplate("Main Carcass / Body", "Basic structure of the unit", "Nos", 1.0, 15000.0),
                SuggestionTemplate("Front Shutters", "Front face doors and hinges", "Nos", 1.0, 8000.0),
                SuggestionTemplate("Premium Fitting Hardware", "Essential handles, hinges, screws", "Sets", 1.0, 3500.0)
            )
        }

        templates.map { t ->
            val computedQuantity = computeQty(t.baseQty, t.unit, t.name)
            QuotationItem(
                id = 0,
                quotationId = 0,
                itemName = t.name,
                description = serializeItemSpecs(t.desc, ItemSpecs()),
                material = mat,
                finish = fin,
                quantity = computedQuantity,
                unit = t.unit,
                rate = t.rate,
                amount = computedQuantity * t.rate
            )
        }
    }

    // Keep track of which items are selected
    val selectedItemsMap = remember { 
        mutableStateMapOf<Int, Boolean>()
    }

    // Customized quantities and rates
    val qtyMap = remember {
        mutableStateMapOf<Int, String>()
    }
    val rateMap = remember {
        mutableStateMapOf<Int, String>()
    }

    LaunchedEffect(suggestions) {
        selectedItemsMap.clear()
        qtyMap.clear()
        rateMap.clear()
        suggestions.forEachIndexed { idx, item ->
            selectedItemsMap[idx] = true
            qtyMap[idx] = item.quantity.toString()
            rateMap[idx] = item.rate.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Smart Item Suggestions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Standard item presets based on selected configuration. Check items to add and adjust Qty/Rate as needed:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions.size) { idx ->
                        val item = suggestions[idx]
                        val isSelected = selectedItemsMap[idx] ?: false
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) 
                                                 else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { selectedItemsMap[idx] = it }
                                )
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    // Extract description
                                    val (cleanDesc, _) = parseItemSpecs(item.description)
                                    Text(cleanDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = qtyMap[idx] ?: "",
                                                onValueChange = { qtyMap[idx] = it },
                                                label = { Text("Qty", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                            OutlinedTextField(
                                                value = rateMap[idx] ?: "",
                                                onValueChange = { rateMap[idx] = it },
                                                label = { Text("Rate", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1.5f),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalItems = mutableListOf<QuotationItem>()
                    suggestions.forEachIndexed { idx, item ->
                        if (selectedItemsMap[idx] == true) {
                            val qty = qtyMap[idx]?.toDoubleOrNull() ?: item.quantity
                            val rate = rateMap[idx]?.toDoubleOrNull() ?: item.rate
                            finalItems.add(
                                item.copy(
                                    quantity = qty,
                                    rate = rate,
                                    amount = qty * rate
                                )
                            )
                        }
                    }
                    onAddItems(finalItems)
                    onDismiss()
                }
            ) {
                Text("Add Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
