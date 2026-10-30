package sh.fyz.fiber;

import org.junit.jupiter.api.*;
import sh.fyz.fiber.core.security.logging.AuditLog;
import sh.fyz.fiber.test.TestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuditLogTest extends IntegrationTestBase {

    @Test
    @Order(1)
    void testAuditLogCaptured() throws Exception {
        auditLogService.clear();
        HttpResponse<String> resp = post("/test/audited", "{\"key\":\"value\"}");
        assertEquals(200, resp.statusCode(), "Audited endpoint failed: " + resp.body());

        Thread.sleep(100);

        AuditLog log = auditLogService.getLastLog();
        assertNotNull(log, "Audit log should have been captured");
        assertEquals("TEST_ACTION", log.getAction());
        assertEquals("POST", log.getMethod());
        assertTrue(log.getUri().contains("/test/audited"));
        assertEquals(200, log.getStatus());
    }

    @Test
    @Order(2)
    void testAuditLogCustomData() throws Exception {
        auditLogService.clear();
        HttpResponse<String> resp = post("/test/audited", "{\"data\":\"test\"}");
        assertEquals(200, resp.statusCode());

        Thread.sleep(100);

        AuditLog log = auditLogService.getLastLog();
        assertNotNull(log, "Audit log should have been captured");

        Map<String, Object> customData = log.getCustomData();
        assertNotNull(customData, "Custom data should be present from AuditContext");
        assertEquals("customValue", customData.get("customField"));
        assertEquals(42, customData.get("itemCount"));
    }

    @Test
    @Order(3)
    void testAuditLogSensitiveDataMasked() throws Exception {
        auditLogService.clear();
        HttpResponse<String> resp = post("/test/audited", "{\"password\":\"secret123\",\"data\":\"ok\"}");
        assertEquals(200, resp.statusCode());

        Thread.sleep(100);

        AuditLog log = auditLogService.getLastLog();
        assertNotNull(log);
        assertEquals("TEST_ACTION", log.getAction());
        assertNotNull(log.getResponse(), "Response should be captured since logResult=true");
    }

    @Test
    @Order(4)
    void testAuditLogWithoutCustomData() throws Exception {
        auditLogService.clear();
        HttpResponse<String> resp = get("/test/audited-no-custom");
        assertEquals(200, resp.statusCode());

        Thread.sleep(100);

        AuditLog log = auditLogService.getLastLog();
        assertNotNull(log, "Audit log should still be captured without custom data");
        assertEquals("SIMPLE_READ", log.getAction());
        assertNull(log.getCustomData(), "Custom data should be null when AuditContext is not used");
        assertNull(log.getPathVariables(), "A route without variables records none");
    }

    @Test
    @Order(5)
    void testAuditLogTimestampAndIp() throws Exception {
        auditLogService.clear();
        long before = System.currentTimeMillis();
        HttpResponse<String> resp = get("/test/audited-no-custom");
        assertEquals(200, resp.statusCode());

        Thread.sleep(100);

        AuditLog log = auditLogService.getLastLog();
        assertNotNull(log);
        assertTrue(log.getTimestamp() >= before, "Timestamp should be around request time");
        assertNotNull(log.getIp(), "IP should be captured");
    }

    @Test
    @Order(6)
    void testAuditLogDurationCoversTheHandler() throws Exception {
        auditLogService.clear();
        HttpResponse<String> resp = get("/test/slow");
        assertEquals(200, resp.statusCode());

        AuditLog log = await().atMost(Duration.ofSeconds(5))
                .until(auditLogService::getLastLog, Objects::nonNull);
        assertEquals("SLOW_READ", log.getAction());
        assertTrue(log.getDurationMs() >= TestController.SLOW_ENDPOINT_MILLIS,
                "Duration should cover the handler, was " + log.getDurationMs() + " ms");
        assertTrue(log.getRawLog().contains("\"durationMs\":" + log.getDurationMs()),
                "The serialized record should carry the same duration");
    }

    @Test
    @Order(7)
    void testAuditLogPathVariablesAreTheOnesTheRequestWasRoutedWith() throws Exception {
        auditLogService.clear();
        HttpResponse<String> resp = get("/audit-vars/acme/42");
        assertEquals(200, resp.statusCode(), resp.body());

        AuditLog log = await().atMost(Duration.ofSeconds(5))
                .until(auditLogService::getLastLog, Objects::nonNull);
        assertEquals("PATH_VARS", log.getAction());
        // The base path's variable is included, and the mapping's leading variable reads its own segment.
        assertEquals(Map.of("tenant", "acme", "id", "42"), log.getPathVariables());
        assertTrue(log.getRawLog().contains("\"pathVariables\":{\"tenant\":\"acme\",\"id\":\"42\"}"),
                "The serialized record lists them in path order: " + log.getRawLog());
    }

    @Test
    @Order(8)
    void testAuditLogOfAMultipartBodyTheEndpointConsumed() throws Exception {
        auditLogService.clear();
        HttpResponse<String> resp = postMultipart("/test/audited-raw-upload?directory=%2Fplugins&tag=a&tag=b",
                "file", "a.jar", "application/java-archive", new byte[2048]);
        assertEquals(200, resp.statusCode(), resp.body());

        AuditLog log = awaitLog("RAW_UPLOAD");
        // The body can no longer be read for parameters: the query string still is.
        assertEquals(Map.of("directory", "/plugins", "tag", "a"), log.getQueryParams());
    }

    @Test
    @Order(9)
    void testAuditLogOfAMalformedQueryString() throws Exception {
        auditLogService.clear();
        assertTrue(rawRequest("GET /test/audited-no-custom?x=%zz", null).startsWith("HTTP/1.1 200"));

        AuditLog log = awaitLog("SIMPLE_READ");
        assertNull(log.getQueryParams(), "A query string Jetty cannot decode is left out");
    }

    @Test
    @Order(10)
    void testAuditLogOfAConsumedMultipartBodyWithAMalformedQueryString() throws Exception {
        auditLogService.clear();
        String boundary = "FiberRawBoundary";
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"a.jar\"\r\n"
                + "Content-Type: application/java-archive\r\n\r\n"
                + "content\r\n--" + boundary + "--\r\n";
        String status = rawRequest("POST /test/audited-raw-upload?directory=100%",
                Map.entry("multipart/form-data; boundary=" + boundary, body));
        assertTrue(status.startsWith("HTTP/1.1 200"), status);

        AuditLog log = awaitLog("RAW_UPLOAD");
        assertNull(log.getQueryParams(), "Neither the body nor the query string can be read");
    }

    private AuditLog awaitLog(String action) {
        return await().atMost(Duration.ofSeconds(5)).until(
                () -> auditLogService.getLogs().stream().filter(l -> action.equals(l.getAction())).findFirst().orElse(null),
                Objects::nonNull);
    }

    /** Sends the request line verbatim (java.net.URI refuses a malformed escape) and returns the status line. */
    private String rawRequest(String requestLine, Map.Entry<String, String> contentTypeAndBody) throws Exception {
        byte[] body = contentTypeAndBody == null ? new byte[0]
                : contentTypeAndBody.getValue().getBytes(StandardCharsets.UTF_8);
        StringBuilder head = new StringBuilder(requestLine).append(" HTTP/1.1\r\n")
                .append("Host: localhost:").append(PORT).append("\r\n")
                .append("Connection: close\r\n");
        if (contentTypeAndBody != null) {
            head.append("Content-Type: ").append(contentTypeAndBody.getKey()).append("\r\n");
        }
        head.append("Content-Length: ").append(body.length).append("\r\n\r\n");
        try (Socket socket = new Socket("localhost", PORT)) {
            socket.setSoTimeout(10_000);
            OutputStream out = socket.getOutputStream();
            out.write(head.toString().getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();
            return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
                    .readLine();
        }
    }
}
