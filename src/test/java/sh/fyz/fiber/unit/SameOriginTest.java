package sh.fyz.fiber.unit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sh.fyz.fiber.core.security.cors.CorsService;

import static org.junit.jupiter.api.Assertions.*;

/** {@link CorsService#isSameOrigin} backs both the CORS check and the CSRF Origin/Referer check. */
class SameOriginTest {

    private static HttpServletRequest request(String scheme, String host, int port) {
        HttpServletRequest r = Mockito.mock(HttpServletRequest.class);
        Mockito.when(r.getScheme()).thenReturn(scheme);
        Mockito.when(r.getServerName()).thenReturn(host);
        Mockito.when(r.getServerPort()).thenReturn(port);
        return r;
    }

    @Test
    void matchesTheRequestOrigin() {
        assertTrue(CorsService.isSameOrigin(request("http", "localhost", 8080), "http://localhost:8080"));
        assertTrue(CorsService.isSameOrigin(request("https", "example.com", 443), "https://example.com"));
        assertTrue(CorsService.isSameOrigin(request("http", "example.com", 80), "http://example.com"));
        assertTrue(CorsService.isSameOrigin(request("http", "::1", 8080), "http://[::1]:8080"));
    }

    @Test
    void rejectsAnyOtherOrigin() {
        HttpServletRequest r = request("https", "example.com", 443);
        assertFalse(CorsService.isSameOrigin(r, "https://evil.com"));
        assertFalse(CorsService.isSameOrigin(r, "http://example.com"), "scheme differs");
        assertFalse(CorsService.isSameOrigin(r, "https://example.com:8443"), "port differs");
        assertFalse(CorsService.isSameOrigin(r, "https://sub.example.com"));
        assertFalse(CorsService.isSameOrigin(r, "https://example.com.evil.com"));
        assertFalse(CorsService.isSameOrigin(r, null));
    }
}
