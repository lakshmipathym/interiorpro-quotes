package com.example.core.backup.pkg

import org.json.JSONObject
import org.json.JSONArray

data class BackupManifest(
    val metadata: BackupMetadata,
    val fileList: List<String>,
    val contentSummary: Map<String, Int>,
    val systemProperties: Map<String, String> = emptyMap()
) {
    fun toJsonObject(): JSONObject {
        val root = JSONObject()
        val meta = JSONObject()
        meta.put("backupId", metadata.backupId)
        meta.put("backupVersion", metadata.backupVersion)
        meta.put("createdDate", metadata.createdDate)
        meta.put("deviceName", metadata.deviceName)
        meta.put("appVersion", metadata.appVersion)
        meta.put("databaseVersion", metadata.databaseVersion)
        meta.put("compressionType", metadata.compressionType)
        meta.put("encryptionType", metadata.encryptionType)
        meta.put("checksumType", metadata.checksumType)
        
        val extraObj = JSONObject()
        metadata.extraMetadata.forEach { (k, v) -> extraObj.put(k, v) }
        meta.put("extraMetadata", extraObj)
        root.put("metadata", meta)

        val fileArray = JSONArray()
        fileList.forEach { fileArray.put(it) }
        root.put("fileList", fileArray)

        val summaryObj = JSONObject()
        contentSummary.forEach { (k, v) -> summaryObj.put(k, v) }
        root.put("contentSummary", summaryObj)

        val sysPropsObj = JSONObject()
        systemProperties.forEach { (k, v) -> sysPropsObj.put(k, v) }
        root.put("systemProperties", sysPropsObj)

        return root
    }
}
