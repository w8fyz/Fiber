package sh.fyz.fiber.core.challenge;

import sh.fyz.fiber.core.challenge.impl.EmailVerificationChallenge;
import sh.fyz.fiber.core.challenge.impl.TwoFactorChallenge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

/**
 * Manages the lifecycle of challenges and their storage.
 * This class is responsible for creating, retrieving, and managing challenges.
 */
public class ChallengeManager {
    private final Map<String, Challenge> challenges;

    public ChallengeManager() {
        this.challenges = new ConcurrentHashMap<>();
    }

    /**
     * Retrieves a challenge by its ID
     * @param challengeId The ID of the challenge to retrieve
     * @return The challenge if found, empty otherwise
     */
    public Optional<Challenge> getChallenge(String challengeId) {
        return Optional.ofNullable(challenges.get(challengeId));
    }

    /**
     * Validates a challenge response
     * @param challengeId The ID of the challenge
     * @param response The response to validate
     * @return true if the response is valid, false otherwise
     */
    public boolean validateChallenge(String challengeId, Object response) {
        return getChallenge(challengeId)
                .map(challenge -> {
                    if (challenge.isExpired()) {
                        challenge.setStatus(ChallengeStatus.EXPIRED);
                        return false;
                    }
                    boolean isValid = challenge.validateResponse(response);
                    if (isValid) {
                        challenge.complete();
                    } else {
                        challenge.fail();
                    }
                    return isValid;
                })
                .orElse(false);
    }

    /**
     * Removes a challenge from storage
     * @param challengeId The ID of the challenge to remove
     */
    public void removeChallenge(String challengeId) {
        challenges.remove(challengeId);
    }

    /**
     * Cleans up expired challenges
     */
    public void cleanupExpiredChallenges() {
        challenges.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
} 