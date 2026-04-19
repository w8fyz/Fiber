package sh.fyz.fiber.core.email;

/**
 * Thrown by {@code EmailService.sendEmail(...)} when SMTP delivery fails. Wrapped in the
 * returned {@code CompletableFuture}'s exceptional completion so callers can surface a
 * meaningful error instead of a swallowed stack trace.
 */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
