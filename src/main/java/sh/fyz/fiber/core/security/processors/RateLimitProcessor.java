package sh.fyz.fiber.core.security.processors;

import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.interceptors.RateLimitInterceptor;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.annotations.auth.IdentifierField;
import sh.fyz.fiber.FiberServer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Field;
import jakarta.servlet.http.HttpServletRequest;

public class RateLimitProcessor {
    public static Object process(Method method, Object[] args, HttpServletRequest request) {
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return null;
        }
        String identifier = request.getRemoteAddr();

        try {
            RateLimitInterceptor.checkRateLimit(identifier, method);
            return null;
        } catch (Exception e) {
            return ResponseEntity.unauthorized(e.getMessage());
        }
    }

    public static void onSuccess(Method method, Object[] args, HttpServletRequest request) {
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return;
        }
        String identifier = request.getRemoteAddr();
        RateLimitInterceptor.resetRateLimit(identifier, method);
    }
} 