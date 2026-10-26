/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Offline-first data layer
 * Type: [Source Code]
 * Available at: <https://developer.android.com/topic/architecture/data-layer/offline-first> [Accessed 15 September 2026]
 **************************************************************************************************************/
/**************************************************************************************************************
 * Author: Android Developers
 * Date: 2026
 * Title: Manage WorkManager work
 * Type: [Source Code]
 * Available at: <https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work> [Accessed 15 September 2026]
 **************************************************************************************************************/

package za.co.yebopets.part3

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import retrofit2.http.*
import za.co.yebopets.BuildConfig
import za.co.yebopets.core.*
import za.co.yebopets.feature.saved.SavedApi
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class Part3Application : Application() {
    override fun onCreate() { super.onCreate(); Local.init(this) }
}
object Local {
    lateinit var context: Context; private set
    val gson = Gson()
    val changed = MutableStateFlow(0L)
    val db: LocalDb by lazy { Room.databaseBuilder(context, LocalDb::class.java, "yebopets-offline.db").build() }
    fun init(context: Context) { this.context = context.applicationContext }
    fun prefs(uid: String) = context.getSharedPreferences("account_" + uid, Context.MODE_PRIVATE)
    fun signal() { changed.value = System.nanoTime() }
    suspend fun cache(uid: String?, animals: List<Animal>) {
        if (uid == null) return
        withContext(Dispatchers.IO) {
            db.runInTransaction {
                for (a in animals) {
                    val row = db.store().pet(uid, a.id) ?: LocalDb.Pet().apply { this.uid=uid; id=a.id }
                    row.json = gson.toJson(a); db.store().pet(row)
                }
            }
        }
    }
    suspend fun pets(uid: String): List<Animal> = withContext(Dispatchers.IO) {
        db.store().pets(uid).map { gson.fromJson(it.json, Animal::class.java) }
    }
    suspend fun discard(uid: String) = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelUniqueWork("saved-$uid")
        db.runInTransaction { db.store().clearPending(uid); db.store().clearDrafts(uid); db.store().clearPets(uid) }
        signal()
    }
}
data class SavedOperation(val clientOperationId: String, val animalId: String, val saved: Boolean)
data class SyncReply(val clientOperationId: String, val replayed: Boolean)
interface SyncApi {
    @POST("api/sync") suspend fun apply(@Header("Authorization") auth: String, @Body operation: SavedOperation): SyncReply
}
object RetryRules {
    fun transient(code: Int) = code == 408 || code == 429 || code >= 500
}
class OfflineSaved(private val client: ApiClient, private val uid: String) {
    private val store get() = Local.db.store()
    suspend fun load(): List<Animal> = withContext(Dispatchers.IO) {
        try {
            val remote = client.create(SavedApi::class.java).load(client.header())
            Local.db.runInTransaction {
                val pending = store.pending(uid).map { it.animalId }.toSet()
                val remoteIds = remote.map { it.id }.toSet()
                store.pets(uid).filter { it.id !in pending }.forEach { it.saved=it.id in remoteIds; store.pet(it) }
                for (a in remote) {
                    val row=store.pet(uid,a.id) ?: LocalDb.Pet().apply { this.uid=uid; id=a.id }
                    row.json=Local.gson.toJson(a); if (a.id !in pending) row.saved=true; store.pet(row)
                }
            }
        } catch (e: com.google.firebase.FirebaseNetworkException) {                               }
        catch (_: IOException) {                                                                  }
        catch (e: HttpException) { if (!RetryRules.transient(e.code())) throw e }
        store.pets(uid).filter { it.saved }.map { Local.gson.fromJson(it.json,Animal::class.java) }
    }
    suspend fun set(id: String, saved: Boolean) = withContext(Dispatchers.IO) {
        Local.db.runInTransaction {
            val row=store.pet(uid,id) ?: throw IllegalArgumentException("Open this pet online once before saving offline.")
            row.saved=saved; store.pet(row)
            val last=store.pending(uid).maxOfOrNull { it.createdAt } ?: 0
            store.enqueue(LocalDb.Pending().apply {
                operationId=UUID.randomUUID().toString().replace("-",""); this.uid=uid
                animalId=id; this.saved=saved; createdAt=maxOf(System.currentTimeMillis(),last+1)
            })
        }
        SyncWorker.schedule(Local.context,uid); Local.signal()
    }
}
class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context,parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uid=inputData.getString("uid") ?: return@withContext Result.failure()
        val firebase=FirebaseAuth.getInstance()
        if (firebase.currentUser?.uid != uid) return@withContext Result.success()
        val api=ApiClient(BuildConfig.API_BASE_URL, uid) {
            val user=firebase.currentUser
            check(user?.uid == uid)
            val token=user!!.getIdToken(false).await().token ?: error("Session expired")
            check(firebase.currentUser?.uid == uid); token
        }
        try {
            // Refresh server-authorised roles before syncing a queue created in either workspace.
            api.create(za.co.yebopets.feature.settings.SettingsApi::class.java).access(api.header())
            PushRegistration.register(api)
            for (item in Local.db.store().pending(uid)) {
                if (firebase.currentUser?.uid != uid) return@withContext Result.success()
                if (item.lastError?.startsWith("HTTP 4") == true &&
                    item.lastError !in listOf("HTTP 408","HTTP 429")) continue
                try {
                    api.create(SyncApi::class.java).apply(api.header(), SavedOperation(item.operationId,item.animalId,item.saved))
                    Local.db.store().acknowledge(uid,item.operationId)
                    Log.i("YeboPets","Saved change synchronised")
                } catch (e: HttpException) {
                    if (e.code() == 401) return@withContext Result.success()
                    Local.db.store().failed(uid,item.operationId,"HTTP ${e.code()}")
                    if (RetryRules.transient(e.code())) return@withContext Result.retry()

                }
            }
            Local.signal(); Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
    }
    companion object {
        fun schedule(context: Context, uid: String) {
            val mobile=Local.prefs(uid).getBoolean("mobile",false)
            val request=OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf("uid" to uid))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(if (mobile) NetworkType.CONNECTED else NetworkType.UNMETERED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork("saved-$uid",ExistingWorkPolicy.APPEND_OR_REPLACE,request)
        }
    }
}