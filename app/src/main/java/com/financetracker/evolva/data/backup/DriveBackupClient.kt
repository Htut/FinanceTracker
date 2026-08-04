package com.financetracker.evolva.data.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Manual Google Drive backup/restore using the private [appDataFolder].
 *
 * Auth uses the Identity [AuthorizationClient] (not deprecated GoogleSignIn).
 * Requires an Android OAuth client in Google Cloud Console for
 * package [com.financetracker.evolva] + the installer's SHA-1, with the
 * Drive API enabled and `drive.appdata` scope allowed.
 */
class DriveBackupClient(private val appContext: Context) {

    private val authClient get() = Identity.getAuthorizationClient(appContext)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedAccessToken: String? = null

    suspend fun authorize(): DriveAuthOutcome = withContext(Dispatchers.Main) {
        runCatching {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(
                    listOf(
                        Scope(DriveScopes.DRIVE_APPDATA),
                        Scope("email")
                    )
                )
                .build()
            val result = authClient.authorize(request).await()
            if (result.hasResolution()) {
                val pending = result.pendingIntent
                    ?: error("Missing authorization UI")
                DriveAuthOutcome.NeedsUi(pending)
            } else {
                val token = result.accessToken ?: error("Missing access token")
                cachedAccessToken = token
                DriveAuthOutcome.Ready(token, fetchEmail(token))
            }
        }.getOrElse { DriveAuthOutcome.Failed(it.message ?: "Authorization failed") }
    }

    suspend fun completeAuthorization(data: Intent?): DriveAuthOutcome =
        withContext(Dispatchers.Main) {
            runCatching {
                val result = authClient.getAuthorizationResultFromIntent(data)
                val token = result.accessToken ?: error("Missing access token")
                cachedAccessToken = token
                DriveAuthOutcome.Ready(token, fetchEmail(token))
            }.getOrElse { DriveAuthOutcome.Failed(it.message ?: "Authorization failed") }
        }

    /**
     * Tries to obtain a token without showing UI. Returns null if the user
     * must sign in / grant Drive access first.
     */
    suspend fun silentAccessToken(): String? = withContext(Dispatchers.Main) {
        runCatching {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(
                    listOf(
                        Scope(DriveScopes.DRIVE_APPDATA),
                        Scope("email")
                    )
                )
                .build()
            val result = authClient.authorize(request).await()
            if (result.hasResolution()) {
                null
            } else {
                result.accessToken?.also { cachedAccessToken = it }
            }
        }.getOrNull()
    }

    /** Returns email when a silent Drive session is already available. */
    suspend fun currentSessionEmail(): String? {
        val token = silentAccessToken() ?: return null
        return fetchEmail(token)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val token = cachedAccessToken
        cachedAccessToken = null
        if (token != null) {
            runCatching {
                authClient.clearToken(
                    ClearTokenRequest.builder()
                        .setToken(token)
                        .build()
                ).await()
            }
        }
        Unit
    }

    suspend fun uploadBackup(profileId: String, jsonBody: String): Result<DriveBackupMeta> =
        withContext(Dispatchers.IO) {
            runCatching {
                val drive = driveService(requireAccessToken())
                val fileName = backupFileName(profileId)
                val existingId = findBackupFileId(drive, fileName)
                val content = ByteArrayContent.fromString("application/json", jsonBody)
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
            val drive = driveService(requireAccessToken())
            val fileId = findBackupFileId(drive, backupFileName(profileId))
                ?: error("NO_BACKUP")
            val out = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(out)
            out.toString(Charsets.UTF_8.name())
        }
    }

    suspend fun backupMeta(profileId: String): DriveBackupMeta? = withContext(Dispatchers.IO) {
        runCatching {
            val drive = driveService(requireAccessToken())
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

    private suspend fun requireAccessToken(): String {
        cachedAccessToken?.let { return it }
        return silentAccessToken() ?: error("NOT_SIGNED_IN")
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

    private fun driveService(accessToken: String): Drive {
        val initializer = HttpRequestInitializer { request ->
            request.headers.authorization = "Bearer $accessToken"
        }
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            initializer
        ).setApplicationName("Finance Tracker eVolva").build()
    }

    private suspend fun fetchEmail(accessToken: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("https://www.googleapis.com/oauth2/v3/userinfo").openConnection()
                as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            conn.inputStream.bufferedReader().use { reader ->
                val body = reader.readText()
                json.parseToJsonElement(body).jsonObject["email"]?.jsonPrimitive?.content
            }
        }.getOrNull()
    }
}

sealed class DriveAuthOutcome {
    data class Ready(val accessToken: String, val email: String?) : DriveAuthOutcome()
    data class NeedsUi(val pendingIntent: PendingIntent) : DriveAuthOutcome()
    data class Failed(val message: String) : DriveAuthOutcome()
}

data class DriveBackupMeta(
    val fileId: String,
    val fileName: String,
    val modifiedTime: String?
)
