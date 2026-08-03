package com.financetracker.evolva.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.ByteArrayOutputStream
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Manual Google Drive backup/restore using the private [appDataFolder].
 * Requires an Android OAuth client in Google Cloud Console for
 * package [com.financetracker.evolva] + the installer's SHA-1, with the
 * Drive API enabled and `drive.appdata` scope allowed.
 */
class DriveBackupClient(private val appContext: Context) {

    fun signInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(appContext, options)
    }

    fun signInIntent(): Intent = signInClient().signInIntent

    fun currentAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(appContext)
            ?.takeIf { GoogleSignIn.hasPermissions(it, Scope(DriveScopes.DRIVE_APPDATA)) }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        runCatching { signInClient().signOut().await() }
        Unit
    }

    suspend fun uploadBackup(profileId: String, json: String): Result<DriveBackupMeta> =
        withContext(Dispatchers.IO) {
            runCatching {
                val drive = driveService()
                val fileName = backupFileName(profileId)
                val existingId = findBackupFileId(drive, fileName)
                val content = ByteArrayContent.fromString("application/json", json)
                val file = if (existingId != null) {
                    drive.files().update(existingId, null, content)
                        .setFields("id, name, modifiedTime")
                        .execute()
                } else {
                    val metadata = File().apply {
                        name = fileName
                        parents = Collections.singletonList("appDataFolder")
                    }
                    drive.files().create(metadata, content)
                        .setFields("id, name, modifiedTime")
                        .execute()
                }
                DriveBackupMeta(
                    fileId = file.id,
                    fileName = file.name,
                    modifiedTime = file.modifiedTime?.toString()
                )
            }
        }

    suspend fun downloadBackup(profileId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val drive = driveService()
            val fileId = findBackupFileId(drive, backupFileName(profileId))
                ?: error("NO_BACKUP")
            val out = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(out)
            out.toString(Charsets.UTF_8.name())
        }
    }

    suspend fun backupMeta(profileId: String): DriveBackupMeta? = withContext(Dispatchers.IO) {
        runCatching {
            val drive = driveService()
            val fileName = backupFileName(profileId)
            val files = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setFields("files(id, name, modifiedTime)")
                .setPageSize(1)
                .execute()
                .files
            val file = files?.firstOrNull() ?: return@runCatching null
            DriveBackupMeta(
                fileId = file.id,
                fileName = file.name,
                modifiedTime = file.modifiedTime?.toString()
            )
        }.getOrNull()
    }

    private fun backupFileName(profileId: String): String =
        "evolva-backup-$profileId.json"

    private fun findBackupFileId(drive: Drive, fileName: String): String? {
        val files = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$fileName' and trashed = false")
            .setFields("files(id)")
            .setPageSize(5)
            .execute()
            .files
        return files?.firstOrNull()?.id
    }

    private fun driveService(): Drive {
        val account = currentAccount()
            ?: error("NOT_SIGNED_IN")
        val credential = GoogleAccountCredential.usingOAuth2(
            appContext,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Finance Tracker eVolva").build()
    }
}

data class DriveBackupMeta(
    val fileId: String,
    val fileName: String,
    val modifiedTime: String?
)
