package za.co.yebopets.feature.saved

object ComparisonSelection {
    fun toggle(selected: Set<String>, petId: String): Set<String> {
        require(petId.isNotBlank()) { "Choose a valid pet." }

        if (petId in selected) {
            return selected - petId
        }

        require(selected.size < 3) {
            "Compare up to three pets. Deselect one before adding another."
        }

        return selected + petId
    }
}