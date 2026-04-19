package sh.fyz.fiber.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * IP resolution helper aware of trusted proxies.
 *
 * <p>Client-IP headers (X-Forwarded-For, X-Real-IP) are only trusted when the direct
 * peer (remoteAddr) is in the explicitly whitelisted {@link #setTrustedProxies} set.
 * The returned IP is the rightmost non-trusted entry of the chain (closest to the
 * client that we actually trust), and each candidate is validated syntactically so a
 * malformed header cannot propagate.</p>
 */
public class HttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    private static volatile Set<String> trustedProxies = null;
    private static volatile boolean proxyHeadersEnabled = true;

    private HttpUtil() {}

    public static void setTrustedProxies(Set<String> proxies) {
        trustedProxies = proxies == null ? null : Set.copyOf(proxies);
    }

    public static Set<String> getTrustedProxies() {
        return trustedProxies;
    }

    /**
     * Globally enable/disable interpretation of {@code X-Forwarded-For} / {@code X-Real-IP}.
     * When disabled, {@link #getClientIpAddress(HttpServletRequest)} always returns
     * {@link HttpServletRequest#getRemoteAddr()} regardless of trusted proxies.
     */
    public static void setProxyHeadersEnabled(boolean enabled) {
        proxyHeadersEnabled = enabled;
    }

    public static boolean isProxyHeadersEnabled() {
        return proxyHeadersEnabled;
    }

    public static String getClientIpAddress(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (!proxyHeadersEnabled) {
            return remoteAddr;
        }

        Set<String> trusted = trustedProxies;
        if (trusted == null || trusted.isEmpty() || !trusted.contains(remoteAddr)) {
            return remoteAddr;
        }

        String realIp = request.getHeader("X-Real-IP");
        if (isValidIp(realIp) && (trusted == null || !trusted.contains(realIp))) {
            return realIp;
        }

        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            return remoteAddr;
        }

        // Parse the chain left-to-right. Validate each entry and drop trailing trusted proxies.
        String[] parts = xff.split(",");
        List<String> valid = new ArrayList<>(parts.length);
        for (String raw : parts) {
            String candidate = raw.trim();
            if (candidate.isEmpty()) continue;
            if (!isValidIp(candidate)) {
                logger.debug("Rejecting malformed X-Forwarded-For entry from {}: '{}'", remoteAddr, candidate);
                continue;
            }
            valid.add(candidate);
        }

        // Walk rightmost first, stripping trusted proxies — the first non-trusted entry is the real client.
        for (int i = valid.size() - 1; i >= 0; i--) {
            String candidate = valid.get(i);
            if (trusted.contains(candidate)) continue;
            return candidate;
        }

        return remoteAddr;
    }

    private static boolean isValidIp(String value) {
        if (value == null || value.isBlank() || "unknown".equalsIgnoreCase(value)) {
            return false;
        }
        try {
            InetAddress.getByName(value);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
