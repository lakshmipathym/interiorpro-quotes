package com.example.core.backup.preview

data class RestoreDifference(
    val recordsToAdd: Int,
    val recordsToUpdate: Int,
    val recordsToReplace: Int,
    val potentialConflicts: Int
)
