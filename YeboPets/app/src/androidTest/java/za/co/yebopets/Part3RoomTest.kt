package za.co.yebopets
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import za.co.yebopets.part3.LocalDb
@RunWith(AndroidJUnit4::class)
class Part3RoomTest {
    @Test fun accountIsolationAndAtomicQueueRollback() {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),LocalDb::class.java).build()
        try {
            val dao=db.store()
            dao.pet(LocalDb.Pet().apply {uid="a";id="luna";json="{}";saved=true})
            assertTrue(dao.pets("b").isEmpty())
            try {
                db.runInTransaction {
                    dao.enqueue(LocalDb.Pending().apply {uid="a";animalId="luna";operationId="op1";createdAt=1})
                    error("Simulated interruption")
                }
            } catch(_:IllegalStateException) { }
            assertTrue(dao.pending("a").isEmpty())
            dao.draft(LocalDb.Draft().apply {uid="a";animalId="luna";json="draft"})
            assertNull(dao.draft("b","luna"))
            assertEquals("draft",dao.draft("a","luna").json)
        } finally { db.close() }
    }

    @Test fun languageResourcesAndAppCompatStartup() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val config=android.content.res.Configuration(context.resources.configuration)
        config.setLocale(java.util.Locale.forLanguageTag("zu"))
        val zulu=context.createConfigurationContext(config)
        assertEquals("Vula i-YeboPets",zulu.getString(R.string.p3_unlock))
        assertEquals("Isicelo sika-Luna",zulu.getString(R.string.fmt_0,"Luna"))
        val scenario=androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        try { scenario.onActivity { activity -> assertFalse(activity.isFinishing) } }
        finally { scenario.close() }
    }
}
