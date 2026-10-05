package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CorsTest extends IntegrationTestBase {

    @Test
    @Order(1)
    void testPreflightAllowedOrigin() throws Exception {
        HttpResponse<String> resp = options("/test/hello", Map.of(
                "Origin", "http://allowed-origin.com",
                "Access-Control-Request-Method", "GET"));
        assertEquals(200, resp.statusCode());

        String allowOrigin = resp.headers().firstValue("Access-Control-Allow-Origin").orElse("");
        assertEquals("http://allowed-origin.com", allowOrigin);
    }

    @Test
    @Order(2)
    void testPreflightBlockedOrigin() throws Exception {
        HttpResponse<String> resp = options("/test/hello", Map.of(
                "Origin", "http://evil-origin.com",
                "Access-Control-Request-Method", "GET"));
        assertEquals(403, resp.statusCode());
    }

    @Test
    @Order(3)
    void testCorsHeadersOnGet() throws Exception {
        HttpResponse<String> resp = get("/test/hello",
                Map.of("Origin", "http://allowed-origin.com"));
        assertEquals(200, resp.statusCode());

        String allowOrigin = resp.headers().firstValue("Access-Control-Allow-Origin").orElse("");
        assertEquals("http://allowed-origin.com", allowOrigin);
    }

    @Test
    @Order(4)
    void testGetWithoutOriginHeader() throws Exception {
        HttpResponse<String> resp = get("/test/hello");
        // Should not return 403 — same-origin requests don't have Origin header
        assertEquals(200, resp.statusCode());
    }

    @Test
    @Order(5)
    void testAllowCredentialsHeader() throws Exception {
        HttpResponse<String> resp = options("/test/hello", Map.of(
                "Origin", "http://allowed-origin.com",
                "Access-Control-Request-Method", "GET"));
        assertEquals(200, resp.statusCode());

        String allowCredentials = resp.headers().firstValue("Access-Control-Allow-Credentials").orElse("");
        assertEquals("true", allowCredentials);
    }

    @Test
    @Order(6)
    void testSameOriginRequestIsNotRejected() throws Exception {
        // Browsers send Origin on same-origin POSTs and fetch calls. 127.0.0.1 is not in the
        // allow-list (localhost is), so only the same-origin rule can let this request through.
        String self = "http://127.0.0.1:" + PORT;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(self + "/test/hello"))
                .GET()
                .header("Origin", self)
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.headers().firstValue("Access-Control-Allow-Origin").isEmpty(),
                "CORS headers are not needed for a same-origin request");

        HttpResponse<String> crossOrigin = get("/test/hello", Map.of("Origin", "http://127.0.0.1:1"));
        assertEquals(403, crossOrigin.statusCode(), "Another port is another origin");
    }
}
