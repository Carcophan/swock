package de.carcophan.swock.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class DriveBackupFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdTime: Long
)

class GoogleDriveService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val BASE_DRIVE_URL = "https://www.googleapis.com/drive/v3"
        private const val BASE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3"
        const val BACKUP_FOLDER_NAME = "Swock Backups"
    }

    /**
     * Searches for the "Swock Backups" folder or creates it if it does not exist yet.
     */
    suspend fun getOrCreateBackupFolder(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val query = "mimeType = 'application/vnd.google-apps.folder' and name = '$BACKUP_FOLDER_NAME' and trashed = false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "$BASE_DRIVE_URL/files?q=$encodedQuery&spaces=drive&fields=files(id,name)"

            val searchReq = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(searchReq).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Fehler bei Ordnersuche: ${response.code} - $body"))
                }

                val json = JSONObject(body)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val folderId = files.getJSONObject(0).getString("id")
                    return@withContext Result.success(folderId)
                }
            }

            // Folder does not exist -> Create it
            val createUrl = "$BASE_DRIVE_URL/files"
            val folderMetadata = JSONObject().apply {
                put("name", BACKUP_FOLDER_NAME)
                put("mimeType", "application/vnd.google-apps.folder")
            }.toString()

            val createReq = Request.Builder()
                .url(createUrl)
                .addHeader("Authorization", "Bearer $token")
                .post(folderMetadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .build()

            client.newCall(createReq).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Fehler beim Erstellen des Backup-Ordners: ${response.code} - $body"))
                }
                val json = JSONObject(body)
                val folderId = json.getString("id")
                Result.success(folderId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads the ZIP backup to the given Google Drive folder via multipart upload.
     */
    suspend fun uploadBackup(
        token: String,
        folderId: String,
        zipFile: File,
        fileName: String
    ): Result<DriveBackupFile> = withContext(Dispatchers.IO) {
        try {
            val uploadUrl = "$BASE_UPLOAD_URL/files?uploadType=multipart&fields=id,name,size,createdTime"

            val metaJson = JSONObject().apply {
                put("name", fileName)
                put("parents", org.json.JSONArray().put(folderId))
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metaJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    fileName,
                    zipFile.asRequestBody("application/zip".toMediaType())
                )
                .build()

            val req = Request.Builder()
                .url(uploadUrl)
                .addHeader("Authorization", "Bearer $token")
                .post(multipartBody)
                .build()

            client.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Upload zu Google Drive fehlgeschlagen: ${response.code} - $body"))
                }

                val json = JSONObject(body)
                val id = json.getString("id")
                val name = json.optString("name", fileName)
                val size = json.optLong("size", zipFile.length())
                val createdTime = parseIso8601(json.optString("createdTime", ""))

                Result.success(DriveBackupFile(id, name, size, createdTime))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists all backup archives present in the Swock Google Drive folder, newest first.
     */
    suspend fun listBackups(token: String, folderId: String): Result<List<DriveBackupFile>> = withContext(Dispatchers.IO) {
        try {
            val query = "'$folderId' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val listUrl = "$BASE_DRIVE_URL/files?q=$encodedQuery&spaces=drive&fields=files(id,name,size,createdTime)&orderBy=createdTime desc"

            val req = Request.Builder()
                .url(listUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Backups konnten nicht abgerufen werden: ${response.code} - $body"))
                }

                val json = JSONObject(body)
                val filesArray = json.optJSONArray("files") ?: org.json.JSONArray()
                val list = mutableListOf<DriveBackupFile>()
                for (i in 0 until filesArray.length()) {
                    val f = filesArray.getJSONObject(i)
                    val id = f.getString("id")
                    val name = f.getString("name")
                    val size = f.optLong("size", 0L)
                    val createdTime = parseIso8601(f.optString("createdTime", ""))
                    list.add(DriveBackupFile(id, name, size, createdTime))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads a backup file by ID from Google Drive into a local destination file.
     */
    suspend fun downloadBackup(token: String, fileId: String, destFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val downloadUrl = "$BASE_DRIVE_URL/files/$fileId?alt=media"
            val req = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            if (destFile.parentFile?.exists() == false) {
                destFile.parentFile?.mkdirs()
            }

            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Download von Google Drive fehlgeschlagen: ${response.code}"))
                }

                val body = response.body ?: return@withContext Result.failure(IllegalStateException("Leere Antwort von Google Drive."))
                FileOutputStream(destFile).use { out ->
                    body.byteStream().copyTo(out)
                }
            }

            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a specific file from Google Drive.
     */
    suspend fun deleteBackup(token: String, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deleteUrl = "$BASE_DRIVE_URL/files/$fileId"
            val req = Request.Builder()
                .url(deleteUrl)
                .addHeader("Authorization", "Bearer $token")
                .delete()
                .build()

            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) {
                    return@withContext Result.failure(IllegalStateException("Löschen fehlgeschlagen: ${response.code}"))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Keeps the most recent [keepCount] backups in the Swock folder and deletes older ones.
     */
    suspend fun pruneOldBackups(token: String, folderId: String, keepCount: Int = 5): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val listRes = listBackups(token, folderId)
            val files = listRes.getOrNull() ?: return@withContext Result.success(0)

            var deleted = 0
            if (files.size > keepCount) {
                val toDelete = files.drop(keepCount)
                for (file in toDelete) {
                    deleteBackup(token, file.id)
                    deleted++
                }
            }
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseIso8601(isoString: String): Long {
        if (isoString.isBlank()) return System.currentTimeMillis()
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val fallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fallback.parse(isoString)?.time ?: System.currentTimeMillis()
            } catch (ex: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
