package za.co.yebopets.feature.auth

import androidx.compose.runtime.*
import za.co.yebopets.core.TaskModel
import za.co.yebopets.feature.settings.SettingsRepository
import za.co.yebopets.feature.settings.UserAccount

class AccountAccessModel(private val repository: SettingsRepository, private val uid: String) : TaskModel() {
    var account by mutableStateOf<UserAccount?>(null)
        private set
    var selectedRole by mutableStateOf<String?>(null)
        private set
    val roles: List<String> get() = account?.roles.orEmpty().filter { it in listOf("Adopter", "ShelterStaff") }.distinct()

    init { load() }

    fun load() = task("Load account access") {
        val result = repository.access()
        require(result.id == uid && result.roles.orEmpty().any { it in listOf("Adopter", "ShelterStaff") }) { "Account access is unavailable. Please sign in again." }
        account = result
        selectedRole = if (roles.size == 1) roles.single() else selectedRole?.takeIf { it in roles }
    }

    fun choose(role: String) {
        require(role in roles) { "This workspace is unavailable for your account." }
        selectedRole = role
    }

    fun switchWorkspace() { selectedRole = null }
}
