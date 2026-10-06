package sh.fyz.fiber.core.security.processors;

import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.exceptions.RateLimitExceededException;
import sh.fyz.fiber.core.security.interceptors.RateLimitInterceptor;
import sh.fyz.fiber.util.HttpUtil;

import java.lang.reflect.Method;
import java.util.Map;

public class RateLimitProcessor {

    private static String resolveIdentifier(RateLimit rateLimit, Object[] args, HttpServletRequest request) {
        if (rateLimit.perUser()) {
            // Set by the security pipeline once the user is authenticated.
            Object userId = request.getAttribute("userId");
            if (userId != null) return "user:" + userId;
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof UserAuth user) {
                        Object id = user.getId();
                        if (id != null) return "user:" + id;
                    }
                }
            }
        }
        return "ip:" + HttpUtil.getClientIpAddress(request);
    }

    public static Object process(Method method, Object[] args, HttpServletRequest request) {
        return check(RateLimitInterceptor.resolveRateLimit(method), method, args, request);
    }

    /**
     * @return a 429 response when the limit is exceeded, {@code null} otherwise.
     */
    public static ResponseEntity<?> check(RateLimit rateLimit, Method method, Object[] args, HttpServletRequest request) {
        if (rateLimit == null) return null;

        String identifier = resolveIdentifier(rateLimit, args, request);

        try {
            RateLimitInterceptor.checkRateLimit(identifier, method, rateLimit);
            return null;
        } catch (RateLimitExceededException e) {
            Map<String, Object> body = Map.of(
                "status", 429,
                "message", e.getMessage(),
                "retryAfter", e.getRetryAfterSeconds()
            );
            return ResponseEntity.tooManyRequest(body)
                    .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
        }
    }

    public static void onSuccess(Method method, HttpServletRequest request) {
        onSuccess(method, null, request);
    }

    public static void onSuccess(Method method, Object[] args, HttpServletRequest request) {
        reset(RateLimitInterceptor.resolveRateLimit(method), method, args, request);
    }

    public static void reset(RateLimit rateLimit, Method method, Object[] args, HttpServletRequest request) {
        if (rateLimit == null) return;
        String identifier = resolveIdentifier(rateLimit, args, request);
        RateLimitInterceptor.resetRateLimit(identifier, method, rateLimit);
    }
}
