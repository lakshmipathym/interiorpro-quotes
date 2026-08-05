/**
 * Security.gs
 * Security, Request Validation, Signature & Response Helpers for InteriorPro ERP
 */

/**
 * Validates timestamp drift
 */
function validateTimestamp(timestamp) {
  if (!timestamp) return false;
  var requestTime = Number(timestamp);
  var currentTime = new Date().getTime();
  var diff = Math.abs(currentTime - requestTime);
  return diff <= CONFIG.TIMESTAMP_MAX_DRIFT_MS;
}

/**
 * Computes SHA-256 HMAC signature
 */
function computeSignature(payloadString, secret) {
  var key = secret || getScriptSecret();
  var signatureBytes = Utilities.computeHmacSha256Signature(payloadString, key);
  return signatureBytes.map(function(byte) {
    var v = (byte < 0 ? byte + 256 : byte).toString(16);
    return v.length === 1 ? "0" + v : v;
  }).join("");
}

/**
 * Validates signature against payload string
 */
function validateSignature(payloadString, providedSignature) {
  if (!providedSignature) return false;
  var expectedSignature = computeSignature(payloadString, getScriptSecret());
  return expectedSignature.toLowerCase() === String(providedSignature).toLowerCase();
}

/**
 * Creates standardized JSON response object
 */
function createJsonResponse(status, code, message, data) {
  var responseObj = {
    status: status,
    code: code || (status === "SUCCESS" ? 200 : 400),
    serverTime: new Date().toISOString(),
    apiVersion: CONFIG.API_VERSION,
    message: message || "",
    data: data || null
  };

  return ContentService.createTextOutput(JSON.stringify(responseObj))
    .setMimeType(ContentService.MimeType.JSON);
}

/**
 * Creates structured error response
 */
function createErrorResponse(code, message) {
  return createJsonResponse("ERROR", code, message, null);
}
