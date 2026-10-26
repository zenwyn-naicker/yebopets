package za.co.yebopets.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import za.co.yebopets.core.*

class MatchModel(private val repository: ProfileRepository) : TaskModel() {
    var result by mutableStateOf<MatchResult?>(null)
        private set

    fun load(id: String) = task("Load compatibility") {
        result = null
        result = repository.match(id)
    }
}

@Composable
fun MatchCard(client: ApiClient, animalId: String) {
    val model: MatchModel = viewModel(key = "match:$animalId") {
        MatchModel(ProfileRepository(client))
    }

    LaunchedEffect(animalId) {
        snapshotFlow { model.busy }.first { !it }
        model.load(animalId)
    }

    Card(Modifier.fillMaxWidth(), shape = LargeCardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(za.co.yebopets.part3.T("Compatibility"), style = MaterialTheme.typography.titleLarge)
            Status(model)

            model.result?.let { result ->
                MatchPill(result.overallScore)

                LinearProgressIndicator(
                    progress = {
                        result.overallScore.coerceIn(0, 100) / 100f
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(androidx.compose.ui.res.stringResource(za.co.yebopets.R.string.dyn_7, result.distanceKm.toString()))

                result.factors.forEach { factor ->
                    Text("${factor.name}: ${factor.points}/${factor.maximum}")
                    Text(za.co.yebopets.part3.T(factor.reason), style = MaterialTheme.typography.bodySmall)
                }

                Text(za.co.yebopets.part3.T(result.advice), style = MaterialTheme.typography.bodySmall)
            }

            TextButton(
                onClick = { model.load(animalId) },
                enabled = !model.busy
            ) {
                Text(za.co.yebopets.part3.T("Refresh compatibility"))
            }
        }
    }
}

@Composable
fun MatchScoreText(client: ApiClient, animalId: String) {
    val model: MatchModel = viewModel(key = "compare-match:$animalId") {
        MatchModel(ProfileRepository(client))
    }

    LaunchedEffect(animalId) {
        snapshotFlow { model.busy }.first { !it }
        model.load(animalId)
    }

    when {
        model.busy -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        model.result != null -> MatchPill(model.result!!.overallScore)
        else -> Text(za.co.yebopets.part3.T("Not available"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
