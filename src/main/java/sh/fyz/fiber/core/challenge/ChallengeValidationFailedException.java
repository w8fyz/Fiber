package sh.fyz.fiber.core.challenge;

/**
 * Thrown when an internal failure (I/O, callback) prevents a challenge response from
 * being processed. The user-facing flow remains undisturbed; the controller surfaces
 * a 500 with the wrapped cause.
 */
public class ChallengeValidationFailedException extends RuntimeException {
    public ChallengeValidationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
