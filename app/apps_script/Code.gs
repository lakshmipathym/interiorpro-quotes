/**
 * Code.gs
 * InteriorPro ERP - Google Apps Script Backend Router & Entry Points
 *
 * Web App Deploy Instructions:
 * 1. Open Google Apps Script editor at https://script.google.com/
 * 2. Paste Config.gs, Sheets.gs, Security.gs, LicenseService.gs, and Code.gs
 * 3. Click "Deploy" -> "New deployment"
 * 4. Select type "Web app"
 * 5. Set "Execute as": "Me"
 * 6. Set "Who has access": "Anyone"
 * 7. Deploy and copy the Web App URL for Android Integration.
 */

/**
 * Entry point for HTTP GET requests
 */
function doGet(e) {
  try {
    var params = (e && e.parameter) ? e.parameter : {};
    var action = params.action || "health";

    return routeAction(action, params);
  } catch (err) {
    return createErrorResponse(500, "Internal Server Error: " + err.toString());
  }
}

/**
 * Entry point for HTTP POST requests
 */
function doPost(e) {
  try {
    var requestData = {};
    if (e && e.postData && e.postData.contents) {
      try {
        requestData = JSON.parse(e.postData.contents);
      } catch (jsonErr) {
        return createErrorResponse(400, "Invalid JSON body provided.");
      }
    }

    var params = (e && e.parameter) ? e.parameter : {};
    var action = requestData.action || params.action || "health";

    // Optional Security verification if timestamp & signature provided
    if (requestData.timestampMs) {
      if (!validateTimestamp(requestData.timestampMs)) {
        return createErrorResponse(401, "Request timestamp expired or signature invalid.");
      }
    }

    return routeAction(action, requestData);
  } catch (err) {
    return createErrorResponse(500, "Internal Server Error: " + err.toString());
  }
}

/**
 * Dispatches action to appropriate LicenseService handler
 */
function routeAction(action, data) {
  switch (String(action).toLowerCase()) {
    case "health":
      return handleHealth();
    case "register":
      return handleRegister(data);
    case "verify":
      return handleVerify(data);
    case "heartbeat":
      return handleHeartbeat(data);
    case "renew":
      return handleRenew(data);
    case "backupmetadata":
      return handleBackupMetadata(data);
    default:
      return createErrorResponse(404, "Unknown or unsupported action: " + action);
  }
}

/**
 * Optional manual test initializer to run inside Apps Script Editor
 */
function setupDatabaseSheets() {
  ensureSheetsAndHeaders();
  Logger.log("All sheets and headers initialized successfully.");
}
