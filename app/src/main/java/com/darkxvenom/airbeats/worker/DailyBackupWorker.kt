package com.darkxvenom.airbeats.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel
import kotlinx.coroutines.flow.first
import android.util.Log

class DailyBackupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val automaticCloudBackupEnabled = context
                .getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
                .getBoolean("enable_cloud_upload", true)

            if (!automaticCloudBackupEnabled) {
                Log.i("DailyBackupWorker", "Automatic cloud backup disabled. Skipping backup.")
                return Result.success()
            }

            val nameManager = NamePreferenceManager(context)
            val email = nameManager.accountEmail.first()
            val name = nameManager.userName.first().takeIf { it.isNotBlank() } ?: "AirBeats User"

            if (email.isNullOrBlank()) {
                Log.w("DailyBackupWorker", "No Google account linked. Skipping backup.")
                return Result.success()
            }

            Log.i("DailyBackupWorker", "Starting daily cloud backup for $email")
            val backupViewModel = BackupRestoreViewModel(com.darkxvenom.airbeats.db.InternalDatabase.newInstance(context))
            val result = backupViewModel.backupToDrive(context, email, name)

            return if (result is com.darkxvenom.airbeats.utils.DriveResult.Success) {
                Log.i("DailyBackupWorker", "Daily backup successful!")
                Result.success()
            } else {
                Log.e("DailyBackupWorker", "Daily backup failed")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("DailyBackupWorker", "Error during backup", e)
            return Result.retry()
        }
    }
}
