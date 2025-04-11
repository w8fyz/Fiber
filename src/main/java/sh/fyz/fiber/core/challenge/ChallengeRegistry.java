package sh.fyz.fiber.core.challenge;

import sh.fyz.fiber.core.challenge.impl.CaptchaChallenge;
import sh.fyz.fiber.core.challenge.impl.EmailVerificationChallenge;
import sh.fyz.fiber.core.challenge.impl.TwoFactorChallenge;

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
        
        // Register built-in challenge types
        registerChallengeType("2FA", TwoFactorChallenge::create);
        registerChallengeType("EMAIL_VERIFICATION", EmailVerificationChallenge::create);
        registerChallengeType("CAPTCHA", CaptchaChallenge::create);
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
        activeChallenges.remove(challengeId);
    }

    /**
     * Cleans up expired challenges
     */
    public void cleanupExpiredChallenges() {
        activeChallenges.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
} 