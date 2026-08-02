package com.financetracker.evolva.data.profile

import kotlinx.serialization.Serializable

object ProfileIds {
    const val PERSONAL = "personal"
}

@Serializable
enum class ProfileKind {
    PERSONAL,
    TEMPLATE
}

@Serializable
data class TrackerProfile(
    val id: String,
    val kind: ProfileKind,
    /** Blueprint id from TEMPLATES when [kind] is TEMPLATE. */
    val templateId: String? = null,
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun personal(): TrackerProfile = TrackerProfile(
            id = ProfileIds.PERSONAL,
            kind = ProfileKind.PERSONAL,
            displayName = "My Tracker"
        )
    }
}
