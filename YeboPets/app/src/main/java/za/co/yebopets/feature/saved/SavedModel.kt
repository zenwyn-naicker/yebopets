package za.co.yebopets.feature.saved

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import za.co.yebopets.core.Animal
import za.co.yebopets.core.TaskModel

class SavedModel(private val repository: SavedRepository) : TaskModel() {
    var animals by mutableStateOf<List<Animal>>(emptyList())
        private set

    var selected by mutableStateOf<Set<String>>(emptySet())
        private set

    init {
        reload()
        viewModelScope.launch { za.co.yebopets.part3.Local.changed.collect { if (it != 0L) reload() } }
    }

    private suspend fun refresh() {
        animals = repository.load()
        selected = selected.intersect(animals.map { it.id }.toSet())
    }

    fun reload() = task("Load saved pets") {
        refresh()
    }

    fun save(id: String) = task("Save pet") {
        repository.save(id)
        refresh()
        notice = "Saved on this device. Sync runs when your allowed network is available."
    }

    fun remove(id: String) = task("Remove saved pet") {
        repository.remove(id)
        refresh()
        notice = "Removed on this device. Sync runs when your allowed network is available."
    }

    fun select(id: String) {
        if (animals.none { it.id == id }) return

        try {
            selected = ComparisonSelection.toggle(selected, id)
            error = null
        } catch (failure: IllegalArgumentException) {
            error = failure.message
        }
    }
}