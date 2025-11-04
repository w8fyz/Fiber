package sh.fyz.fiber.core.challenge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.ResponseEntity;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.function.Function;

/**
 * Registry for challenge types and their creation functions.
 * This class manages the registration and creation of different challenge types.
 */
public class ChallengeRegistry {
    private final Map<String, Challenge> activeChallenges;

    public ChallengeRegistry() {
        this.activeChallenges = new ConcurrentHashMap<>();
    }
    public Challenge createChallenge(Challenge challenge, ChallengeCallback callback) {
        if (callback != null) {
            challenge.setCallback(callback);
        }
        activeChallenges.put(challenge.getId(), challenge);
        return challenge;
    }

    /**
     * Retrieves a challenge by its ID
     * @param challengeId The ID of the challenge to retrieve
     * @return The challenge if found, empty otherwise
     */
    public Optional<Challenge> getChallenge(String challengeId) {
        return Optional.ofNullable(activeChallenges.get(challengeId));
    }

    /**
     * Validates a challenge response
     * @param challengeId The ID of the challenge
     * @param response The response to validate
     * @return true if the response is valid, false otherwise
     */
    public ResponseEntity<Object> validateChallenge(String challengeId, Object response, HttpServletRequest request, HttpServletResponse httpResponse) {
        return getChallenge(challengeId)
                .map(challenge -> {
                    if (challenge.isExpired()) {
                        try {
                            challenge.setStatus(ChallengeStatus.EXPIRED, request, httpResponse);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                    boolean isValid = challenge.validateResponse(response);
                    if (isValid) {
                        return challenge.complete(request, httpResponse);
                    } else {
                        try {
                            return challenge.fail(request, httpResponse);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                })
                .orElse(null);
    }

    /**
     * Removes a challenge from storage
     * @param challengeId The ID of the challenge to remove
     */
    public void removeChallenge(String challengeId) {
        activeChallenges.remove(challengeId);
    }

    /**
     * Cleans up expired challenges
     */
    public void cleanupExpiredChallenges() {
        activeChallenges.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
} 