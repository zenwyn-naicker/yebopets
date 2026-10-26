package za.co.yebopets.feature.profile

import androidx.compose.runtime.*
import retrofit2.HttpException
import za.co.yebopets.core.TaskModel

class ProfileModel(private val repository: ProfileRepository) : TaskModel() {
    var draft by mutableStateOf(AdopterProfile())

    init {
        load()
    }

    fun load() = task("Load adopter profile") {
        draft = try {
            repository.load()
        } catch (failure: HttpException) {
            if (failure.code() == 404) {
                AdopterProfile()
            } else {
                throw failure
            }
        }
    }

    fun save() = task("Save adopter profile") {
        draft = repository.save(draft)
        notice = "Profile saved. Open a pet to see your updated compatibility."
    }
}