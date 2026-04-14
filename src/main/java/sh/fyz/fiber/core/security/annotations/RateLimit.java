package sh.fyz.fiber.core.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Rate-limits an endpoint. Supports fixed-window and sliding-window strategies,
 * keyed by IP or by authenticated user.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** Maximum number of attempts allowed within the time window. */
    int attempts() default 5;

    /** Time window duration. */
    int timeout() default 15;

    /** Time unit for the timeout. */
    TimeUnit unit() default TimeUnit.MINUTES;

    /** Custom error message when rate limit is exceeded. */
    String message() default "Too many attempts. Please try again later.";

    /** If true, rate-limit per authenticated user ID instead of per IP. Falls back to IP if unauthenticated. */
    boolean perUser() default false;

    /** Use sliding window algorithm instead of fixed window. More accurate but slightly more memory. */
    boolean slidingWindow() default false;

    /** Optional logical key to group multiple endpoints under the same rate-limit bucket. */
    String key() default "";
}
