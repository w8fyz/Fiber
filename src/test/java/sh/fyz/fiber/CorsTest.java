package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;
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
}
