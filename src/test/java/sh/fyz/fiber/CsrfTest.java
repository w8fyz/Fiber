package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CsrfTest extends IntegrationTestBase {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-CSRF-TOKEN";
    private static final String ALLOWED_ORIGIN = "http://allowed-origin.com";

    @Test
    @Order(1)
    void testGetSafeMethodPassesWithoutToken() throws Exception {
        HttpResponse<String> resp = get("/test/hello");
        assertEquals(200, resp.statusCode());
    }

    @Test
    @Order(2)
    void testNoCsrfAnnotationBypassesProtection() throws Exception {
        HttpResponse<String> resp = post("/test/no-csrf", "{}");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("bypassed"));
    }

    @Test
    @Order(10)
    void testTokenEndpointReturnsCookieAndHeader() throws Exception {
        HttpResponse<String> resp = get("/internal/csrf/token");
        assertEquals(200, resp.statusCode());

        String headerToken = resp.headers().firstValue(CSRF_HEADER).orElse(null);
        assertNotNull(headerToken, "Response must carry the X-CSRF-TOKEN header");
        assertTrue(headerToken.contains("."), "Token should be a signed HMAC (<nonce>.<sig>)");

        String cookieToken = extractCookies(resp).getOrDefault(CSRF_COOKIE, "");
        assertEquals(headerToken, cookieToken,
                "Cookie XSRF-TOKEN must match the header value (double-submit pattern)");
    }

    @Test
    @Order(11)
    void testCsrfTokenIsExposedToCrossOriginJs() throws Exception {
        HttpResponse<String> resp = get("/internal/csrf/token", Map.of("Origin", ALLOWED_ORIGIN));
        assertEquals(200, resp.statusCode());

        String exposed = resp.headers().firstValue("Access-Control-Expose-Headers").orElse("");
        assertTrue(exposed.toLowerCase().contains("x-csrf-token"),
                "Expected Access-Control-Expose-Headers to list X-CSRF-TOKEN, got '" + exposed + "'");
    }

    @Test
    @Order(12)
    void testGlobalMiddlewareRunsOnCsrfTokenEndpoint() throws Exception {
        HttpResponse<String> resp = get("/internal/csrf/token");
        assertEquals(200, resp.statusCode());
        assertEquals("true",
                resp.headers().firstValue("X-Test-Middleware-Ran").orElse(""),
                "Middleware added after enableCSRFProtection must still run on /internal/csrf/token");
    }

    @Test
    @Order(13)
    void testValidTokenAllowsProtectedPost() throws Exception {
        CsrfCredentials creds = fetchCsrfCredentials();

        HttpResponse<String> resp = post("/test/csrf-protected", "{}",
                Map.of(
                        CSRF_HEADER, creds.token,
                        "Cookie", CSRF_COOKIE + "=" + creds.cookie,
                        "Origin", ALLOWED_ORIGIN));

        assertEquals(200, resp.statusCode(), "Body: " + resp.body());
        assertTrue(resp.body().contains("validated"));
    }

    @Test
    @Order(20)
    void testAttackPostWithoutAnyToken() throws Exception {
        HttpResponse<String> resp = post("/test/csrf-protected", "{}",
                Map.of("Origin", ALLOWED_ORIGIN));

        assertEquals(403, resp.statusCode());
        assertTrue(resp.body().toLowerCase().contains("csrf"),
                "Error body should mention CSRF, got: " + resp.body());
    }

    @Test
    @Order(21)
    void testAttackPostWithOnlyCookieButNoHeader() throws Exception {
        CsrfCredentials creds = fetchCsrfCredentials();

        HttpResponse<String> resp = post("/test/csrf-protected", "{}",
                Map.of(
                        "Cookie", CSRF_COOKIE + "=" + creds.cookie,
                        "Origin", ALLOWED_ORIGIN));

        assertEquals(403, resp.statusCode());
        assertTrue(resp.body().toLowerCase().contains("csrf"));
    }

    @Test
    @Order(22)
    void testAttackPostWithOnlyHeaderButNoCookie() throws Exception {
        CsrfCredentials creds = fetchCsrfCredentials();

        HttpResponse<String> resp = post("/test/csrf-protected", "{}",
                Map.of(
                        CSRF_HEADER, creds.token,
                        "Origin", ALLOWED_ORIGIN));

        assertEquals(403, resp.statusCode());
        assertTrue(resp.body().toLowerCase().contains("csrf"));
    }

    @Test
    @Order(23)
    void testAttackPostWithMismatchedHeaderAndCookie() throws Exception {
        CsrfCredentials first = fetchCsrfCredentials();
        CsrfCredentials second = fetchCsrfCredentials();
        assertNotEquals(first.token, second.token);

        HttpResponse<String> resp = post("/test/csrf-protected", "{}",
                Map.of(
                        CSRF_HEADER, first.token,
                        "Cookie", CSRF_COOKIE + "=" + second.cookie,
                        "Origin", ALLOWED_ORIGIN));

        assertEquals(403, resp.statusCode());
        assertTrue(resp.body().toLowerCase().contains("csrf"));
    }

    @Test
    @Order(24)
    void testAttackPostWithForgedToken() throws Exception {
        String forged = "attackerNonce.attackerSignature";

        HttpResponse<String> resp = post("/test/csrf-protected", "{}",
                Map.of(
                        CSRF_HEADER, forged,
                        "Cookie", CSRF_COOKIE + "=" + forged,
                        "Origin", ALLOWED_ORIGIN));

        assertEquals(403, resp.statusCode());
        assertTrue(resp.body().toLowerCase().contains("csrf"));
    }

    @Test
    @Order(25)
    void testAttackPostWithTamperedSignature() throws Exception {
        CsrfCredentials creds = fetchCsrfCredentials();
        int dot = creds.token.indexOf('.');
        assertTrue(dot > 0);
        String tampered = creds.token.substring(0, dot + 1) + "AAAA" + creds.token.substring(dot + 1 + 4);

        HttpResponse<String> resp = post("/test/csrf-protected", "{}",
                Map.of(
                        CSRF_HEADER, tampered,
                        "Cookie", CSRF_COOKIE + "=" + tampered,
                        "Origin", ALLOWED_ORIGIN));

        assertEquals(403, resp.statusCode());
        assertTrue(resp.body().toLowerCase().contains("csrf"));
    }

    @Test
    @Order(26)
    void testAttackReplayAfterTokenRotation() throws Exception {
        CsrfCredentials stolen = fetchCsrfCredentials();
        CsrfCredentials fresh = fetchCsrfCredentials();

        HttpResponse<String> resp = post("/test/csrf-protected", "{}",
                Map.of(
                        CSRF_HEADER, stolen.token,
                        "Cookie", CSRF_COOKIE + "=" + fresh.cookie,
                        "Origin", ALLOWED_ORIGIN));

        assertEquals(403, resp.statusCode());
        assertTrue(resp.body().toLowerCase().contains("csrf"));
    }

    private CsrfCredentials fetchCsrfCredentials() throws Exception {
        HttpResponse<String> resp = get("/internal/csrf/token");
        assertEquals(200, resp.statusCode());
        String header = resp.headers().firstValue(CSRF_HEADER).orElseThrow(
                () -> new AssertionError("Missing X-CSRF-TOKEN response header"));
        String cookie = extractCookies(resp).getOrDefault(CSRF_COOKIE, "");
        assertFalse(cookie.isBlank(), "Missing XSRF-TOKEN cookie on /internal/csrf/token response");
        return new CsrfCredentials(header, cookie);
    }

    private record CsrfCredentials(String token, String cookie) {}
}
