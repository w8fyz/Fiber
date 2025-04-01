package sh.fyz.fiber.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required roles for controllers and endpoints.
 * If not specified, no authentication is required.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /**
     * The roles required to access the controller or endpoint.
     * If empty, no authentication is required.
     */
    String[] value() default {};
} 