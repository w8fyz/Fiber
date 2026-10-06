package sh.fyz.fiber.core.security.exceptions;

public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(String message) {
        this(message, -1);
    }

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        // No stack trace: thrown on every rejected request, i.e. on exactly the path a flood hits.
        super(message, null, false, false);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
