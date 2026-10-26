package za.co.yebopets

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import za.co.yebopets.core.ApiClient
import za.co.yebopets.feature.settings.SettingsRepository

class AccountHttpTest {
    @Test
    fun accountRequestUsesTheFirebaseBearerToken() = runBlocking {
        val server = MockWebServer()
        server.start()

        try {
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "id": "test-user",
                      "name": "Test User",
                      "email": "test@example.com",
                      "role": "Adopter"
                    }
                    """.trimIndent()
                )
            )

            val client = ApiClient(server.url("/").toString()) {
                "firebase-test-token"
            }

            val account = SettingsRepository(client, "Adopter").account()
            val request = server.takeRequest()

            assertEquals("/api/me?requestedRole=Adopter", request.path)
            assertEquals(
                "Bearer firebase-test-token",
                request.getHeader("Authorization")
            )
            assertEquals("test-user", account.id)
            assertEquals("Adopter", account.role)
        } finally {
            server.shutdown()
        }
    }
}