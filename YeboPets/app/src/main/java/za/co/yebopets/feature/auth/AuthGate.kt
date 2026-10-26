/**************************************************************************************************************
 * Author: Firebase
 * Date: 2026
 * Title: Add Firebase to your Android project
 * Type: [Source Code]
 * Available at: <https://firebase.google.com/docs/android/setup> [Accessed 9 September 2026]
 **************************************************************************************************************/
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
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import za.co.yebopets.BuildConfig
import za.co.yebopets.R
import za.co.yebopets.core.*

@Composable
fun AuthGate(content: @Composable (ApiClient, String, () -> Unit, (() -> Unit)?) -> Unit) {
    val context = LocalContext.current

    if (FirebaseApp.getApps(context).isEmpty() || BuildConfig.WEB_CLIENT_ID.isBlank()) {
        Page("YeboPets setup") {
            Text(za.co.yebopets.part3.T("Sign-in is unavailable in this build. Please contact the app team."))
        }
        return
    }

    val firebase = remember { FirebaseAuth.getInstance() }
    val google = remember { GoogleAuth(context as Activity) }
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf(firebase.currentUser) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    DisposableEffect(firebase) {
        val listener = FirebaseAuth.AuthStateListener {
            user = it.currentUser
            Log.i(
                "YeboPets",
                if (user == null) "Signed out" else "Authenticated session"
            )
        }

        firebase.addAuthStateListener(listener)
        onDispose { firebase.removeAuthStateListener(listener) }
    }

    val current = user

    if (current == null) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()),
        ) {
            ScreenHeader("Welcome")
            Box(Modifier.fillMaxWidth().height(230.dp)) {
                Image(
                    painterResource(R.drawable.pet_luna),
                    "Luna smiling in a garden",
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    "YeboPets",
                    Modifier.align(Alignment.TopStart).padding(20.dp).clip(RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = .94f)).padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    za.co.yebopets.part3.T("Find your kind of family."),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    za.co.yebopets.part3.T("Sign in to find your next companion or manage your shelter."),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                message?.let { Text(za.co.yebopets.part3.T(it), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                if (busy) CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                PrimaryAction(
                    "Continue with Google",
                    {
                        scope.launch {
                            busy = true
                            message = null
                            try {
                                google.signIn()
                            } catch (_: GetCredentialCancellationException) {
                                message = "Sign-in cancelled."
                            } catch (_: NoCredentialException) {
                                message = "No Google account is available. Add an account and retry."
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                message = "Google sign-in failed. Please check your connection and retry."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = !busy,
                    leadingContent = { GoogleMark() },
                )
                Text(
                    za.co.yebopets.part3.T("Secure sign-in · No separate password"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    key(current.uid) {
        val owner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }

        DisposableEffect(owner) {
            onDispose { owner.viewModelStore.clear() }
        }

        val api = remember(current.uid) {
            ApiClient(BuildConfig.API_BASE_URL, current.uid) {
                check(firebase.currentUser?.uid == current.uid)

                val token = current.getIdToken(false).await().token
                    ?: error("Sign-in session is unavailable. Please sign in again.")

                check(firebase.currentUser?.uid == current.uid)
                token
            }
        }

        LaunchedEffect(current.uid) { za.co.yebopets.part3.SyncWorker.schedule(context,current.uid) }
        val signOut: () -> Unit = {
            scope.launch {
                try {
                    za.co.yebopets.part3.PushRegistration.unregister(api)
                    androidx.work.WorkManager.getInstance(context).cancelUniqueWork("saved-${current.uid}")
                    google.signOut()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    message = "Signed out. Google account-picker cleanup could not finish."
                }
            }
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner, za.co.yebopets.part3.LocalMediaClient provides api) {
            val access: AccountAccessModel = viewModel {
                AccountAccessModel(za.co.yebopets.feature.settings.SettingsRepository(api, "Adopter"), current.uid)
            }
            val role = access.selectedRole
            when {
                access.account == null -> Page("Connecting your account") {
                    Status(access)
                    Text(za.co.yebopets.part3.T("Loading your account and saved preferences."))
                    if (!access.busy) PrimaryAction("Retry", access::load)
                    TextButton(onClick = signOut) { Text(za.co.yebopets.part3.T("Sign out")) }
                }
                role == null -> WorkspacePicker(access.account!!, access.roles, access::choose, signOut)
                else -> key(role) {
                    // Workspace ViewModels must not leak selected-role settings or screens into the other mode.
                    val workspaceOwner = remember {
                        object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
                    }
                    DisposableEffect(workspaceOwner) { onDispose { workspaceOwner.viewModelStore.clear() } }
                    CompositionLocalProvider(LocalViewModelStoreOwner provides workspaceOwner) {
                        content(api, role, signOut, if (access.roles.size > 1) access::switchWorkspace else null)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleMark() {
    Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
        Text("G", style = MaterialTheme.typography.labelLarge, color = Color(0xFF4285F4))
    }
}
@Composable
internal fun WorkspacePicker(
    account: za.co.yebopets.feature.settings.UserAccount,
    roles: List<String>,
    choose: (String) -> Unit,
    signOut: () -> Unit,
) {
    Page("Choose your workspace") {
        Text(account.name.ifBlank { "YeboPets" }, style = MaterialTheme.typography.headlineMedium)
        Text(account.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(za.co.yebopets.part3.T("Choose how you want to use YeboPets."))
        if ("Adopter" in roles) PrimaryAction("Adopter", { choose("Adopter") }, Modifier.fillMaxWidth())
        if ("ShelterStaff" in roles) SecondaryAction("Shelter staff", { choose("ShelterStaff") }, Modifier.fillMaxWidth())
        TextButton(onClick = signOut) { Text(za.co.yebopets.part3.T("Sign out")) }
    }
}
