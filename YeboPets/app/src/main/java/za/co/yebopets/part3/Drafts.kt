package za.co.yebopets.part3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import za.co.yebopets.feature.journey.ApplicationRequest

object Drafts {
    suspend fun load(uid:String,id:String):ApplicationRequest? = withContext(Dispatchers.IO) {
        Local.db.store().draft(uid,id)?.let { Local.gson.fromJson(it.json,ApplicationRequest::class.java) }
    }
    suspend fun save(uid:String,id:String,value:ApplicationRequest) = withContext(Dispatchers.IO) {
        Local.db.store().draft(LocalDb.Draft().apply {
            this.uid=uid; animalId=id; json=Local.gson.toJson(value); lastEditedAt=System.currentTimeMillis()
        })
    }
    suspend fun delete(uid:String,id:String) = withContext(Dispatchers.IO) { Local.db.store().deleteDraft(uid,id) }
}