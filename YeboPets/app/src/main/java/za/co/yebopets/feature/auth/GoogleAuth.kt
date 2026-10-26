/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Implement Sign in with Google
 * Type: [Source Code]
 * Available at: <https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Firebase
 * Date: 2026
 * Title: Authenticate with Google on Android
 * Type: [Source Code]
 * Available at: <https://firebase.google.com/docs/auth/android/google-signin> [Accessed 9 September 2026]
 **************************************************************************************************************/

package za.co.yebopets.feature.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import za.co.yebopets.BuildConfig

class GoogleAuth(private val activity: Activity) {
    private val credentials = CredentialManager.create(activity)
    private val firebase = FirebaseAuth.getInstance()

    suspend fun signIn() {
        require(BuildConfig.WEB_CLIENT_ID.isNotBlank()) {
            "The Google web client ID is missing."
        }

        val option = GetSignInWithGoogleOption.Builder(
            BuildConfig.WEB_CLIENT_ID
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val result = credentials.getCredential(
            context = activity,
            request = request
        )

        val credential = result.credential

        require(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            "Google did not return a supported credential."
        }

        val google = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(google.idToken, null)

        firebase.signInWithCredential(firebaseCredential).await()
    }

    suspend fun signOut() {
        firebase.signOut()
        credentials.clearCredentialState(ClearCredentialStateRequest())
    }
}
