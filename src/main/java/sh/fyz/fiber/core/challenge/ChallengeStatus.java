package sh.fyz.fiber.core.challenge;

/**
 * Represents the possible states of a challenge.
 */
public enum ChallengeStatus {
    /**
     * Challenge is pending and waiting for user response
     */
    PENDING,

    /**
     * Challenge has been completed successfully
     */
    COMPLETED,

    /**
     * Challenge has failed (e.g., wrong answer, timeout)
     */
    FAILED,

    /**
     * Challenge has expired
     */
    EXPIRED,

    /**
     * Challenge has been cancelled
     */
    CANCELLED
} 