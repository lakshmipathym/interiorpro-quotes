package com.example.core.backup.policy

enum class BackupTriggerType {
    MANUAL,
    ON_SAVE,
    SCHEDULED
}

interface BackupPolicy {
    val id: String
    val name: String
    val description: String
    val triggerType: BackupTriggerType
    
    /**
     * Evaluates if this policy should trigger a backup based on the current context maps.
     */
    fun shouldTrigger(context: Map<String, Any>): Boolean
}

class ManualBackupPolicy : BackupPolicy {
    override val id: String = "MANUAL"
    override val name: String = "Manual Backup"
    override val description: String = "Backups are triggered manually by the user."
    override val triggerType: BackupTriggerType = BackupTriggerType.MANUAL

    override fun shouldTrigger(context: Map<String, Any>): Boolean = false
}

class BackupAfterSavePolicy : BackupPolicy {
    override val id: String = "ON_SAVE"
    override val name: String = "Backup After Quotation Save"
    override val description: String = "Trigger a backup automatically whenever a quotation is saved."
    override val triggerType: BackupTriggerType = BackupTriggerType.ON_SAVE

    override fun shouldTrigger(context: Map<String, Any>): Boolean {
        return context["event"] == "QUOTATION_SAVED"
    }
}

abstract class ScheduledBackupPolicy(
    override val id: String,
    override val name: String,
    override val description: String,
    val intervalMillis: Long
) : BackupPolicy {
    override val triggerType: BackupTriggerType = BackupTriggerType.SCHEDULED

    override fun shouldTrigger(context: Map<String, Any>): Boolean {
        if (context["event"] != "TIME_CHECK") return false
        val lastBackupTime = context["lastBackupTime"] as? Long ?: return true
        val currentTime = context["currentTime"] as? Long ?: System.currentTimeMillis()
        return (currentTime - lastBackupTime) >= intervalMillis
    }
}

class DailyBackupPolicy : ScheduledBackupPolicy(
    id = "DAILY",
    name = "Daily Backup",
    description = "Trigger backup daily (every 24 hours).",
    intervalMillis = 24 * 60 * 60 * 1000L
)

class WeeklyBackupPolicy : ScheduledBackupPolicy(
    id = "WEEKLY",
    name = "Weekly Backup",
    description = "Trigger backup weekly (every 7 days).",
    intervalMillis = 7 * 24 * 60 * 60 * 1000L
)

class MonthlyBackupPolicy : ScheduledBackupPolicy(
    id = "MONTHLY",
    name = "Monthly Backup",
    description = "Trigger backup monthly (every 30 days).",
    intervalMillis = 30 * 24 * 60 * 60 * 1000L
)
