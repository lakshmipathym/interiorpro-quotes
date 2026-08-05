/**
 * Sheets.gs
 * Database helper layer using Google Sheets for InteriorPro ERP
 */

function getSpreadsheet() {
  if (CONFIG.SPREADSHEET_ID && CONFIG.SPREADSHEET_ID.trim() !== "") {
    return SpreadsheetApp.openById(CONFIG.SPREADSHEET_ID);
  }
  return SpreadsheetApp.getActiveSpreadsheet();
}

/**
 * Initializes all required sheets and headers if they do not exist
 */
function ensureSheetsAndHeaders() {
  var ss = getSpreadsheet();
  var sheetNames = CONFIG.SHEET_NAMES;
  var headers = CONFIG.HEADERS;

  for (var key in sheetNames) {
    var sheetName = sheetNames[key];
    var sheet = ss.getSheetByName(sheetName);
    if (!sheet) {
      sheet = ss.insertSheet(sheetName);
      var headerRow = headers[key];
      if (headerRow) {
        sheet.appendRow(headerRow);
        sheet.getRange(1, 1, 1, headerRow.length).setFontWeight("bold").setBackground("#E0E0E0");
        sheet.setFrozenRows(1);
      }
    }
  }
}

/**
 * Finds a row by matching a column value (1-indexed columnIndex)
 * Returns { rowIndex: number, rowData: Array, rowObject: Object } or null
 */
function findRowByValue(sheetName, columnIndex, searchVal) {
  var ss = getSpreadsheet();
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) return null;

  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) return null;

  var headers = data[0];
  for (var i = 1; i < data.length; i++) {
    if (String(data[i][columnIndex - 1]) === String(searchVal)) {
      var rowObj = {};
      for (var j = 0; j < headers.length; j++) {
        rowObj[headers[j]] = data[i][j];
      }
      return {
        rowIndex: i + 1, // 1-indexed row in sheet
        rowData: data[i],
        rowObject: rowObj
      };
    }
  }
  return null;
}

/**
 * Appends a row array or object to the specified sheet
 */
function appendRow(sheetName, rowData) {
  var ss = getSpreadsheet();
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    ensureSheetsAndHeaders();
    sheet = ss.getSheetByName(sheetName);
  }

  if (Array.isArray(rowData)) {
    sheet.appendRow(rowData);
  } else if (typeof rowData === "object") {
    var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
    var rowArray = [];
    for (var i = 0; i < headers.length; i++) {
      rowArray.push(rowData[headers[i]] !== undefined ? rowData[headers[i]] : "");
    }
    sheet.appendRow(rowArray);
  }
}

/**
 * Updates specific column cells in a given row
 */
function updateRowColumns(sheetName, rowIndex, updatesMap) {
  var ss = getSpreadsheet();
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) return;

  var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
  for (var key in updatesMap) {
    var colIndex = headers.indexOf(key);
    if (colIndex !== -1) {
      sheet.getRange(rowIndex, colIndex + 1).setValue(updatesMap[key]);
    }
  }
}

/**
 * Logs audit trail entry into Device_Audit_Log sheet
 */
function logAudit(action, status, deviceFingerprint, workspaceId, email, details) {
  var auditObj = {
    Timestamp: new Date().toISOString(),
    Action: action || "UNKNOWN",
    Status: status || "INFO",
    DeviceFingerprint: deviceFingerprint || "",
    WorkspaceID: workspaceId || "",
    Email: email || "",
    IPAddress: "CLIENT_DIRECT",
    Details: typeof details === "object" ? JSON.stringify(details) : String(details || "")
  };
  appendRow(CONFIG.SHEET_NAMES.DEVICE_AUDIT_LOG, auditObj);
}
