package sh.fyz.fiber.annotations.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required roles for controllers and endpoints.
 * If not specified, no role is required.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /**
     * The roles required to access the controller or endpoint.
     * If empty, no role is required.
     */
    String[] value() default {};
} 