package com.example.core.identity

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class WorkspaceIdentity(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("identity_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WORKSPACE_ID = "workspace_id"
    }

    fun getWorkspaceId(): String {
        var workspaceId = prefs.getString(KEY_WORKSPACE_ID, null)
        if (workspaceId.isNullOrEmpty()) {
            workspaceId = "WS-" + UUID.randomUUID().toString().take(12)
            prefs.edit().putString(KEY_WORKSPACE_ID, workspaceId).apply()
        }
        return workspaceId
    }
}
