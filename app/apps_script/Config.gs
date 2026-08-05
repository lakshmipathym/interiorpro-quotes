/**
 * Config.gs
 * InteriorPro ERP - Google Apps Script Backend Configuration
 */

var CONFIG = {
  API_VERSION: "1.0",
  SPREADSHEET_ID: "", // Leave blank to use ActiveSpreadsheet when bound to Google Sheet, or set specific ID
  DEFAULT_SHARED_SECRET: "INTERIORPRO_ERP_SECRET_KEY_2026",
  TIMESTAMP_MAX_DRIFT_MS: 300000, // 5 minutes tolerance for request timestamps
  GRACE_PERIOD_DAYS: 7,
  
  SHEET_NAMES: {
    LICENSE_MASTER: "License_Master",
    DEVICE_AUDIT_LOG: "Device_Audit_Log",
    BACKUP_METADATA: "Backup_Metadata",
    SUBSCRIPTION_HISTORY: "Subscription_History"
  },
  
  HEADERS: {
    LICENSE_MASTER: [
      "LicenseKey", "CustomerName", "Email", "Phone",
      "DeviceFingerprint", "DeviceID", "WorkspaceID", "InstallationID",
      "Plan", "Status", "ActivatedOn", "ExpiryDate", "LastSeen", "Notes"
    ],
    DEVICE_AUDIT_LOG: [
      "Timestamp", "Action", "Status", "DeviceFingerprint",
      "WorkspaceID", "Email", "IPAddress", "Details"
    ],
    BACKUP_METADATA: [
      "BackupID", "WorkspaceID", "DeviceFingerprint", "Email",
      "BackupDate", "FileSizeBytes", "DriveFileID", "Checksum", "Status"
    ],
    SUBSCRIPTION_HISTORY: [
      "TransactionID", "LicenseKey", "WorkspaceID", "Plan",
      "AmountPaid", "PaymentMethod", "StartDate", "EndDate", "CreatedOn"
    ]
  }
};

/**
 * Returns shared secret dynamically from Script Properties or fallback
 */
function getScriptSecret() {
  try {
    var props = PropertiesService.getScriptProperties();
    var secret = props.getProperty("SHARED_SECRET");
    if (secret && secret.trim() !== "") {
      return secret.trim();
    }
  } catch (e) {
    // Ignore property fetch error in standalone mode
  }
  return CONFIG.DEFAULT_SHARED_SECRET;
}
