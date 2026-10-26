package za.co.yebopets

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import za.co.yebopets.core.ApiClient
import za.co.yebopets.core.AnimalsRepository

class AnimalHttpTest {
    @Test
    fun searchUsesTrimmedQueryAndBearerToken() = runBlocking {
        val server = MockWebServer()
        server.start()

        try {
            server.enqueue(MockResponse().setBody("[]"))

            val client = ApiClient(server.url("/").toString()) {
                "test-token"
            }

            AnimalsRepository(client).browse(
                "  Luna  ", "Dog", "Any", "Any",
                "Any", "Any", "Any", 50
            )

            val request = server.takeRequest()

            assertEquals("Bearer test-token", request.getHeader("Authorization"))
            assertEquals("/api/animals", request.requestUrl!!.encodedPath)
            assertEquals("Luna", request.requestUrl!!.queryParameter("q"))
            assertEquals("Dog", request.requestUrl!!.queryParameter("species"))
        } finally {
            server.shutdown()
        }
    }
}