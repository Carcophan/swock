package de.carcophan.swock.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.carcophan.swock.data.preferences.UserProfileRepository
import de.carcophan.swock.data.repository.MealRepository
import java.io.File
import java.util.concurrent.TimeUnit

class PeriodicBackupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val profileRepo = UserProfileRepository(context)
        val profile = profileRepo.profile.value

        if (!profile.autoBackupEnabled) {
            return Result.success()
        }

        val authManager = GoogleDriveAuthManager(context)
        val signedInAccount = authManager.getSignedInAccount() ?: return Result.failure()

        val tokenResult = authManager.getAccessToken(signedInAccount.account ?: return Result.failure())
        val token = tokenResult.getOrNull() ?: return Result.retry()

        val mealRepo = MealRepository(context)
        val backupManager = BackupManager(context, mealRepo, profileRepo)
        val driveService = GoogleDriveService()

        val tempZip = File(context.cacheDir, backupManager.generateBackupFileName())
        try {
            // 1. Create ZIP
            val backupInfoRes = backupManager.createBackupZip(tempZip)
            if (backupInfoRes.isFailure) {
                return Result.retry()
            }

            // 2. Locate or create backup folder
            val folderRes = driveService.getOrCreateBackupFolder(token)
            val folderId = folderRes.getOrNull() ?: return Result.retry()

            // 3. Upload backup
            val uploadRes = driveService.uploadBackup(
                token = token,
                folderId = folderId,
                zipFile = tempZip,
                fileName = tempZip.name
            )
            if (uploadRes.isFailure) {
                return Result.retry()
            }

            // 4. Prune older backups, keeping last 5
            driveService.pruneOldBackups(token, folderId, keepCount = 5)

            // 5. Update timestamp
            profileRepo.updateLastBackupTimestamp(System.currentTimeMillis())

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        } finally {
            if (tempZip.exists()) {
                tempZip.delete()
            }
        }
    }
}

object BackupScheduler {
    private const val UNIQUE_WORK_NAME = "swock_periodic_google_drive_backup"

    fun schedule(context: Context, intervalDays: Int, wifiOnly: Boolean) {
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()

        val interval = intervalDays.coerceAtLeast(1).toLong()
        val request = PeriodicWorkRequestBuilder<PeriodicBackupWorker>(interval, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
