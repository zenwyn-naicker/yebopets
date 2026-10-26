package za.co.yebopets.part3

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.http.*
import za.co.yebopets.core.*
import java.io.ByteArrayOutputStream

data class MediaReply(val id:String,val url:String)
interface MediaApi {
    @Multipart @POST("api/media") suspend fun upload(@Header("Authorization") auth:String,
        @Part("kind") kind:okhttp3.RequestBody,@Part("entityId") entityId:okhttp3.RequestBody,
        @Part file:MultipartBody.Part):MediaReply
}
@Composable fun PhotoUpload(client:ApiClient,kind:String,entityId:String,onUploaded:(String)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember {mutableStateOf(false)}
    var status by remember {mutableStateOf("")}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null) scope.launch {
            busy=true; status=""
            try {
                val bytes=withContext(Dispatchers.IO) {

                    val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,bounds) }
                    require(bounds.outWidth>0 && bounds.outHeight>0)
                    var sample=1
                    while(bounds.outWidth/sample>1600 || bounds.outHeight/sample>1600) sample*=2
                    val bitmap=context.contentResolver.openInputStream(uri).use {
                        BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=sample})
                    } ?: error("Invalid image")
                    try { ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG,85,out); out.toByteArray() } }
                    finally { bitmap.recycle() }
                }
                require(bytes.size<=5_000_000)
                val reply=client.create(MediaApi::class.java).upload(client.header(),kind.toRequestBody("text/plain".toMediaType()),
                    entityId.toRequestBody("text/plain".toMediaType()),MultipartBody.Part.createFormData("file","photo.jpg",bytes.toRequestBody("image/jpeg".toMediaType())))
                onUploaded(reply.url); status="p3_photo_saved"
            } catch(e:kotlinx.coroutines.CancellationException) { throw e }
              catch(_:Exception) {status="p3_photo_failed"}
            finally {busy=false}
        }
    }
    SecondaryAction(S("p3_photo"),{picker.launch("image/*")},enabled=!busy)
    if(status.isNotEmpty()) Text(S(status))
}
