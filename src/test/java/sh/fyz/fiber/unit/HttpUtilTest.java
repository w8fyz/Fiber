package sh.fyz.fiber.unit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sh.fyz.fiber.util.HttpUtil;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HttpUtilTest {

    @AfterEach
    void reset() {
        HttpUtil.setTrustedProxies(null);
        HttpUtil.setProxyHeadersEnabled(true);
    }

    private static HttpServletRequest mockRequest(String remote, String xff, String realIp) {
        HttpServletRequest r = Mockito.mock(HttpServletRequest.class);
        Mockito.when(r.getRemoteAddr()).thenReturn(remote);
        Mockito.when(r.getHeader("X-Forwarded-For")).thenReturn(xff);
        Mockito.when(r.getHeader("X-Real-IP")).thenReturn(realIp);
        return r;
    }

    @Test
    void untrustedProxyReturnsRemoteAddr() {
        HttpUtil.setTrustedProxies(Set.of("10.0.0.1"));
        HttpServletRequest r = mockRequest("203.0.113.5", "1.1.1.1", null);
        assertEquals("203.0.113.5", HttpUtil.getClientIpAddress(r));
    }

    @Test
    void trustedProxyUsesRightmostNonTrustedInChain() {
        HttpUtil.setTrustedProxies(Set.of("10.0.0.1", "10.0.0.2"));
        HttpServletRequest r = mockRequest("10.0.0.1",
                "198.51.100.5, 203.0.113.9, 10.0.0.2",
                null);
        // rightmost non-trusted entry is 203.0.113.9
        assertEquals("203.0.113.9", HttpUtil.getClientIpAddress(r));
    }

    @Test
    void malformedEntriesAreSkipped() {
        HttpUtil.setTrustedProxies(Set.of("10.0.0.1"));
        HttpServletRequest r = mockRequest("10.0.0.1",
                "not-an-ip, 198.51.100.5, unknown",
                null);
        assertEquals("198.51.100.5", HttpUtil.getClientIpAddress(r));
    }

    @Test
    void realIpHeaderTakesPrecedenceWhenValidAndNotTrusted() {
        HttpUtil.setTrustedProxies(Set.of("10.0.0.1"));
        HttpServletRequest r = mockRequest("10.0.0.1", "203.0.113.1", "198.51.100.9");
        assertEquals("198.51.100.9", HttpUtil.getClientIpAddress(r));
    }

    @Test
    void disabledProxyHeadersIgnoresXFF() {
        HttpUtil.setTrustedProxies(Set.of("10.0.0.1"));
        HttpUtil.setProxyHeadersEnabled(false);
        HttpServletRequest r = mockRequest("10.0.0.1", "203.0.113.1", "198.51.100.9");
        assertEquals("10.0.0.1", HttpUtil.getClientIpAddress(r));
    }

    @Test
    void blankXffReturnsRemoteAddr() {
        HttpUtil.setTrustedProxies(Set.of("10.0.0.1"));
        HttpServletRequest r = mockRequest("10.0.0.1", "   ", null);
        assertEquals("10.0.0.1", HttpUtil.getClientIpAddress(r));
    }
}
