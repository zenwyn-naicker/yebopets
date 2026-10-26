/**************************************************************************************************************
 * Author: Firebase
 * Date: 2026
 * Title: Android Cloud Messaging setup
 * Type: [Source Code]
 * Available at: <https://firebase.google.com/docs/cloud-messaging/android/get-started> [Accessed 15 September 2026]
 **************************************************************************************************************/

package za.co.yebopets.part3

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.tasks.await
import retrofit2.http.*
import za.co.yebopets.MainActivity
import za.co.yebopets.R
import za.co.yebopets.core.ApiClient
import za.co.yebopets.feature.settings.UserSettings

data class DeviceRequest(val token: String)
interface DevicesApi {
    @PUT("api/me/devices") suspend fun register(@Header("Authorization") auth:String,@Body request:DeviceRequest)
    @HTTP(method="DELETE",path="api/me/devices",hasBody=true)
    suspend fun remove(@Header("Authorization") auth:String,@Body request:DeviceRequest)
}
object PushRegistration {
    suspend fun register(api:ApiClient) {
        try {
            val token=FirebaseMessaging.getInstance().token.await()
            api.create(DevicesApi::class.java).register(api.header(),DeviceRequest(token))
        } catch (e:kotlinx.coroutines.CancellationException) { throw e }
          catch (_:Exception) {                                                                  }
    }
    suspend fun unregister(api:ApiClient) {
        try {
            val token=FirebaseMessaging.getInstance().token.await()
            api.create(DevicesApi::class.java).remove(api.header(),DeviceRequest(token))
        } catch (e:kotlinx.coroutines.CancellationException) { throw e }
          catch (_:Exception) {                                                                   }
        NotificationManagerCompat.from(Local.context).cancelAll()
    }
}
class PetMessagingService:FirebaseMessagingService() {
    override fun onNewToken(token:String) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { SyncWorker.schedule(this,it) }
    }
    override fun onMessageReceived(message:RemoteMessage) {
        val uid=FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (message.data["uid"] != uid) return
        val json=Local.prefs(uid).getString("settings",null)
        val settings=json?.let { Local.gson.fromJson(it,UserSettings::class.java) } ?: UserSettings()
        val kind=message.data["kind"] ?: return
        if (!settings.notificationsEnabled || (kind=="status" && !settings.statusAlertsEnabled) ||
            (kind=="appointment" && !settings.appointmentRemindersEnabled)) return
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 33) return
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("pet-updates","YeboPets",NotificationManager.IMPORTANCE_DEFAULT))
        val eventId=message.data["eventId"] ?: return
        val seen=Local.prefs(uid)
        if (seen.getBoolean("event-$eventId",false)) return
        val intent=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val body=if (settings.language=="zu") "Vula i-YeboPets ukuze ubone isibuyekezo sakho." else "Open YeboPets to view your update."
        val notification=NotificationCompat.Builder(this,"pet-updates")
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("YeboPets").setContentText(body)
            .setContentIntent(intent).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        NotificationManagerCompat.from(this).notify(eventId.hashCode(),notification)
        seen.edit().putBoolean("event-$eventId",true).apply()
    }
}