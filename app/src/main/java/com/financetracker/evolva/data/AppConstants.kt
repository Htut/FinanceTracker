package com.financetracker.evolva.data

import java.time.LocalDate

/**
 * App-wide constants shown in About and similar surfaces.
 * Keep [APP_VERSION] in sync with `versionName` in `app/build.gradle.kts`.
 */
object AppConstants {
    const val APP_VERSION = "1.0.0-beta"

    /**
     * Google Drive appdata backup UI / client calls.
     * Keep false until OAuth client + SHA-1 are provisioned (see deferred-features rule).
     * Local JSON/CSV export-import remains the supported backup path.
     */
    const val ENABLE_GOOGLE_DRIVE_BACKUP = false

    /** Inclusive expiry date for this beta build (ISO-8601). */
    const val BETA_EXPIRES_ON = "2027-07-31"

    val betaExpiresOn: LocalDate = LocalDate.parse(BETA_EXPIRES_ON)

    /** Default seconds before the date filter bar auto-collapses. 0 = never. */
    const val DEFAULT_FILTER_AUTO_CLOSE_SECONDS = 7

    /** True when device local date is on/after [betaExpiresOn]. */
    fun isBetaExpired(today: LocalDate = LocalDate.now()): Boolean =
        !today.isBefore(betaExpiresOn)
}
