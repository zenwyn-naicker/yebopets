package za.co.yebopets

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import za.co.yebopets.feature.journey.ApplicationRequest
import za.co.yebopets.part3.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class Part3RoomJvmTest {
    @Test fun accountIsolationAndAtomicQueueRollback() = runBlocking<Unit>(Dispatchers.IO) {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, LocalDb::class.java).build()
        try {
            val dao = db.store()
            dao.pet(LocalDb.Pet().apply { uid = "a"; id = "luna"; json = "{}"; saved = true })
            assertTrue(dao.pets("b").isEmpty())
            try {
                db.runInTransaction {
                    dao.enqueue(LocalDb.Pending().apply { uid = "a"; animalId = "luna"; operationId = "op1"; createdAt = 1 })
                    error("Simulated interruption")
                }
            } catch (_: IllegalStateException) { }
            assertTrue(dao.pending("a").isEmpty())
        } finally { db.close() }
    }

    @Test fun draftsPersistOnlyForTheirOwnerAndDeleteAfterSubmission() = runBlocking<Unit> {
        Local.init(RuntimeEnvironment.getApplication())
        val draft = ApplicationRequest("I offer a safe and caring home.", true, true, "Daily walks")
        Drafts.save("draft-a", "luna", draft)
        assertEquals(draft, Drafts.load("draft-a", "luna"))
        assertNull(Drafts.load("draft-b", "luna"))
        Drafts.delete("draft-a", "luna")
        assertNull(Drafts.load("draft-a", "luna"))
    }
}
