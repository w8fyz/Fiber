package sh.fyz.fiber.core.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to log audit events.
 * Can be applied to methods to log security-relevant events.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    /**
     * The action being audited
     */
    String action();

    /**
     * Whether to log the method parameters
     */
    boolean logParameters() default true;

    /**
     * Whether to log the method result
     */
    boolean logResult() default true;

    /**
     * Whether to mask sensitive data in logs
     */
    boolean maskSensitiveData() default true;
} 