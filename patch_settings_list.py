import re

with open("app/src/main/java/com/example/ui/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

# I will replace the entire SettingsMainList function block.
start_idx = content.find("fun SettingsMainList(")
if start_idx == -1:
    print("SettingsMainList not found")
    exit(1)

# Find end of function
brace_count = 0
end_idx = -1
in_func = False

for i in range(start_idx, len(content)):
    if content[i] == '{':
        in_func = True
        brace_count += 1
    elif content[i] == '}':
        if in_func:
            brace_count -= 1
            if brace_count == 0:
                end_idx = i
                break

if end_idx == -1:
    print("Could not find end of SettingsMainList")
    exit(1)

old_func = content[start_idx:end_idx+1]

new_func = """fun SettingsMainList(
    onNavigate: (String) -> Unit,
    themeManager: ThemeManager,
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
    pdfShowPageNumber: Boolean, onPdfShowPageNumberChange: (Boolean) -> Unit,
    onCreateWorkspace: () -> Unit,
    onImportWorkspace: () -> Unit,
    lastBackupDate: String,
    onNavigateToAbout: () -> Unit,
    onNavigateToSyncDashboard: () -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            SettingsSectionHeader("COMPANY")
            SettingsCard("Company Settings") {
                ListItem(
                    headlineContent = { Text("Company Profile") },
                    supportingContent = { Text("Business identity, contact, address & banking") },
                    leadingContent = { Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("company_profile") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ListItem(
                    headlineContent = { Text("Document Assets") },
                    supportingContent = { Text("Logo, Signature and Company Seal") },
                    leadingContent = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("document_assets") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        item {
            SettingsSectionHeader("QUOTATION")
            SettingsCard("Quotation Settings") {
                ListItem(
                    headlineContent = { Text("Default Parameters") },
                    supportingContent = { Text("GST, Payment Terms, Validity & Warranty") },
                    leadingContent = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("quotation_defaults") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ListItem(
                    headlineContent = { Text("PDF Configuration") },
                    supportingContent = { Text("Configure what details appear on PDFs") },
                    leadingContent = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("pdf_preferences") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        item {
            SettingsSectionHeader("MASTER DATA")
            SettingsCard("Database Masters") {
                ListItem(
                    headlineContent = { Text("Manage Masters") },
                    supportingContent = { Text("Edit materials, finishes, categories") },
                    leadingContent = { Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("manage_masters") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        item {
            SettingsSectionHeader("DATA")
            SettingsCard("Backup & Restore") {
                ListItem(
                    headlineContent = { Text("Export Workspace (Backup)") },
                    supportingContent = { Text("Export a local backup. Last: $lastBackupDate") },
                    leadingContent = { Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onCreateWorkspace() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ListItem(
                    headlineContent = { Text("Import Workspace (Restore)") },
                    supportingContent = { Text("Import a local backup from your device") },
                    leadingContent = { Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onImportWorkspace() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
        
        item {
            SettingsSectionHeader("SYNC")
            SettingsCard("Cloud Synchronization") {
                ListItem(
                    headlineContent = { Text("Cloud Sync & Backup History") },
                    supportingContent = { Text("Manage Google Drive cloud backups") },
                    leadingContent = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("cloud_backup") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ListItem(
                    headlineContent = { Text("Enterprise Sync Dashboard") },
                    supportingContent = { Text("Advanced workspace health and sync status") },
                    leadingContent = { Icon(Icons.Default.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToSyncDashboard() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        item {
            SettingsSectionHeader("APP")
            SettingsCard("App Preferences") {
                val isDark = themeManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM).value == ThemeMode.DARK
                ListItem(
                    headlineContent = { Text("Appearance") },
                    supportingContent = { Text("Configure display theme") },
                    leadingContent = { Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigate("appearance") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ListItem(
                    headlineContent = { Text("About Application") },
                    supportingContent = { Text("Version, developer and database info") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToAbout() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}"""

content = content.replace(old_func, new_func)

with open("app/src/main/java/com/example/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(content)
