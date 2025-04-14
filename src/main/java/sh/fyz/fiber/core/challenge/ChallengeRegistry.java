package sh.fyz.fiber.core.challenge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.challenge.impl.EmailVerificationChallenge;
import sh.fyz.fiber.core.challenge.impl.TwoFactorChallenge;

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
    private final Map<String, Function<Map<String, Object>, Challenge>> challengeCreators;
    private final Map<String, Challenge> activeChallenges;

    public ChallengeRegistry() {
        this.challengeCreators = new ConcurrentHashMap<>();
        this.activeChallenges = new ConcurrentHashMap<>();
    }

    /**
     * Registers a new challenge type with its creation function
     * @param type The type identifier for the challenge
     * @param creator Function that creates a new challenge instance
     */
    public void registerChallengeType(String type, Function<Map<String, Object>, Challenge> creator) {
        challengeCreators.put(type.toUpperCase(), creator);
    }

    /**
     * Creates a new challenge of the specified type
     * @param type The type of challenge to create
     * @param params Parameters needed for challenge creation
     * @param callback Optional callback for challenge success/failure
     * @return The created challenge
     * @throws IllegalArgumentException if the challenge type is not registered
     */
    public Challenge createChallenge(String type, Map<String, Object> params, ChallengeCallback callback) {
        Function<Map<String, Object>, Challenge> creator = challengeCreators.get(type.toUpperCase());
        if (creator == null) {
            throw new IllegalArgumentException("Unsupported challenge type: " + type);
        }
        
        Challenge challenge = creator.apply(params);
        if (callback != null) {
            challenge.setCallback(callback);
        }
        activeChallenges.put(challenge.getId(), challenge);
        return challenge;
    }

    /**
     * Creates a new challenge of the specified type without a callback
     * @param type The type of challenge to create
     * @param params Parameters needed for challenge creation
     * @return The created challenge
     * @throws IllegalArgumentException if the challenge type is not registered
     */
    public Challenge createChallenge(String type, Map<String, Object> params) {
        return createChallenge(type, params, null);
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
                    System.out.println("|---> Challenge ID: " + challengeId);
                    if (challenge.isExpired()) {
                        System.out.println("|---> Challenge expired");
                        try {
                            challenge.setStatus(ChallengeStatus.EXPIRED, request, httpResponse);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                    boolean isValid = challenge.validateResponse(response);
                    if (isValid) {
                        System.out.println("|---> Challenge completed");
                        return challenge.complete(request, httpResponse);
                    } else {
                        try {
                            System.out.println("|---> Challenge failed");
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