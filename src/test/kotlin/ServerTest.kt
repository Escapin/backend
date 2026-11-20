import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {

    @Test
    fun `root endpoint returns 200`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

    @Test
    fun `test endpoint returns 200 without database`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.OK, client.get("/test").status)
    }
}
