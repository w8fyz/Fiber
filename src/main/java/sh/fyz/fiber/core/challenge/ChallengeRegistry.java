package sh.fyz.fiber.core.challenge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.fyz.fiber.core.ResponseEntity;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for challenge types and their creation functions.
 *
 * <p>{@link #validateChallenge(String, Object, HttpServletRequest, HttpServletResponse)}
 * raises {@link ChallengeNotFoundException} when the id is unknown and
 * {@link ChallengeValidationFailedException} for any internal IO error during processing.
 * Returning {@code null} now <b>only</b> means the challenge is expired and the response
 * status was already written by the challenge itself.</p>
 */
public class ChallengeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ChallengeRegistry.class);

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
     * Validates a challenge response.
     *
     * @return the {@link ResponseEntity} produced by the challenge, or {@code null} if
     *         the challenge has expired (response already written).
     * @throws ChallengeNotFoundException if the id does not match an active challenge.
     * @throws ChallengeValidationFailedException on internal IO errors.
     */
    public ResponseEntity<Object> validateChallenge(String challengeId, Object response,
                                                    HttpServletRequest request,
                                                    HttpServletResponse httpResponse) {
        Challenge challenge = activeChallenges.get(challengeId);
        if (challenge == null) {
            throw new ChallengeNotFoundException(challengeId);
        }

        if (challenge.isExpired()) {
            try {
                challenge.setStatus(ChallengeStatus.EXPIRED, request, httpResponse);
            } catch (IOException e) {
                logger.error("Failed to mark challenge {} as expired", challengeId, e);
                throw new ChallengeValidationFailedException("Failed to expire challenge", e);
            }
            return null;
        }

        boolean isValid = challenge.validateResponse(response);
        if (isValid) {
            return challenge.complete(request, httpResponse);
        }
        try {
            return challenge.fail(request, httpResponse);
        } catch (IOException e) {
            logger.error("Failed to mark challenge {} as failed", challengeId, e);
            throw new ChallengeValidationFailedException("Failed to fail challenge", e);
        }
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
