package za.co.yebopets.part3

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import za.co.yebopets.core.ApiClient

/** Re-fetch private photos through the API so cached pixels do not outlive account access. */
@Composable
fun PrivatePhoto(client: ApiClient, url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var header by remember(client, url) { mutableStateOf<String?>(null) }
    var failed by remember(client, url) { mutableStateOf(false) }
    LaunchedEffect(client, url) {
        try { client.mediaUrl(url); header = client.header() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    if (failed) Text(T("Photograph unavailable. Reopen when connected."))
    else if (header == null) CircularProgressIndicator()
    else AsyncImage(
        model = ImageRequest.Builder(context).data(client.mediaUrl(url)).addHeader("Authorization", header!!)
            .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build(),
        contentDescription = T("Follow-up photograph"),
        modifier = modifier,
        contentScale = ContentScale.Fit,
        onError = { failed = true },
    )
}
