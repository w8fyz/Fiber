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

        // Find the identifier parameter using IdentifierField annotation
        String identifier = findIdentifier(method, args, request);
        if (identifier == null) {
            identifier = request.getRemoteAddr(); // Fallback to IP address
        }

        try {
            // Check rate limit
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

        // Find the identifier parameter
        String identifier = findIdentifier(method, args, request);
        if (identifier == null) {
            identifier = request.getRemoteAddr();
        }

        // Reset rate limit on success
        RateLimitInterceptor.resetRateLimit(identifier, method);
    }

    private static String findIdentifier(Method method, Object[] args, HttpServletRequest request) {
        // First try to find a parameter that matches an identifier field
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = param.getName().toLowerCase();
            
            // Get the authentication service to check identifier fields
            AuthenticationService<?> authService = FiberServer.get().getAuthService();
            if (authService != null) {
                Class<?> userClass = authService.getUserClass();
                if (userClass != null) {
                    // Check if this parameter name matches any identifier field
                    for (Field field : userClass.getDeclaredFields()) {
                        if (field.isAnnotationPresent(IdentifierField.class) && 
                            field.getName().toLowerCase().equals(paramName)) {
                            return args[i].toString();
                        }
                    }
                }
            }
        }

        // If no matching parameter found, try to find an identifier in the request body
        for (Object arg : args) {
            if (arg != null) {
                Class<?> argClass = arg.getClass();
                for (Field field : argClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(IdentifierField.class)) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(arg);
                            if (value != null) {
                                return value.toString();
                            }
                        } catch (IllegalAccessException e) {
                            // Ignore and continue
                        }
                    }
                }
            }
        }

        return null;
    }
} 