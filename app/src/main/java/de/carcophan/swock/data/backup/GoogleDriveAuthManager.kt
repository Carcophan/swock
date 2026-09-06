package de.carcophan.swock.data.backup

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveAuthManager(private val context: Context) {

    companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        val SCOPE_DRIVE = Scope(DRIVE_FILE_SCOPE)
    }

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(SCOPE_DRIVE)
        .build()

    val client: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    fun getSignInIntent(): Intent {
        return client.signInIntent
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && GoogleSignIn.hasPermissions(account, SCOPE_DRIVE)) {
            account
        } else {
            null
        }
    }

    fun parseSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null && GoogleSignIn.hasPermissions(account, SCOPE_DRIVE)) {
                Result.success(account)
            } else if (account != null) {
                Result.failure(IllegalStateException("Berechtigung für Google Drive wurde nicht erteilt."))
            } else {
                Result.failure(IllegalStateException("Google-Anmeldung fehlgeschlagen."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAccessToken(account: Account): Result<String> = withContext(Dispatchers.IO) {
        try {
            val scopeString = "oauth2:$DRIVE_FILE_SCOPE"
            val token = GoogleAuthUtil.getToken(context, account, scopeString)
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearToken(token: String) {
        try {
            GoogleAuthUtil.clearToken(context, token)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        client.signOut().addOnCompleteListener {
            onComplete()
        }
    }
}
