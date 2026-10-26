/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Biometric authentication
 * Type: [Source Code]
 * Available at: <https://developer.android.com/identity/sign-in/biometric-auth> [Accessed 15 September 2026]
 **************************************************************************************************************/

package za.co.yebopets.part3

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import za.co.yebopets.core.*
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.alpha
import androidx.activity.compose.BackHandler

object Biometrics {
    fun available(activity:FragmentActivity) = BiometricManager.from(activity)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    fun prompt(activity:FragmentActivity, success:()->Unit, failed:()->Unit) {
        if (!available(activity)) { failed(); return }
        val prompt=BiometricPrompt(activity,ContextCompat.getMainExecutor(activity),object:BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult) { success() }
            override fun onAuthenticationError(code:Int,message:CharSequence) { failed() }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(activity.getString(za.co.yebopets.R.string.p3_unlock))
            .setSubtitle(activity.getString(za.co.yebopets.R.string.p3_biometric_help))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(activity.getString(za.co.yebopets.R.string.p3_cancel)).build())
    }
}
@Composable
fun ReturningAccess(content: @Composable () -> Unit) {
    val activity = androidx.activity.compose.LocalActivity.current as FragmentActivity
    if (FirebaseApp.getApps(activity).isEmpty()) { content(); return }
    val auth = remember { FirebaseAuth.getInstance() }
    val session = remember {
        val uid = auth.currentUser?.uid
        BiometricSession(uid, uid?.let { Local.prefs(it).getBoolean("biometric", false) } == true)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, auth) {
        val listener = FirebaseAuth.AuthStateListener { session.accountChanged(it.currentUser?.uid) }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> session.resume()
                Lifecycle.Event.ON_STOP -> if (!activity.isChangingConfigurations) {
                    session.stop(auth.currentUser?.uid?.let { Local.prefs(it).getBoolean("biometric", false) } == true)
                }
                else -> Unit
            }
        }
        auth.addAuthStateListener(listener)
        lifecycle.addObserver(observer)
        onDispose {
            auth.removeAuthStateListener(listener)
            lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(session.request) {
        val attempt = session.beginAttempt() ?: return@LaunchedEffect
        Biometrics.prompt(activity,
            { session.complete(attempt, true) },
            { session.complete(attempt, false) })
    }
    Box(Modifier.fillMaxSize()) {
        if (session.opened) {
            Box(Modifier.fillMaxSize().then(
                if (session.locked) Modifier.alpha(0f).clearAndSetSemantics {} else Modifier
            )) { content() }
        }
        if (session.locked && session.uid != null) {
            BackHandler { }
            Surface(Modifier.fillMaxSize()) {
                Page(S("p3_unlock")) {
                    Text(S("p3_biometric_help"))
                    if (session.error) {
                        Text(S("p3_biometric_failed"))
                        PrimaryAction(S("p3_unlock"), session::retry)
                        SecondaryAction(S("p3_google"), { auth.signOut() })
                    } else {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
