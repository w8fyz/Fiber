package sh.fyz.fiber.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required permissions for controllers and endpoints.
 * If not specified, no specific permissions are required.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
    /**
     * The permissions required to access the controller or endpoint.
     * If empty, no specific permissions are required.
     */
    String[] value() default {};
} 