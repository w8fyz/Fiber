package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SessionTest extends IntegrationTestBase {

    private static Map<String, String> session1Cookies;
    private static Map<String, String> session2Cookies;
    private static final String USERNAME = "sessuser" + RUN_ID;

    @Test
    @Order(1)
    void testSetupSessionUser() throws Exception {
        registerUser(USERNAME, USERNAME + "@test.com", "pass123", "user");
        HttpResponse<String> loginResp = loginUser(USERNAME, "pass123");
        assertEquals(200, loginResp.statusCode(), "Login failed: " + loginResp.body());
        session1Cookies = extractCookies(loginResp);
        assertFalse(session1Cookies.isEmpty());
    }

    @Test
    @Order(2)
    void testGetCurrentSession() throws Exception {
        assertNotNull(session1Cookies, "Session cookies should be set");
        HttpResponse<String> resp = get("/test/session",
                Map.of("Cookie", cookieHeader(session1Cookies)));
        assertEquals(200, resp.statusCode(), "Get session failed: " + resp.body());
        assertTrue(resp.body().contains("\"sessionId\""));
        assertTrue(resp.body().contains("\"userId\""));
        assertTrue(resp.body().contains("\"ipAddress\""));
    }

    @Test
    @Order(3)
    void testGetAllSessions() throws Exception {
        HttpResponse<String> resp = get("/test/sessions",
                Map.of("Cookie", cookieHeader(session1Cookies)));
        assertEquals(200, resp.statusCode(), "Get sessions failed: " + resp.body());
        assertTrue(resp.body().contains("\"sessionId\""));
    }

    @Test
    @Order(4)
    void testSecondLoginCreatesNewSession() throws Exception {
        HttpResponse<String> loginResp = loginUser(USERNAME, "pass123");
        assertEquals(200, loginResp.statusCode());
        session2Cookies = extractCookies(loginResp);

        HttpResponse<String> sessionsResp = get("/test/sessions",
                Map.of("Cookie", cookieHeader(session2Cookies)));
        assertEquals(200, sessionsResp.statusCode(), "Get sessions failed: " + sessionsResp.body());

        String body = sessionsResp.body();
        int count = countOccurrences(body, "\"sessionId\"");
        assertTrue(count >= 2, "Should have at least 2 active sessions, got: " + count + " body: " + body);
    }

    @Test
    @Order(5)
    void testInvalidateSession() throws Exception {
        assertNotNull(session2Cookies);
        HttpResponse<String> sessionsResp = get("/test/sessions",
                Map.of("Cookie", cookieHeader(session2Cookies)));
        String body = sessionsResp.body();

        String sessionIdToInvalidate = extractFirstSessionId(body);
        assertNotNull(sessionIdToInvalidate, "Should find a session ID to invalidate");

        HttpResponse<String> invalidateResp = post("/test/sessions/" + sessionIdToInvalidate + "/invalidate", "{}",
                Map.of("Cookie", cookieHeader(session2Cookies)));
        assertEquals(200, invalidateResp.statusCode(), "Invalidate failed: " + invalidateResp.body());
    }

    @Test
    @Order(6)
    void testSessionCountAfterInvalidation() throws Exception {
        HttpResponse<String> sessionsResp = get("/test/sessions",
                Map.of("Cookie", cookieHeader(session2Cookies)));
        assertEquals(200, sessionsResp.statusCode(), "Sessions after invalidation failed: " + sessionsResp.body());
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    private String extractFirstSessionId(String json) {
        int idx = json.indexOf("\"sessionId\"");
        if (idx == -1) return null;
        int colonIdx = json.indexOf(":", idx);
        if (colonIdx == -1) return null;
        int quoteStart = json.indexOf("\"", colonIdx + 1);
        if (quoteStart == -1) return null;
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd == -1) return null;
        return json.substring(quoteStart + 1, quoteEnd);
    }
}
