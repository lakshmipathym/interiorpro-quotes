package com.example.core.backup.pkg

data class BackupPackage(
    val manifest: BackupManifest,
    val encryptedPayload: ByteArray,
    val checksum: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BackupPackage
        if (manifest != other.manifest) return false
        if (!encryptedPayload.contentEquals(other.encryptedPayload)) return false
        if (checksum != other.checksum) return false
        return true
    }

    override fun hashCode(): Int {
        var result = manifest.hashCode()
        result = 31 * result + encryptedPayload.contentHashCode()
        result = 31 * result + checksum.hashCode()
        return result
    }
}
