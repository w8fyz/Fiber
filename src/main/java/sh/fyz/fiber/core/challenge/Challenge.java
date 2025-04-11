package sh.fyz.fiber.core.challenge;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a challenge that needs to be completed by a user.
 * Challenges can be of various types like 2FA, email verification, etc.
 */
public interface Challenge {
    /**
     * @return The unique identifier for this challenge
     */
    String getId();

    /**
     * @return The type of challenge (e.g., "2FA", "EMAIL_VERIFICATION")
     */
    String getType();

    /**
     * @return The user ID this challenge is associated with
     */
    String getUserId();

    /**
     * @return The timestamp when this challenge was created
     */
    Instant getCreatedAt();

    /**
     * @return The timestamp when this challenge expires
     */
    Instant getExpiresAt();

    /**
     * @return The current status of the challenge
     */
    ChallengeStatus getStatus();

    /**
     * @return Additional metadata specific to the challenge type
     */
    Map<String, Object> getMetadata();

    /**
     * Validates the challenge response
     * @param response The response to validate
     * @return true if the response is valid, false otherwise
     */
    boolean validateResponse(Object response);

    /**
     * Marks the challenge as completed
     */
    void complete();

    /**
     * Marks the challenge as failed
     */
    void fail();

    /**
     * Checks if the challenge has expired
     * @return true if the challenge has expired, false otherwise
     */
    boolean isExpired();

    /**
     * Sets the status of the challenge
     * @param status The new status to set
     */
    void setStatus(ChallengeStatus status);

    /**
     * @return The callback for this challenge, or null if none is set
     */
    ChallengeCallback getCallback();

    /**
     * Sets the callback for this challenge
     * @param callback The callback to set
     */
    void setCallback(ChallengeCallback callback);
} 