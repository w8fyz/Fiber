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

    /**
     * Resolves the effective @RateLimit — method-level takes precedence over class-level.
     */
    private static RateLimit resolveRateLimit(Method method) {
        RateLimit rl = method.getAnnotation(RateLimit.class);
        if (rl != null) return rl;
        return method.getDeclaringClass().getAnnotation(RateLimit.class);
    }

    /**
     * Builds the identifier used as rate-limit key. If perUser is true and a UserAuth
     * argument is present, uses userId; otherwise falls back to client IP.
     */
    private static String resolveIdentifier(RateLimit rateLimit, Object[] args, HttpServletRequest request) {
        if (rateLimit.perUser() && args != null) {
            for (Object arg : args) {
                if (arg instanceof UserAuth user) {
                    Object id = user.getId();
                    if (id != null) return "user:" + id;
                }
            }
        }
        return "ip:" + HttpUtil.getClientIpAddress(request);
    }

    public static Object process(Method method, Object[] args, HttpServletRequest request) {
        RateLimit rateLimit = resolveRateLimit(method);
        if (rateLimit == null) return null;

        String identifier = resolveIdentifier(rateLimit, args, request);

        try {
            RateLimitInterceptor.checkRateLimit(identifier, method);
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
        RateLimit rateLimit = resolveRateLimit(method);
        if (rateLimit == null) return;
        String identifier = "ip:" + HttpUtil.getClientIpAddress(request);
        RateLimitInterceptor.resetRateLimit(identifier, method);
    }
}
