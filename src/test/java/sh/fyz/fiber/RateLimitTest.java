package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RateLimitTest extends IntegrationTestBase {

    @Test
    @Order(1)
    void testRateLimitAllowedRequests() throws Exception {
        for (int i = 0; i < 3; i++) {
            HttpResponse<String> resp = get("/test/rate-limited");
            assertEquals(400, resp.statusCode(), "Request " + (i + 1) + " should reach the handler");
        }
    }

    @Test
    @Order(2)
    void testRateLimitExceeded() throws Exception {
        HttpResponse<String> resp = get("/test/rate-limited");
        assertEquals(429, resp.statusCode(), "4th request should be rate limited, body: " + resp.body());
    }

    @Test
    @Order(3)
    void testRetryAfterHeader() throws Exception {
        HttpResponse<String> resp = get("/test/rate-limited");
        assertEquals(429, resp.statusCode());
        String retryAfter = resp.headers().firstValue("Retry-After").orElse(null);
        assertNotNull(retryAfter, "429 response should include Retry-After header");
        assertTrue(Long.parseLong(retryAfter) > 0, "Retry-After should be a positive number");
    }

    @Test
    @Order(4)
    void testRetryAfterInBody() throws Exception {
        HttpResponse<String> resp = get("/test/rate-limited");
        assertEquals(429, resp.statusCode());
        assertTrue(resp.body().contains("\"retryAfter\""), "Body should include retryAfter field");
    }

    @Test
    @Order(5)
    void testDifferentEndpointNotAffected() throws Exception {
        HttpResponse<String> resp = get("/test/hello");
        assertEquals(200, resp.statusCode(), "Non-rate-limited endpoint should still work");
    }

    @Test
    @Order(6)
    void testSlidingWindowRateLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            HttpResponse<String> resp = get("/test/rate-limited-sliding");
            assertEquals(400, resp.statusCode(), "Sliding window request " + (i + 1) + " should reach handler");
        }
        HttpResponse<String> resp = get("/test/rate-limited-sliding");
        assertEquals(429, resp.statusCode(), "Sliding window should block after max attempts, body: " + resp.body());
    }
}
