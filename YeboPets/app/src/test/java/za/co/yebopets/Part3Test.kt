package za.co.yebopets
import org.junit.Assert.*
import org.junit.Test
import za.co.yebopets.part3.*
import za.co.yebopets.core.ApiClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
class Part3Test {
    @Test fun retryPolicySeparatesPermanentFailures() {
        listOf(408,429,500,503).forEach {assertTrue(RetryRules.transient(it))}
        listOf(400,401,403,404,409,422).forEach {assertFalse(RetryRules.transient(it))}
    }
    @Test fun syncWireContractPreservesIdAndOwnerToken()= runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"clientOperationId":"abc","replayed":true}"""))
            val client=ApiClient(server.url("/").toString(),"owner-a") {"token-a"}
            val result=client.create(SyncApi::class.java).apply(client.header(),SavedOperation("abc","luna",false))
            val request=server.takeRequest()
            assertEquals("/api/sync",request.path); assertEquals("Bearer token-a",request.getHeader("Authorization"))
            assertTrue(request.body.readUtf8().contains("\"saved\":false"));assertTrue(result.replayed)
        }
    }
}
