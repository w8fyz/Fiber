package sh.fyz.fiber.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to extract path variables from the request URL.
 * The variable name must match the placeholder in the path.
 * Example: @RequestMapping("/users/{userId}") with @PathVariable("userId")
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVariable {
    /**
     * The name of the path variable to extract
     */
    String value();
} 