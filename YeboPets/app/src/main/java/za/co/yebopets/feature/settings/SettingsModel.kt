package za.co.yebopets.feature.settings

import androidx.compose.runtime.*
import za.co.yebopets.core.TaskModel

class SettingsModel(private val repository: SettingsRepository) : TaskModel() {
    var ready by mutableStateOf(false)
        private set

    var current by mutableStateOf(UserSettings())
        private set

    var draft by mutableStateOf(UserSettings())

    var account by mutableStateOf<UserAccount?>(null)
        private set

    init {
        load()
    }

    fun load() = task("Load settings") {
        account = repository.account()
        current = repository.load()
        draft = current.copy(biometricsEnabled=account?.id?.let {
            za.co.yebopets.part3.Local.prefs(it).getBoolean("biometric",false)
        } ?: false)
        ready = true
        account?.id?.let { uid ->
            za.co.yebopets.part3.Local.prefs(uid).edit().putBoolean("mobile",current.syncOnMobileData).apply()
            za.co.yebopets.part3.SyncWorker.schedule(za.co.yebopets.part3.Local.context,uid)
        }
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
            androidx.core.os.LocaleListCompat.forLanguageTags(current.language))
    }

    fun save() = task("Save settings") {
        current = repository.save(draft)
        draft = current
        account?.id?.let { uid ->
            androidx.work.WorkManager.getInstance(za.co.yebopets.part3.Local.context).cancelUniqueWork("saved-$uid")
            za.co.yebopets.part3.SyncWorker.schedule(za.co.yebopets.part3.Local.context,uid)
        }
        notice = "Settings saved to your account."
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
            androidx.core.os.LocaleListCompat.forLanguageTags(current.language))
    }
}