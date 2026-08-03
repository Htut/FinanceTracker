package com.financetracker.evolva.data

import java.time.LocalDate

/**
 * App-wide constants shown in About and similar surfaces.
 * Edit [APP_VERSION] here when releasing a new build label.
 */
object AppConstants {
    const val APP_VERSION = "beta 1.0.0"

    /** Inclusive expiry date for this beta build (ISO-8601). */
    const val BETA_EXPIRES_ON = "2027-07-31"

    val betaExpiresOn: LocalDate = LocalDate.parse(BETA_EXPIRES_ON)

    /** Default seconds before the date filter bar auto-collapses. 0 = never. */
    const val DEFAULT_FILTER_AUTO_CLOSE_SECONDS = 7

    /** True when device local date is on/after [betaExpiresOn]. */
    fun isBetaExpired(today: LocalDate = LocalDate.now()): Boolean =
        !today.isBefore(betaExpiresOn)
}
