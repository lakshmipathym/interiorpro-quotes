package com.example.core.backup.sandbox

import java.io.File

data class SandboxDatabase(
    val databaseFile: File,
    val version: Int
)
