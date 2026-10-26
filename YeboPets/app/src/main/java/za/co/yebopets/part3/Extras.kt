/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Per-app language preferences
 * Type: [Source Code]
 * Available at: <https://developer.android.com/guide/topics/resources/app-languages> [Accessed 15 September 2026]
 **************************************************************************************************************/

package za.co.yebopets.part3

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.co.yebopets.core.*
import za.co.yebopets.feature.settings.SettingsModel

@Composable fun S(name:String):String {
    val context=LocalContext.current
    val id=context.resources.getIdentifier(name,"string",context.packageName)
    return androidx.compose.ui.res.stringResource(id)
}
@Composable fun Part3Settings(model:SettingsModel) {
    val activity=androidx.activity.compose.LocalActivity.current as FragmentActivity
    val uid=model.account?.id ?: return
    val scope=rememberCoroutineScope()
    var localBiometric by remember(uid) { mutableStateOf(Local.prefs(uid).getBoolean("biometric",false)) }
    var biometricError by remember { mutableStateOf(false) }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LanguageSelector(model.draft.language, enabled = !model.busy) {
        model.draft = model.draft.copy(language = it)
    }
    BooleanRow(S("p3_notifications"),model.draft.notificationsEnabled) {
        model.draft=model.draft.copy(notificationsEnabled=it)
        if(it && Build.VERSION.SDK_INT>=33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    BooleanRow(S("p3_status_alerts"),model.draft.statusAlertsEnabled) { model.draft=model.draft.copy(statusAlertsEnabled=it) }
    BooleanRow(S("p3_appointments"),model.draft.appointmentRemindersEnabled) { model.draft=model.draft.copy(appointmentRemindersEnabled=it) }
    BooleanRow(S("p3_mobile"),model.draft.syncOnMobileData) { model.draft=model.draft.copy(syncOnMobileData=it) }
    BooleanRow(S("p3_biometrics"),localBiometric) { enabled ->
        if(enabled) Biometrics.prompt(activity,{
            Local.prefs(uid).edit().putBoolean("biometric",true).apply()
            localBiometric=true; biometricError=false; model.draft=model.draft.copy(biometricsEnabled=true)
        },{biometricError=true})
        else { Local.prefs(uid).edit().putBoolean("biometric",false).apply(); localBiometric=false; model.draft=model.draft.copy(biometricsEnabled=false) }
    }
    if(biometricError) Text(S("p3_biometric_failed"))
    Text(S("p3_biometric_device"))
    var pending by remember { mutableStateOf<List<LocalDb.Pending>>(emptyList()) }
    val changed by Local.changed.collectAsState()
    LaunchedEffect(uid,changed) { pending=withContext(Dispatchers.IO) { Local.db.store().pending(uid) } }
    pending.filter { it.lastError!=null }.forEach { Text("${it.animalId}: ${it.lastError}") }
    var confirm by remember { mutableStateOf(false) }
    TextButton(onClick={confirm=true}) { Text(S("p3_discard")) }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text(S("p3_discard"))},text={Text(S("p3_discard_help"))},
        confirmButton={ TextButton(onClick={scope.launch { Local.discard(uid); confirm=false }}) {Text(S("p3_discard"))} },
        dismissButton={TextButton(onClick={confirm=false}) {Text(S("p3_cancel"))}})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(language: String, enabled: Boolean = true, select: (String) -> Unit) {
    Column {
        Text(S("p3_language"), style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("en" to "English", "zu" to "isiZulu").forEachIndexed { index, (code, label) ->
                SegmentedButton(
                    selected = language == code,
                    onClick = { select(code) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                ) { Text(label) }
            }
        }
    }
}
