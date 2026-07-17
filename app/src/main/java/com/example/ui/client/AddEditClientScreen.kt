package com.example.ui.client

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditClientScreen(
    clientId: Long,
    clientViewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Screen title
    val isNew = clientId == 0L
    val screenTitle = if (isNew) "Register New Client" else "Edit Client Profile"

    // Form field states
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var siteLocation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }

    // Validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    // Holds original client reference for updates
    var originalClient by remember { mutableStateOf<Client?>(null) }
    var isLoading by remember { mutableStateOf(!isNew) }

    // Fetch existing client if editing
    LaunchedEffect(clientId) {
        if (!isNew) {
            isLoading = true
            val existing = clientViewModel.getClientById(clientId)
            if (existing != null) {
                originalClient = existing
                name = existing.clientName
                mobile = existing.mobileNumber
                email = existing.email
                address = existing.address
                siteLocation = existing.siteLocation
                notes = existing.notes
                isActive = existing.isActive
            } else {
                Toast.makeText(context, "Client not found", Toast.LENGTH_SHORT).show()
                onBack()
            }
            isLoading = false
        }
    }

    // Validation helper
    fun validateForm(): Boolean {
        var isValid = true

        if (name.isBlank()) {
            nameError = "Client Name is required"
            isValid = false
        } else {
            nameError = null
        }

        if (mobile.isBlank()) {
            mobileError = "Mobile Number is required"
            isValid = false
        } else if (mobile.length < 10) {
            mobileError = "Enter a valid 10-digit mobile number"
            isValid = false
        } else {
            mobileError = null
        }

        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            emailError = "Please enter a valid email address"
            isValid = false
        } else {
            emailError = null
        }

        return isValid
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("client_form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // INFO CARD: Basic Guidelines
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Fields marked with (*) are mandatory for registration.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // GROUP 1: Primary Contact Details
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Primary Contact Details",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Client Name Field
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    if (it.isNotBlank()) nameError = null
                                },
                                label = { Text("Client/Client Name *") },
                                isError = nameError != null,
                                supportingText = { nameError?.let { Text(it) } },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("client_form_name_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )

                            // Mobile Number Field
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = {
                                    mobile = it
                                    if (it.length >= 10) mobileError = null
                                },
                                label = { Text("Mobile Number *") },
                                isError = mobileError != null,
                                supportingText = { mobileError?.let { Text(it) } },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("client_form_mobile_field"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Email Address Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    emailError = null
                                },
                                label = { Text("Email Address") },
                                isError = emailError != null,
                                supportingText = { emailError?.let { Text(it) } },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("client_form_email_field"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // GROUP 2: Project & Site Details
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Project & Address Details",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Site Name / Location Field
                            OutlinedTextField(
                                value = siteLocation,
                                onValueChange = { siteLocation = it },
                                label = { Text("Site Name / Location") },
                                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("client_form_sitename_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )

                            // Address Field
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Billing / Full Address") },
                                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("client_form_address_field"),
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }
                    }

                    // GROUP 3: Project Notes & Status
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Administrative Details",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Notes Field
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Client/Project Notes") },
                                leadingIcon = { Icon(Icons.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("client_form_notes_field"),
                                maxLines = 5,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Done
                                )
                            )

                            // Active Switch (only relevant / useful for edits to deactivate, or toggle during add)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Client Account Status",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (isActive) "Active & available in directory" else "Inactive / Hidden from shortcuts",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Switch(
                                    checked = isActive,
                                    onCheckedChange = { isActive = it },
                                    modifier = Modifier.testTag("client_form_status_switch")
                                )
                            }
                        }
                    }

                    // Save / Register Button
                    Button(
                        onClick = {
                            if (validateForm()) {
                                val finalClient = (originalClient ?: Client()).copy(
                                    clientName = name.trim(),
                                    mobileNumber = mobile.trim(),
                                    email = email.trim(),
                                    address = address.trim(),
                                    siteLocation = siteLocation.trim(),
                                    notes = notes.trim(),
                                    isActive = isActive
                                )

                                scope.launch {
                                    // Direct Mobile Number duplicate check
                                    val dup = clientViewModel.getClientByMobile(mobile)
                                    if (dup != null && dup.clientId != finalClient.clientId) {
                                        Toast.makeText(context, "A client with this mobile number already exists: ${dup.clientName}", Toast.LENGTH_LONG).show()
                                    } else {
                                        if (isNew) {
                                            clientViewModel.saveClient(finalClient) {
                                                Toast.makeText(context, "Client registered successfully", Toast.LENGTH_SHORT).show()
                                                onBack()
                                            }
                                        } else {
                                            clientViewModel.updateClient(finalClient) {
                                                Toast.makeText(context, "Client profile updated", Toast.LENGTH_SHORT).show()
                                                onBack()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("client_form_submit_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isNew) Icons.Filled.HowToReg else Icons.Filled.Save,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNew) "Register Client" else "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
