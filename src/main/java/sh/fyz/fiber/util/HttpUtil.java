package sh.fyz.fiber.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

public class HttpUtil {

    private static volatile Set<String> trustedProxies = null;

    private HttpUtil() {}

    public static void setTrustedProxies(Set<String> proxies) {
        trustedProxies = proxies == null ? null : Set.copyOf(proxies);
    }

    public static Set<String> getTrustedProxies() {
        return trustedProxies;
    }

    public static String getClientIpAddress(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        Set<String> trusted = trustedProxies;
        if (trusted == null || !trusted.contains(remoteAddr)) {
            return remoteAddr;
        }

        String ipAddress = request.getHeader("X-Real-IP");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            return remoteAddr;
        }
        return ipAddress;
    }
}
