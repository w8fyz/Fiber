package sh.fyz.fiber.unit;

import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.authentication.oauth2.AbstractOAuth2Provider;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class SsrfGuardTest {

    private static void assertBlocked(String ip) throws Exception {
        InetAddress addr = InetAddress.getByName(ip);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AbstractOAuth2Provider.validateResolvedAddress(addr, "test", "https://evil/"));
        assertTrue(ex.getMessage().toLowerCase().contains("forbidden")
                        || ex.getMessage().toLowerCase().contains("ula"),
                "unexpected message: " + ex.getMessage());
    }

    private static void assertAllowed(String ip) throws Exception {
        InetAddress addr = InetAddress.getByName(ip);
        assertDoesNotThrow(() -> AbstractOAuth2Provider.validateResolvedAddress(addr, "test", "https://example/"));
    }

    @Test
    void rfc1918Rejected() throws Exception {
        assertBlocked("10.0.0.1");
        assertBlocked("192.168.1.1");
        assertBlocked("172.16.0.5");
    }

    @Test
    void loopbackRejected() throws Exception {
        assertBlocked("127.0.0.1");
        assertBlocked("::1");
    }

    @Test
    void linkLocalRejected() throws Exception {
        assertBlocked("169.254.169.254"); // AWS metadata service
        assertBlocked("fe80::1");
    }

    @Test
    void ipv6UlaRejected() throws Exception {
        assertBlocked("fd12:3456:789a::1");
        assertBlocked("fc00::1");
    }

    @Test
    void multicastRejected() throws Exception {
        assertBlocked("224.0.0.1");
    }

    @Test
    void globalUnicastAllowed() throws Exception {
        assertAllowed("8.8.8.8");
        assertAllowed("1.1.1.1");
        assertAllowed("2606:4700:4700::1111"); // Cloudflare
    }
}
