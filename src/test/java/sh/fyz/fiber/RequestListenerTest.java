package sh.fyz.fiber;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.log.LogContext;
import sh.fyz.fiber.core.request.CompletedRequest;
import sh.fyz.fiber.core.request.RequestListener;
import sh.fyz.fiber.core.security.interceptors.RateLimitInterceptor;
import sh.fyz.fiber.test.TestController;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class RequestListenerTest extends IntegrationTestBase {

    private final List<CompletedRequest> completed = new CopyOnWriteArrayList<>();
    private final RequestListener recorder = completed::add;

    @BeforeEach
    void registerRecorder() {
        completed.clear();
        server.addRequestListener(recorder);
    }

    @AfterEach
    void unregisterRecorder() {
        server.removeRequestListener(recorder);
    }

    private List<CompletedRequest> completedFor(String method, String path) {
        return completed.stream()
                .filter(r -> method.equals(r.method()) && path.equals(r.path()))
                .toList();
    }

    /**
     * The listener runs as the request completes, which may be just after the client read the
     * response. Also asserts the request was reported exactly once.
     */
    private CompletedRequest awaitCompleted(String method, String path) {
        CompletedRequest request = await().atMost(Duration.ofSeconds(5)).until(
                () -> completedFor(method, path).stream().findFirst().orElse(null),
                Objects::nonNull);
        assertEquals(1, completedFor(method, path).size(), "A request must be reported exactly once");
        return request;
    }

    @Test
    void reportsTheRouteTemplateOfTheMatchedEndpoint() throws Exception {
        long before = System.currentTimeMillis();
        HttpResponse<String> resp = get("/test/users/42");
        assertEquals(200, resp.statusCode());

        CompletedRequest request = awaitCompleted("GET", "/test/users/42");
        assertEquals("/test/users/{id}", request.route());
        assertTrue(request.matched());
        assertEquals(200, request.status());
        assertNotNull(request.requestId());
        assertTrue(request.startedAtMillis() >= before, "startedAtMillis should be when the request was received");
        assertTrue(request.durationNanos() >= 0);
    }

    @Test
    void reportsTheStatusTheEndpointAnswered() throws Exception {
        HttpResponse<String> resp = get("/test/status/418");
        assertEquals(418, resp.statusCode());

        CompletedRequest request = awaitCompleted("GET", "/test/status/418");
        assertEquals(418, request.status());
        assertEquals("/test/status/{code}", request.route());
    }

    @Test
    void reportsAnUnmatchedPathWithoutARoute() throws Exception {
        HttpResponse<String> resp = get("/test/no-such-endpoint");
        assertEquals(404, resp.statusCode());

        CompletedRequest request = awaitCompleted("GET", "/test/no-such-endpoint");
        assertEquals(404, request.status());
        assertNull(request.route());
        assertFalse(request.matched());
    }

    @Test
    void reportsACorsPreflight() throws Exception {
        HttpResponse<String> resp = options("/test/hello", Map.of(
                "Origin", "http://allowed-origin.com",
                "Access-Control-Request-Method", "GET"));
        assertEquals(200, resp.statusCode());

        CompletedRequest request = awaitCompleted("OPTIONS", "/test/hello");
        assertEquals(200, request.status());
        assertNull(request.route(), "A preflight is answered before any endpoint is matched");
    }

    @Test
    void reportsACorsRejectionWithItsRoute() throws Exception {
        HttpResponse<String> resp = get("/test/users/cors", Map.of("Origin", "http://evil-origin.com"));
        assertEquals(403, resp.statusCode());

        CompletedRequest request = awaitCompleted("GET", "/test/users/cors");
        assertEquals(403, request.status());
        assertEquals("/test/users/{id}", request.route(), "The endpoint matched before CORS rejected it");
    }

    @Test
    void reportsAnUnauthenticatedRequest() throws Exception {
        HttpResponse<String> resp = get("/test/protected");
        assertEquals(401, resp.statusCode());

        CompletedRequest request = awaitCompleted("GET", "/test/protected");
        assertEquals(401, request.status());
        assertEquals("/test/protected", request.route());
    }

    @Test
    void reportsARateLimitedRequest() throws Exception {
        RateLimitInterceptor.clearAll();
        try {
            HttpResponse<String> resp = null;
            for (int i = 0; i < 4 && (resp == null || resp.statusCode() != 429); i++) {
                resp = get("/test/rate-limited");
            }
            assertEquals(429, resp.statusCode());

            await().atMost(Duration.ofSeconds(5)).until(() ->
                    completedFor("GET", "/test/rate-limited").stream().anyMatch(r -> r.status() == 429));
            assertTrue(completedFor("GET", "/test/rate-limited").stream()
                    .allMatch(r -> "/test/rate-limited".equals(r.route())));
        } finally {
            RateLimitInterceptor.clearAll();
        }
    }

    @Test
    void reportsAFailingHandlerAsAServerError() throws Exception {
        HttpResponse<String> resp = get("/test/fail");
        assertEquals(500, resp.statusCode());

        CompletedRequest request = awaitCompleted("GET", "/test/fail");
        assertEquals(500, request.status());
        assertEquals("/test/fail", request.route());
    }

    @Test
    void measuresTheTimeSpentHandlingTheRequest() throws Exception {
        HttpResponse<String> resp = get("/test/slow");
        assertEquals(200, resp.statusCode());

        CompletedRequest request = awaitCompleted("GET", "/test/slow");
        assertTrue(request.duration().toMillis() >= TestController.SLOW_ENDPOINT_MILLIS,
                "Duration should cover the handler, was " + request.duration().toMillis() + " ms");
    }

    @Test
    void runsInTheRequestLogContextAndExposesTheRequest() throws Exception {
        Map<String, String> seen = new ConcurrentHashMap<>();
        RequestListener probe = request -> {
            if (!"/test/users/context".equals(request.path())) return;
            seen.put("eventId", request.requestId());
            seen.put("logContextId", Objects.toString(LogContext.requestId()));
            seen.put("userAgent", Objects.toString(request.request().getHeader("User-Agent")));
        };
        server.addRequestListener(probe);
        try {
            assertEquals(200, get("/test/users/context").statusCode());
            await().atMost(Duration.ofSeconds(5)).until(() -> seen.size() == 3);
        } finally {
            server.removeRequestListener(probe);
        }
        assertEquals(seen.get("eventId"), seen.get("logContextId"),
                "requestId should be the id the request's log lines carry");
        assertEquals("FiberTestClient/1.0", seen.get("userAgent"));
    }

    @Test
    void theTextFormNeverCarriesTheQueryString() throws Exception {
        HttpResponse<String> resp = get("/test/users/text?token=query-secret");
        assertEquals(200, resp.statusCode());

        CompletedRequest request = awaitCompleted("GET", "/test/users/text");
        String text = request.toString();
        assertFalse(text.contains("query-secret"), "toString must not expose the query string: " + text);
        assertTrue(text.contains("route=/test/users/{id}"), text);
    }

    @Test
    void aFailingListenerAffectsNeitherTheResponseNorTheOtherListeners() throws Exception {
        RequestListener throwsException = request -> {
            throw new IllegalStateException("Intentional listener failure");
        };
        RequestListener throwsError = request -> {
            throw new AssertionError("Intentional listener error");
        };
        // Registered ahead of the recorder, so the recorder only sees the request if both failures are contained.
        server.removeRequestListener(recorder);
        server.addRequestListener(throwsException);
        server.addRequestListener(throwsError);
        server.addRequestListener(recorder);
        try {
            // An unknown path: its 404 has no body and is still uncommitted when the listeners run.
            HttpResponse<String> notFound = get("/test/no-such-endpoint-listener");
            assertEquals(404, notFound.statusCode());
            HttpResponse<String> ok = get("/test/hello");
            assertEquals(200, ok.statusCode());
            assertTrue(ok.body().contains("hello"));

            assertEquals(404, awaitCompleted("GET", "/test/no-such-endpoint-listener").status());
            assertEquals(200, awaitCompleted("GET", "/test/hello").status());
        } finally {
            server.removeRequestListener(throwsException);
            server.removeRequestListener(throwsError);
        }
    }

    @Test
    void aRemovedListenerIsNoLongerNotified() throws Exception {
        // The probe stays registered: once it has seen the request, every listener has been called.
        List<CompletedRequest> probed = new CopyOnWriteArrayList<>();
        RequestListener probe = probed::add;
        server.removeRequestListener(recorder);
        server.addRequestListener(probe);
        try {
            HttpResponse<String> resp = get("/test/users/removed");
            assertEquals(200, resp.statusCode());
            await().atMost(Duration.ofSeconds(5))
                    .until(() -> probed.stream().anyMatch(r -> "/test/users/removed".equals(r.path())));
        } finally {
            server.removeRequestListener(probe);
        }
        assertTrue(completedFor("GET", "/test/users/removed").isEmpty(), "A removed listener must not be notified");
    }
}
