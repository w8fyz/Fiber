package sh.fyz.fiber;

import org.junit.jupiter.api.*;
import sh.fyz.fiber.core.JwtUtil;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationTest extends IntegrationTestBase {

    private static Map<String, String> authCookies;
    private static String bearerToken;
    private static final String USERNAME = "authuser" + RUN_ID;

    @Test
    @Order(1)
    void testRegisterUser() throws Exception {
        HttpResponse<String> resp = registerUser(USERNAME, USERNAME + "@test.com", "password123", "user");
        assertEquals(201, resp.statusCode(), "Register failed: " + resp.body());
        assertTrue(resp.body().contains(USERNAME));

        Map<String, String> cookies = extractCookies(resp);
        assertFalse(cookies.isEmpty(), "Should receive auth cookies on register");
        assertTrue(cookies.containsKey("access_token"), "Should have access_token cookie");
        authCookies = cookies;
        bearerToken = cookies.get("access_token");
    }

    @Test
    @Order(2)
    void testRegisterDuplicateUser() throws Exception {
        HttpResponse<String> resp = registerUser(USERNAME, USERNAME + "@test.com", "password123", "user");
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("already exists"));
    }

    @Test
    @Order(3)
    void testLoginUser() throws Exception {
        HttpResponse<String> resp = loginUser(USERNAME, "password123");
        assertEquals(200, resp.statusCode(), "Login failed: " + resp.body());

        Map<String, String> cookies = extractCookies(resp);
        assertTrue(cookies.containsKey("access_token"), "Login should return access_token");
        assertTrue(cookies.containsKey("refresh_token"), "Login should return refresh_token");
        authCookies = cookies;
        bearerToken = cookies.get("access_token");
    }

    @Test
    @Order(4)
    void testLoginInvalidCredentials() throws Exception {
        HttpResponse<String> resp = loginUser(USERNAME, "wrongpassword");
        assertEquals(401, resp.statusCode());
    }

    @Test
    @Order(5)
    void testProtectedEndpointWithCookie() throws Exception {
        assertNotNull(authCookies, "Auth cookies should be set from login");
        HttpResponse<String> resp = get("/test/protected",
                Map.of("Cookie", cookieHeader(authCookies)));
        assertEquals(200, resp.statusCode(),
                "Protected endpoint with cookie failed. Body: " + resp.body());
        assertTrue(resp.body().contains("\"userId\""));
        assertTrue(resp.body().contains("\"role\""));
    }

    @Test
    @Order(6)
    void testProtectedEndpointWithoutAuth() throws Exception {
        HttpResponse<String> resp = get("/test/protected");
        assertEquals(401, resp.statusCode());
    }

    @Test
    @Order(7)
    void testBearerTokenAuth() throws Exception {
        assertNotNull(bearerToken, "Bearer token should be set from login");
        HttpResponse<String> resp = get("/test/bearer",
                Map.of("Authorization", "Bearer " + bearerToken));
        assertEquals(200, resp.statusCode(),
                "Bearer auth failed. Body: " + resp.body());
        assertTrue(resp.body().contains("\"userId\""));
    }

    @Test
    @Order(8)
    void testInvalidBearerToken() throws Exception {
        HttpResponse<String> resp = get("/test/bearer",
                Map.of("Authorization", "Bearer invalid.token.here"));
        assertEquals(401, resp.statusCode());
    }

    @Test
    @Order(9)
    void testLogout() throws Exception {
        assertNotNull(authCookies, "Auth cookies should be set");
        HttpResponse<String> resp = post("/test-auth/logout", "{}",
                Map.of("Cookie", cookieHeader(authCookies)));
        assertEquals(200, resp.statusCode(), "Logout failed: " + resp.body());

        String setCookieHeader = resp.headers().allValues("set-cookie").toString();
        assertTrue(setCookieHeader.contains("Max-Age=0"), "Cookies should be expired on logout");
    }

    @Test
    @Order(10)
    void testLogoutWithoutAuthenticationRevokesSession() throws Exception {
        Map<String, String> cookies = extractCookies(loginUser(USERNAME, "password123"));
        String accessToken = cookies.get("access_token");
        assertEquals(200, get("/test/bearer", Map.of("Authorization", "Bearer " + accessToken)).statusCode());

        HttpResponse<String> resp = post("/test-auth/logout-unauthenticated", "{}",
                Map.of("Cookie", cookieHeader(cookies)));
        assertEquals(200, resp.statusCode(), "Logout failed: " + resp.body());

        assertEquals(401, get("/test/bearer", Map.of("Authorization", "Bearer " + accessToken)).statusCode(),
                "The access token of a logged-out session must be rejected");
    }

    @Test
    @Order(11)
    void testLogoutWithRotatedRefreshTokenKeepsTheSession() throws Exception {
        // The loser of two concurrent refreshes still holds the old, already rotated refresh token.
        // Clearing its cookies must not revoke the session the winner's new tokens are tied to.
        Map<String, String> cookies = extractCookies(loginUser(USERNAME, "password123"));
        String accessToken = cookies.get("access_token");
        assertTrue(JwtUtil.consumeRefreshToken(cookies.get("refresh_token")), "simulates the winning refresh");

        HttpResponse<String> resp = post("/test-auth/logout-unauthenticated", "{}",
                Map.of("Cookie", cookieHeader(cookies)));
        assertEquals(200, resp.statusCode(), "Logout failed: " + resp.body());

        assertEquals(200, get("/test/bearer", Map.of("Authorization", "Bearer " + accessToken)).statusCode(),
                "The session must survive a logout carrying a stale refresh token");
    }

    @Test
    @Order(12)
    void testRefreshTokenCanBeConsumedOnlyOnce() throws Exception {
        String refreshToken = extractCookies(loginUser(USERNAME, "password123")).get("refresh_token");
        assertNotNull(refreshToken);
        assertTrue(JwtUtil.consumeRefreshToken(refreshToken));
        assertFalse(JwtUtil.consumeRefreshToken(refreshToken), "A refresh token is single-use");
        assertFalse(JwtUtil.consumeRefreshToken("forged.token.value"));
    }
}
