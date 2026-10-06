package sh.fyz.fiber;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Errors Jetty raises before the request reaches Fiber's servlet must still be JSON
 * and must not advertise the Jetty version.
 */
public class ErrorHandlerTest extends IntegrationTestBase {

    @Test
    void ambiguousUriIsRejectedAsJson() throws Exception {
        HttpResponse<String> resp = get("/test/a%2Fb", Map.of("Accept", "text/html"));
        assertEquals(400, resp.statusCode());
        assertTrue(resp.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertTrue(resp.body().contains("\"status\":400"), resp.body());
        assertTrue(resp.body().contains("\"message\":\"Ambiguous URI path separator\""), resp.body());
        assertJettyHidden(resp);
    }

    @Test
    void ambiguousUriIsRejectedAsJsonForEveryMethod() throws Exception {
        for (HttpResponse<String> resp : List.of(put("/test/a%2Fb", "{}"), delete("/test/a%2Fb"))) {
            assertEquals(400, resp.statusCode());
            assertTrue(resp.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
            assertTrue(resp.body().contains("\"status\":400"), resp.body());
            assertTrue(resp.body().contains("\"message\":\"Ambiguous URI path separator\""), resp.body());
            assertJettyHidden(resp);
        }
    }

    @Test
    void oversizedHeaderIsRejectedAsJson() throws Exception {
        HttpResponse<String> resp = get("/test/hello", Map.of("X-Big", "a".repeat(20_000)));
        assertEquals(431, resp.statusCode());
        assertTrue(resp.body().contains("\"status\":431"), resp.body());
        assertJettyHidden(resp);
    }

    private static void assertJettyHidden(HttpResponse<String> resp) {
        assertFalse(resp.headers().firstValue("Server").orElse("").contains("Jetty"));
        assertFalse(resp.body().contains("Jetty"), resp.body());
    }
}
