package com.example.core.backup.sandbox

import java.io.File

data class SandboxSession(
    val sessionId: String,
    val workspaceDir: File,
    val creationTime: Long
)
