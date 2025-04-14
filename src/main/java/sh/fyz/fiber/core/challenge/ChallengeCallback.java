package sh.fyz.fiber.core.challenge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.ResponseEntity;

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
    ResponseEntity<Object> onSuccess(Challenge challenge, HttpServletRequest request, HttpServletResponse response);

    /**
     * Called when a challenge fails
     * @param challenge The challenge that failed
     * @param reason The reason for failure (e.g., "EXPIRED", "INVALID_RESPONSE")
     */
    ResponseEntity<Object> onFailure(Challenge challenge, String reason, HttpServletRequest request, HttpServletResponse response) throws IOException;
} 