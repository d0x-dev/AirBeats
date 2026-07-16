package com.darkxvenom.airbeats.utils

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class DriveResult<out T> {
    data class Success<out T>(val data: T) : DriveResult<T>()
    data class Error(val exception: Exception) : DriveResult<Nothing>()
    data class NeedsPermission(val intent: Intent) : DriveResult<Nothing>()
}

class GoogleAuthManager(private val context: Context) {
    
    companion object {
        const val WEB_CLIENT_ID = "83152931540-n3p9rbv3f41p5mhjbvk2fr0et09q44l7.apps.googleusercontent.com"
        const val BACKUP_FILE_NAME = "airbeats_backup.backup"
    }

    fun getSignInClient(): com.google.android.gms.auth.api.signin.GoogleSignInClient {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    }

    private fun getDriveService(email: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = Account(email, "com.google")
        
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("AirBeats").build()
    }

    suspend fun uploadBackupToDrive(email: String, backupFile: File): DriveResult<String> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(email)
            
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("nextPageToken, files(id, name)")
                .setQ("name='$BACKUP_FILE_NAME'")
                .execute()

            val fileContent = FileContent("application/octet-stream", backupFile)
            
            if (fileList.files.isNotEmpty()) {
                val existingFileId = fileList.files[0].id
                driveService.files().update(existingFileId, null, fileContent).execute()
                DriveResult.Success(existingFileId)
            } else {
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                val file = driveService.files().create(fileMetadata, fileContent)
                    .setFields("id")
                    .execute()
                DriveResult.Success(file.id)
            }
        } catch (e: UserRecoverableAuthIOException) {
            DriveResult.NeedsPermission(e.intent!!)
        } catch (e: Exception) {
            DriveResult.Error(e)
        }
    }

    suspend fun downloadBackupFromDrive(email: String, destFile: File): DriveResult<File> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(email)
            
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("nextPageToken, files(id, name)")
                .setQ("name='$BACKUP_FILE_NAME'")
                .execute()

            if (fileList.files.isEmpty()) {
                DriveResult.Error(Exception("No backup found on Google Drive"))
            } else {
                val fileId = fileList.files[0].id
                FileOutputStream(destFile).use { outStream ->
                    driveService.files().get(fileId).executeMediaAndDownloadTo(outStream)
                }
                DriveResult.Success(destFile)
            }
        } catch (e: UserRecoverableAuthIOException) {
            DriveResult.NeedsPermission(e.intent!!)
        } catch (e: Exception) {
            DriveResult.Error(e)
        }
    }
}
