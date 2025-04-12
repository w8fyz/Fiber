package sh.fyz.fiber.core.challenge;

import java.io.IOException;

/**
 * Callback interface for challenge success and failure scenarios.
 * Implementations can define custom behavior for when a challenge succeeds or fails.
 */
public interface ChallengeCallback {
    /**
     * Called when a challenge is successfully completed
     * @param challenge The challenge that was completed
     */
    default void onSuccess(Challenge challenge) {
        // Default implementation does nothing
    }

    /**
     * Called when a challenge fails
     * @param challenge The challenge that failed
     * @param reason The reason for failure (e.g., "EXPIRED", "INVALID_RESPONSE")
     */
    default void onFailure(Challenge challenge, String reason) throws IOException {
        // Default implementation does nothing
    }
} 