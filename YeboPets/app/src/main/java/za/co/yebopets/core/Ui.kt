/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: State and Jetpack Compose
 * Type: [Source Code]
 * Available at: <https://developer.android.com/develop/ui/compose/state> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: State holders and UI state
 * Type: [Source Code]
 * Available at: <https://developer.android.com/topic/architecture/ui-layer/stateholders> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Logcat command-line tool
 * Type: [Source Code]
 * Available at: <https://developer.android.com/tools/logcat> [Accessed 9 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Material 3 in Compose
 * Type: [Source Code]
 * Available at: <https://developer.android.com/develop/ui/compose/designsystems/material3> [Accessed 9 September 2026]
 **************************************************************************************************************/

package za.co.yebopets.core

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import za.co.yebopets.R
import za.co.yebopets.ui.theme.*
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

open class TaskModel : ViewModel() {
    var busy by mutableStateOf(false)
        protected set
    var error by mutableStateOf<String?>(null)
        protected set
    var notice by mutableStateOf<String?>(null)
        protected set

    protected fun task(label: String, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        notice = null

        viewModelScope.launch {
            Log.i("YeboPets", "$label started")
            try {
                action()
                Log.i("YeboPets", "$label completed")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = when (failure) {
                    is HttpException -> when (failure.code()) {
                        400 -> "Please check your input."
                        401 -> "Your session is invalid. Sign out and sign in again."
                        404 -> "This information is unavailable."
                        409 -> "This request conflicts with current data. Check your profile and existing applications."
                        else -> "The service could not complete this request. Please retry."
                    }
                    is IOException -> "Connection failed. Check your internet and retry."
                    is IllegalArgumentException -> failure.message ?: "Please check your input."
                    else -> "The operation failed. Please retry."
                }
                Log.w("YeboPets", "$label failed: ${failure.javaClass.simpleName}")
            } finally {
                busy = false
            }
        }
    }
}

val SmallCardShape = RoundedCornerShape(18.dp)
val LargeCardShape = RoundedCornerShape(24.dp)

enum class AppIconKind { HOME, HEART, PROFILE, SETTINGS, FILTER, BACK, SEARCH, LOCATION, CHECK, APPLICATION, PLUS, CALENDAR }

@Composable
fun AppIcon(
    kind: AppIconKind,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 22.dp,
    contentDescription: String? = null,
) {
    val spoken=contentDescription?.let { za.co.yebopets.part3.T(it) }
    val iconModifier = if (contentDescription == null) modifier else modifier.semantics {
        this.contentDescription = spoken.orEmpty()
    }
    Canvas(iconModifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * .085f)
        when (kind) {
            AppIconKind.HOME -> {
                val roof = Path().apply {
                    moveTo(w * .12f, h * .48f)
                    lineTo(w * .5f, h * .16f)
                    lineTo(w * .88f, h * .48f)
                }
                drawPath(roof, tint, style = stroke)
                drawRect(tint, androidx.compose.ui.geometry.Offset(w * .25f, h * .45f), androidx.compose.ui.geometry.Size(w * .5f, h * .4f), style = stroke)
            }
            AppIconKind.HEART -> {
                val heart = Path().apply {
                    moveTo(w * .5f, h * .84f)
                    cubicTo(w * .16f, h * .6f, w * .08f, h * .36f, w * .26f, h * .24f)
                    cubicTo(w * .38f, h * .16f, w * .48f, h * .23f, w * .5f, h * .32f)
                    cubicTo(w * .54f, h * .22f, w * .66f, h * .16f, w * .77f, h * .24f)
                    cubicTo(w * .94f, h * .38f, w * .82f, h * .62f, w * .5f, h * .84f)
                    close()
                }
                drawPath(heart, tint, style = stroke)
            }
            AppIconKind.PROFILE -> {
                drawCircle(tint, w * .18f, androidx.compose.ui.geometry.Offset(w * .5f, h * .31f), style = stroke)
                drawArc(tint, 200f, 140f, false, androidx.compose.ui.geometry.Offset(w * .18f, h * .45f), androidx.compose.ui.geometry.Size(w * .64f, h * .48f), style = stroke)
            }
            AppIconKind.SETTINGS -> {
                drawCircle(tint, w * .31f, androidx.compose.ui.geometry.Offset(w * .5f, h * .5f), style = stroke)
                drawCircle(tint, w * .09f, androidx.compose.ui.geometry.Offset(w * .5f, h * .5f), style = stroke)
            }
            AppIconKind.FILTER -> {
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .15f, h * .27f), androidx.compose.ui.geometry.Offset(w * .85f, h * .27f), strokeWidth = w * .08f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .28f, h * .5f), androidx.compose.ui.geometry.Offset(w * .72f, h * .5f), strokeWidth = w * .08f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .4f, h * .73f), androidx.compose.ui.geometry.Offset(w * .6f, h * .73f), strokeWidth = w * .08f)
            }
            AppIconKind.BACK -> {
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .75f, h * .2f), androidx.compose.ui.geometry.Offset(w * .28f, h * .5f), strokeWidth = w * .1f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .28f, h * .5f), androidx.compose.ui.geometry.Offset(w * .75f, h * .8f), strokeWidth = w * .1f)
            }
            AppIconKind.SEARCH -> {
                drawCircle(tint, w * .26f, androidx.compose.ui.geometry.Offset(w * .42f, h * .42f), style = stroke)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .62f, h * .62f), androidx.compose.ui.geometry.Offset(w * .85f, h * .85f), strokeWidth = w * .1f)
            }
            AppIconKind.LOCATION -> {
                drawCircle(tint, w * .26f, androidx.compose.ui.geometry.Offset(w * .5f, h * .4f), style = stroke)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .34f, h * .6f), androidx.compose.ui.geometry.Offset(w * .5f, h * .88f), strokeWidth = w * .08f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .66f, h * .6f), androidx.compose.ui.geometry.Offset(w * .5f, h * .88f), strokeWidth = w * .08f)
            }
            AppIconKind.CHECK -> {
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .17f, h * .52f), androidx.compose.ui.geometry.Offset(w * .4f, h * .75f), strokeWidth = w * .11f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .4f, h * .75f), androidx.compose.ui.geometry.Offset(w * .84f, h * .25f), strokeWidth = w * .11f)
            }
            AppIconKind.APPLICATION -> {
                drawRect(tint, androidx.compose.ui.geometry.Offset(w * .2f, h * .12f), androidx.compose.ui.geometry.Size(w * .6f, h * .76f), style = stroke)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .32f, h * .38f), androidx.compose.ui.geometry.Offset(w * .68f, h * .38f), strokeWidth = w * .08f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .32f, h * .58f), androidx.compose.ui.geometry.Offset(w * .68f, h * .58f), strokeWidth = w * .08f)
            }
            AppIconKind.PLUS -> {
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .2f, h * .5f), androidx.compose.ui.geometry.Offset(w * .8f, h * .5f), strokeWidth = w * .1f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .5f, h * .2f), androidx.compose.ui.geometry.Offset(w * .5f, h * .8f), strokeWidth = w * .1f)
            }
            AppIconKind.CALENDAR -> {
                drawRect(tint, androidx.compose.ui.geometry.Offset(w * .15f, h * .22f), androidx.compose.ui.geometry.Size(w * .7f, h * .65f), style = stroke)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .15f, h * .4f), androidx.compose.ui.geometry.Offset(w * .85f, h * .4f), strokeWidth = w * .08f)
            }
        }
    }
}

@DrawableRes
fun Animal.imageResource(): Int = when (id.lowercase()) {
    "luna" -> R.drawable.pet_luna
    "milo" -> R.drawable.pet_milo
    "nala" -> R.drawable.pet_nala
    "saffron" -> R.drawable.pet_saffron
    else -> R.drawable.pet_luna
}

@Composable
fun AnimalImage(animal: Animal, modifier: Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val client=za.co.yebopets.part3.LocalMediaClient.current
    val context=LocalContext.current
    var header by remember(client,animal.photoUrl) { mutableStateOf<String?>(null) }
    LaunchedEffect(client,animal.photoUrl) {
        if (client != null && animal.photoUrl?.startsWith("/api/media/") == true)
            try { header=client.header() } catch (e:kotlinx.coroutines.CancellationException) {throw e} catch (_:Exception) { }
    }
    if(header!=null && animal.photoUrl!=null) coil.compose.AsyncImage(
        model=coil.request.ImageRequest.Builder(context)
            .data(za.co.yebopets.BuildConfig.API_BASE_URL.trimEnd('/')+animal.photoUrl)
            .addHeader("Authorization",header!!)
            .memoryCachePolicy(coil.request.CachePolicy.DISABLED).diskCachePolicy(coil.request.CachePolicy.DISABLED).build(),
        contentDescription=animal.name,modifier=modifier,contentScale=contentScale,
        placeholder=painterResource(animal.imageResource()),error=painterResource(animal.imageResource()))
    else Image(painterResource(animal.imageResource()), "${animal.name}, ${animal.breed}", modifier, contentScale = contentScale)
}

@Composable
fun BrandBar(initials: String, onProfile: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("YeboPets", style = MaterialTheme.typography.titleLarge, color = YeboInk)
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onProfile,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(YeboInk),
        ) {
            Text(initials, style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            ) { AppIcon(AppIconKind.BACK, size = 19.dp, contentDescription = "Back") }
        }
        Text(za.co.yebopets.part3.T(title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun Page(title: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader(title, onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun Status(model: TaskModel) {
    if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = YeboCoral)
    model.error?.let { Text(za.co.yebopets.part3.T(it), color = MaterialTheme.colorScheme.error) }
    model.notice?.let {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) {
            Text(za.co.yebopets.part3.T(it), Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), color = YeboInk)
        }
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(za.co.yebopets.part3.T(title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        if (action != null) {
            Text(za.co.yebopets.part3.T(action), Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = onAction != null) { onAction?.invoke() }.padding(6.dp), style = MaterialTheme.typography.labelMedium, color = YeboCoral)
        }
    }
}

@Composable
fun MatchPill(score: Int, modifier: Modifier = Modifier) {
    Box(modifier.border(1.dp, YeboInk, RoundedCornerShape(100.dp)).background(YeboSun, RoundedCornerShape(100.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(za.co.yebopets.part3.T("$score% match"), style = MaterialTheme.typography.labelMedium, color = YeboInk)
    }
}

@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier, color: Color = YeboTeal) {
    Text(za.co.yebopets.part3.T(text), modifier.background(color.copy(alpha = .16f), RoundedCornerShape(100.dp)).padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = YeboInk)
}

@Composable
fun PetCard(animal: Animal, saved: Boolean, score: Int?, onSave: () -> Unit, onOpen: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = LargeCardShape,
        border = BorderStroke(1.dp, YeboInk.copy(alpha = .45f)),
        colors = CardDefaults.cardColors(containerColor = YeboPaper),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box {
            AnimalImage(animal, Modifier.fillMaxWidth().height(238.dp))
            IconButton(
                onClick = onSave,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp).clip(CircleShape).background(YeboPaper.copy(alpha = .93f)),
            ) {
                AppIcon(
                    AppIconKind.HEART,
                    tint = if (saved) YeboCoral else YeboInk,
                    size = 21.dp,
                    contentDescription = if (saved) "Remove ${animal.name} from saved pets" else "Save ${animal.name}",
                )
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(animal.name, style = MaterialTheme.typography.titleLarge)
                    Text("${animal.ageLabel()} • ${za.co.yebopets.part3.T(animal.size)} • ${za.co.yebopets.part3.T(animal.sex)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                score?.let { MatchPill(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIconKind.LOCATION, size = 15.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(animal.locationLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun Animal.ageLabel(): String = za.co.yebopets.part3.T(when {
    ageMonths % 12 == 0 -> "${ageMonths / 12} ${if (ageMonths == 12) "year" else "years"}"
    else -> "$ageMonths months"
})

fun Animal.locationLabel(): String = distanceKm?.let { "$area • ${it.toInt()} km" } ?: area

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Text(za.co.yebopets.part3.T(label),
        modifier.clip(RoundedCornerShape(100.dp)).background(if (selected) YeboInk else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) YeboInk else MaterialTheme.colorScheme.outline, RoundedCornerShape(100.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color.White else YeboInk
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceRow(label: String, options: List<String>, selected: String, enabled: Boolean = true, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(za.co.yebopets.part3.T(label), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option -> ChoiceChip(option, option == selected, { onSelect(option) }, enabled = enabled) }
        }
    }
}

@Composable
fun PetSummary(animal: Animal, onOpen: () -> Unit) = PetCard(animal, false, null, {}, onOpen)

@Composable
fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, leadingContent: (@Composable () -> Unit)? = null) {
    Button(onClick, modifier.height(52.dp), enabled, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = YeboCoral, contentColor = YeboInk), contentPadding = PaddingValues(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            leadingContent?.invoke()
            Text(za.co.yebopets.part3.T(text), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SecondaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick, modifier.height(52.dp), enabled, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, YeboInk), colors = ButtonDefaults.outlinedButtonColors(contentColor = YeboInk), contentPadding = PaddingValues(horizontal = 18.dp)) {
        Text(za.co.yebopets.part3.T(text), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun YeboTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, singleLine: Boolean = true, minLines: Int = 1) {
    OutlinedTextField(
        value, onValueChange, label = { Text(za.co.yebopets.part3.T(label)) }, modifier = modifier.fillMaxWidth(), singleLine = singleLine, minLines = minLines, enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YeboInk, unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
fun BooleanRow(label: String, value: Boolean, enabled: Boolean = true, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(za.co.yebopets.part3.T(label), Modifier.weight(1f).padding(end = 12.dp), style = MaterialTheme.typography.bodyMedium)
        Switch(value, change, enabled = enabled, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = YeboTeal))
    }
}

@Composable
fun KeyValueRow(label: String, value: String, showDivider: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(za.co.yebopets.part3.T(label), Modifier.weight(.8f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1.2f), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private val YeboZone: ZoneId = ZoneId.of("Africa/Johannesburg")
private val FriendlyDateTimeFormatter get() = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.getDefault())
private val FriendlyDateFormatter get() = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

fun defaultDateTime(daysAhead: Long, hour: Int): ZonedDateTime =
    ZonedDateTime.now(YeboZone).plusDays(daysAhead).withHour(hour).withMinute(0).withSecond(0).withNano(0)

fun ZonedDateTime.toApiTimestamp(): String = format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

fun ZonedDateTime.friendlyDateTime(): String = format(FriendlyDateTimeFormatter)

fun friendlyDateTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(YeboZone).format(FriendlyDateTimeFormatter)
}.getOrElse { value }

fun friendlyDate(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(YeboZone).format(FriendlyDateFormatter)
}.getOrElse { value }

fun statusLabel(value: String): String = when (value) {
    "UnderReview" -> "Under review"
    "MoreInfo" -> "Request info"
    "Approved" -> "Approved"
    "Declined" -> "Declined"
    "Submitted" -> "Submitted"
    "Published" -> "Published"
    "Paused" -> "Paused"
    "Draft" -> "Draft"
    "Adopted" -> "Adopted"
    else -> value.replace(Regex("([a-z])([A-Z])")) {
        "${it.groupValues[1]} ${it.groupValues[2]}"
    }.replaceFirstChar { it.uppercase() }
}

fun wellbeingLabel(value: String): String = when (value) {
    "Good" -> "Doing well"
    "Concern" -> "A concern"
    "NeedsAssistance" -> "Need help"
    else -> statusLabel(value)
}

@Composable
fun DateTimePickerField(
    label: String,
    value: ZonedDateTime,
    onValueChange: (ZonedDateTime) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val context = LocalContext.current

    fun showPicker() {
        if (!enabled) return
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val selectedDate = LocalDate.of(year, month + 1, day)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onValueChange(ZonedDateTime.of(selectedDate, LocalTime.of(hour, minute), YeboZone))
                    },
                    value.hour,
                    value.minute,
                    true,
                ).show()
            },
            value.year,
            value.monthValue - 1,
            value.dayOfMonth,
        ).show()
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(za.co.yebopets.part3.T(label), style = MaterialTheme.typography.titleSmall)
        Card(
            modifier = Modifier.fillMaxWidth().semantics {
                role = Role.Button
                contentDescription = "$label, ${value.friendlyDateTime()}"
            }.clickable(enabled = enabled, onClick = ::showPicker),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppIcon(AppIconKind.CALENDAR, tint = YeboCoral, contentDescription = "Choose date and time")
                Text(value.friendlyDateTime(), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
