package com.example.core.backup.sandbox

import com.example.core.backup.pkg.BackupPackage

interface RestoreSandboxRepository {
    fun createSandboxWorkspace(): SandboxSession
    fun extractAndDecrypt(
        session: SandboxSession,
        encryptedPayload: ByteArray,
        password: String
    ): ByteArray
    fun destroySandbox(session: SandboxSession)
    fun verifySchema(decryptedPayload: ByteArray): Boolean
}
