package sh.fyz.fiber.core.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation to rate limit method calls.
 * Can be applied to methods to limit the number of calls within a specified time window.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * Maximum number of attempts allowed within the time window
     */
    int attempts() default 5;

    /**
     * Time window duration
     */
    int timeout() default 15;

    /**
     * Time unit for the timeout
     */
    TimeUnit unit() default TimeUnit.MINUTES;

    /**
     * Custom error message when rate limit is exceeded
     */
    String message() default "Too many attempts. Please try again later.";
} 