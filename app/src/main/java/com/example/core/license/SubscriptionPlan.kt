package com.example.core.license

enum class SubscriptionPlan(
    val planCode: String,
    val displayName: String,
    val durationDays: Long,
    val priceDisplay: String,
    val description: String
) {
    TRIAL_30_DAYS(
        planCode = "TRIAL_30_DAYS",
        displayName = "30-Day Free Trial",
        durationDays = 30L,
        priceDisplay = "Free",
        description = "Full feature access during 30 days evaluation period"
    ),
    COMMERCIAL_MONTHLY(
        planCode = "COMMERCIAL_MONTHLY",
        displayName = "Commercial Monthly",
        durationDays = 30L,
        priceDisplay = "$29 / month",
        description = "Flexible monthly subscription, cancel anytime"
    ),
    COMMERCIAL_ANNUAL(
        planCode = "COMMERCIAL_ANNUAL",
        displayName = "Commercial Annual",
        durationDays = 365L,
        priceDisplay = "$249 / year",
        description = "Annual license for active businesses - Save 30%"
    ),
    COMMERCIAL_LIFETIME(
        planCode = "COMMERCIAL_LIFETIME",
        displayName = "Commercial Lifetime",
        durationDays = 36500L,
        priceDisplay = "$699 One-Time",
        description = "Perpetual lifetime license with priority updates"
    );

    companion object {
        fun fromCode(code: String?): SubscriptionPlan {
            if (code.isNullOrBlank()) return TRIAL_30_DAYS
            return values().firstOrNull {
                it.planCode.equals(code, ignoreCase = true) ||
                it.name.equals(code, ignoreCase = true)
            } ?: if (code.contains("MONTH", ignoreCase = true)) {
                COMMERCIAL_MONTHLY
            } else if (code.contains("ANNUAL", ignoreCase = true) || code.contains("YEAR", ignoreCase = true)) {
                COMMERCIAL_ANNUAL
            } else if (code.contains("LIFE", ignoreCase = true)) {
                COMMERCIAL_LIFETIME
            } else {
                TRIAL_30_DAYS
            }
        }
    }
}
