package sh.fyz.fiber.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark identifier fields in a user class.
 * These fields can be used to identify a user during login.
 * Multiple fields can be marked as identifiers.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdentifierField {
    /**
     * The name of the identifier field as it will appear in login requests.
     * If not specified, the field name will be used.
     */
    String value() default "";
} 