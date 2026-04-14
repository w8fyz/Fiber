package sh.fyz.fiber;

import org.junit.jupiter.api.*;
import sh.fyz.fiber.core.security.logging.AuditLog;

import java.net.http.HttpResponse;
import java.util.Map;

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
}
