package za.co.yebopets

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import za.co.yebopets.core.ApiClient
import za.co.yebopets.feature.journey.*

class JourneyHttpTest {
    @Test fun detailsAndHistoryUseAuthenticatedOwnerRoutes() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"application":{"id":"app"},"appointments":[],"adoption":null,"followUps":[]}"""))
            server.enqueue(MockResponse().setBody("[]"))
            val client = ApiClient(server.url("/").toString(), "owner") { "owner-token" }
            val repository = JourneyRepository(client)
            assertEquals("app", repository.details("app").application.id)
            assertTrue(repository.followUps("adoption").isEmpty())
            assertEquals("/api/applications/app", server.takeRequest().path)
            val history = server.takeRequest()
            assertEquals("/api/adoptions/adoption/follow-ups", history.path)
            assertEquals("Bearer owner-token", history.getHeader("Authorization"))
        }
    }

    @Test fun reschedulingSendsDateAndEditableLocationFields() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"id":"visit","applicationId":"app","status":"Scheduled"}"""))
            val repository = JourneyRepository(ApiClient(server.url("/").toString()) { "token" })
            repository.updateAppointment("visit", AppointmentUpdateRequest("Scheduled", "2026-11-01T10:00:00+02:00", "Home", "14 Road", "Updated notes"))
            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/appointments/visit", request.path)
            val body = request.body.readUtf8()
            assertTrue(body.contains("14 Road"))
            assertTrue(body.contains("Updated notes"))
        }
    }
}
