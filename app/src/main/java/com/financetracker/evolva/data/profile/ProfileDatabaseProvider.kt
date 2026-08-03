package com.financetracker.evolva.data.profile

import android.content.Context
import androidx.room.Room
import com.financetracker.evolva.data.db.FinanceDatabase
import java.io.File

/**
 * Opens and closes per-profile Room databases. Not a process-wide singleton —
 * [ActiveProfileSession] owns the open instance for the active profile.
 */
object ProfileDatabaseProvider {

    fun databaseName(profileId: String): String =
        if (profileId == ProfileIds.PERSONAL) {
            "finance-personal.db"
        } else {
            "finance-profile-$profileId.db"
        }

    fun open(context: Context, profileId: String): FinanceDatabase {
        migrateLegacyDatabaseIfNeeded(context)
        return Room.databaseBuilder(
            context.applicationContext,
            FinanceDatabase::class.java,
            databaseName(profileId)
        )
            .addMigrations(
                FinanceDatabase.MIGRATION_1_2,
                FinanceDatabase.MIGRATION_2_3,
                FinanceDatabase.MIGRATION_3_4
            )
            .build()
    }

    fun deleteDatabase(context: Context, profileId: String) {
        if (profileId == ProfileIds.PERSONAL) return
        val app = context.applicationContext
        val name = databaseName(profileId)
        app.deleteDatabase(name)
        // Extra safety for leftover journal files if deleteDatabase missed them.
        listOf(name, "$name-wal", "$name-shm").forEach { fileName ->
            File(app.getDatabasePath(fileName).absolutePath).delete()
        }
    }

    /**
     * First-launch migration: rename the pre-profiles DB to the personal profile DB.
     */
    fun migrateLegacyDatabaseIfNeeded(context: Context) {
        val app = context.applicationContext
        val legacy = app.getDatabasePath("finance-tracker.db")
        val personal = app.getDatabasePath(databaseName(ProfileIds.PERSONAL))
        if (!legacy.exists() || personal.exists()) return
        renameWithSidecars(legacy, personal)
    }

    private fun renameWithSidecars(from: File, to: File) {
        from.renameTo(to)
        File("${from.path}-wal").takeIf { it.exists() }?.renameTo(File("${to.path}-wal"))
        File("${from.path}-shm").takeIf { it.exists() }?.renameTo(File("${to.path}-shm"))
    }
}
