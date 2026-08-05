/**
 * LicenseService.gs
 * Business logic implementation for InteriorPro ERP Commercial License Backend
 */

/**
 * Handles /health endpoint
 */
function handleHealth() {
  ensureSheetsAndHeaders();
  return createJsonResponse("SUCCESS", 200, "InteriorPro ERP Licensing Backend Service Active", {
    service: "InteriorPro ERP License API",
    status: "HEALTHY",
    timestampMs: new Date().getTime()
  });
}

/**
 * Handles /register endpoint
 * Request data: { deviceFingerprint, deviceId, workspaceId, installationId, email, customerName, licenseKey, phone }
 */
function handleRegister(data) {
  ensureSheetsAndHeaders();

  if (!data.deviceFingerprint || !data.workspaceId) {
    logAudit("REGISTER", "REJECTED", data.deviceFingerprint, data.workspaceId, data.email, "Missing deviceFingerprint or workspaceId");
    return createErrorResponse(400, "Device Fingerprint and Workspace ID are required for registration.");
  }

  // Check if device is already registered in License_Master
  var existingByFingerprint = findRowByValue(CONFIG.SHEET_NAMES.LICENSE_MASTER, 5, data.deviceFingerprint); // Col 5: DeviceFingerprint
  if (existingByFingerprint) {
    logAudit("REGISTER", "DUPLICATE", data.deviceFingerprint, data.workspaceId, data.email, "Device already registered");
    return createJsonResponse("SUCCESS", 200, "Device is already registered.", existingByFingerprint.rowObject);
  }

  // Generate or assign License Key
  var licenseKey = data.licenseKey || ("TRIAL-" + Utilities.getUuid().substring(0, 8).toUpperCase());
  var nowIso = new Date().toISOString();
  var expiryDate = new Date(new Date().getTime() + 30 * 24 * 60 * 60 * 1000).toISOString(); // 30 days trial

  var licenseRecord = {
    LicenseKey: licenseKey,
    CustomerName: data.customerName || "Trial User",
    Email: data.email || "",
    Phone: data.phone || "",
    DeviceFingerprint: data.deviceFingerprint,
    DeviceID: data.deviceId || "",
    WorkspaceID: data.workspaceId,
    InstallationID: data.installationId || "",
    Plan: data.licenseKey ? "COMMERCIAL_ANNUAL" : "TRIAL_30_DAYS",
    Status: "ACTIVE",
    ActivatedOn: nowIso,
    ExpiryDate: expiryDate,
    LastSeen: nowIso,
    Notes: "Registered via API"
  };

  appendRow(CONFIG.SHEET_NAMES.LICENSE_MASTER, licenseRecord);
  logAudit("REGISTER", "SUCCESS", data.deviceFingerprint, data.workspaceId, data.email, "Device registered successfully with key " + licenseKey);

  return createJsonResponse("SUCCESS", 201, "Device registration successful.", licenseRecord);
}

/**
 * Handles /verify endpoint
 * Request data: { deviceFingerprint, workspaceId, email, licenseKey }
 */
function handleVerify(data) {
  ensureSheetsAndHeaders();

  if (!data.deviceFingerprint || !data.workspaceId) {
    logAudit("VERIFY", "REJECTED", data.deviceFingerprint, data.workspaceId, data.email, "Missing parameters");
    return createErrorResponse(400, "Device Fingerprint and Workspace ID are required for verification.");
  }

  var record = findRowByValue(CONFIG.SHEET_NAMES.LICENSE_MASTER, 5, data.deviceFingerprint);
  if (!record) {
    logAudit("VERIFY", "NOT_FOUND", data.deviceFingerprint, data.workspaceId, data.email, "Unregistered device");
    return createErrorResponse(404, "Device not registered in License System.");
  }

  var rowObj = record.rowObject;

  // Validate Workspace ID match
  if (rowObj.WorkspaceID && rowObj.WorkspaceID !== data.workspaceId) {
    logAudit("VERIFY", "WORKSPACE_MISMATCH", data.deviceFingerprint, data.workspaceId, data.email, "Workspace mismatch: expected " + rowObj.WorkspaceID);
    return createErrorResponse(403, "Workspace ID mismatch for registered device.");
  }

  // Update LastSeen
  var nowIso = new Date().toISOString();
  updateRowColumns(CONFIG.SHEET_NAMES.LICENSE_MASTER, record.rowIndex, { LastSeen: nowIso });

  // Evaluate status and expiry
  var expiryMs = new Date(rowObj.ExpiryDate).getTime();
  var currentMs = new Date().getTime();
  var isExpired = currentMs > expiryMs;

  var statusStr = rowObj.Status;
  if (isExpired && statusStr === "ACTIVE") {
    var gracePeriodMs = CONFIG.GRACE_PERIOD_DAYS * 24 * 60 * 60 * 1000;
    if (currentMs <= expiryMs + gracePeriodMs) {
      statusStr = "GRACE_PERIOD";
    } else {
      statusStr = "EXPIRED";
    }
  }

  logAudit("VERIFY", "SUCCESS", data.deviceFingerprint, data.workspaceId, data.email, "Verification status: " + statusStr);

  return createJsonResponse("SUCCESS", 200, "License verification completed.", {
    licenseKey: rowObj.LicenseKey,
    plan: rowObj.Plan,
    status: statusStr,
    activatedOn: rowObj.ActivatedOn,
    expiryDate: rowObj.ExpiryDate,
    lastSeen: nowIso,
    remainingDays: Math.max(0, Math.ceil((expiryMs - currentMs) / (1000 * 60 * 60 * 24)))
  });
}

/**
 * Handles /heartbeat endpoint
 * Request data: { deviceFingerprint, workspaceId, email }
 */
function handleHeartbeat(data) {
  ensureSheetsAndHeaders();

  if (!data.deviceFingerprint) {
    return createErrorResponse(400, "Device Fingerprint required for heartbeat.");
  }

  var record = findRowByValue(CONFIG.SHEET_NAMES.LICENSE_MASTER, 5, data.deviceFingerprint);
  if (record) {
    var nowIso = new Date().toISOString();
    updateRowColumns(CONFIG.SHEET_NAMES.LICENSE_MASTER, record.rowIndex, { LastSeen: nowIso });
    logAudit("HEARTBEAT", "SUCCESS", data.deviceFingerprint, data.workspaceId, data.email, "Heartbeat updated");
    return createJsonResponse("SUCCESS", 200, "Heartbeat recorded.", { lastSeen: nowIso });
  }

  return createErrorResponse(404, "Device not found for heartbeat.");
}

/**
 * Handles /renew endpoint
 * Request data: { licenseKey, deviceFingerprint, workspaceId, extensionDays, amountPaid, transactionId, paymentMethod }
 */
function handleRenew(data) {
  ensureSheetsAndHeaders();

  if (!data.licenseKey || !data.deviceFingerprint) {
    logAudit("RENEW", "REJECTED", data.deviceFingerprint, data.workspaceId, data.email, "Missing licenseKey or deviceFingerprint");
    return createErrorResponse(400, "License Key and Device Fingerprint are required for renewal.");
  }

  var record = findRowByValue(CONFIG.SHEET_NAMES.LICENSE_MASTER, 1, data.licenseKey); // Col 1: LicenseKey
  if (!record) {
    logAudit("RENEW", "NOT_FOUND", data.deviceFingerprint, data.workspaceId, data.email, "Invalid License Key " + data.licenseKey);
    return createErrorResponse(404, "License Key not found.");
  }

  var extensionDays = data.extensionDays || 365; // Default 1 year
  var currentExpiry = new Date(record.rowObject.ExpiryDate).getTime();
  var baseTime = currentExpiry > new Date().getTime() ? currentExpiry : new Date().getTime();
  var newExpiryIso = new Date(baseTime + extensionDays * 24 * 60 * 60 * 1000).toISOString();
  var nowIso = new Date().toISOString();

  updateRowColumns(CONFIG.SHEET_NAMES.LICENSE_MASTER, record.rowIndex, {
    ExpiryDate: newExpiryIso,
    Status: "ACTIVE",
    LastSeen: nowIso
  });

  // Record transaction in Subscription_History
  var historyRecord = {
    TransactionID: data.transactionId || ("TXN-" + Utilities.getUuid().substring(0, 8).toUpperCase()),
    LicenseKey: data.licenseKey,
    WorkspaceID: data.workspaceId || record.rowObject.WorkspaceID,
    Plan: record.rowObject.Plan,
    AmountPaid: data.amountPaid || 0,
    PaymentMethod: data.paymentMethod || "MANUAL_RENEWAL",
    StartDate: nowIso,
    EndDate: newExpiryIso,
    CreatedOn: nowIso
  };
  appendRow(CONFIG.SHEET_NAMES.SUBSCRIPTION_HISTORY, historyRecord);

  logAudit("RENEW", "SUCCESS", data.deviceFingerprint, data.workspaceId, data.email, "License renewed until " + newExpiryIso);

  return createJsonResponse("SUCCESS", 200, "License renewal successful.", {
    licenseKey: data.licenseKey,
    newExpiryDate: newExpiryIso,
    status: "ACTIVE"
  });
}

/**
 * Handles /backupMetadata endpoint
 * Request data: { action: "save" | "query", workspaceId, deviceFingerprint, email, backupId, fileSizeBytes, driveFileId, checksum }
 */
function handleBackupMetadata(data) {
  ensureSheetsAndHeaders();

  var action = data.action || "save";

  if (action === "save") {
    if (!data.workspaceId || !data.backupId) {
      return createErrorResponse(400, "Workspace ID and Backup ID are required to save backup metadata.");
    }

    var nowIso = new Date().toISOString();
    var backupRecord = {
      BackupID: data.backupId,
      WorkspaceID: data.workspaceId,
      DeviceFingerprint: data.deviceFingerprint || "",
      Email: data.email || "",
      BackupDate: nowIso,
      FileSizeBytes: data.fileSizeBytes || 0,
      DriveFileID: data.driveFileId || "",
      Checksum: data.checksum || "",
      Status: "COMPLETED"
    };

    appendRow(CONFIG.SHEET_NAMES.BACKUP_METADATA, backupRecord);
    logAudit("BACKUP_METADATA", "SAVE", data.deviceFingerprint, data.workspaceId, data.email, "Saved backup " + data.backupId);

    return createJsonResponse("SUCCESS", 201, "Backup metadata saved successfully.", backupRecord);
  } else if (action === "query") {
    if (!data.workspaceId) {
      return createErrorResponse(400, "Workspace ID is required to query backup metadata.");
    }

    var record = findRowByValue(CONFIG.SHEET_NAMES.BACKUP_METADATA, 2, data.workspaceId); // Col 2: WorkspaceID
    if (!record) {
      return createJsonResponse("SUCCESS", 200, "No backup metadata found for workspace.", null);
    }

    return createJsonResponse("SUCCESS", 200, "Backup metadata retrieved.", record.rowObject);
  }

  return createErrorResponse(400, "Invalid backup metadata action.");
}
