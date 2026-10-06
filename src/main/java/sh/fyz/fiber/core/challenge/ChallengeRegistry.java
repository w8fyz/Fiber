package sh.fyz.fiber.core.challenge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.log.FiberLogger;
import sh.fyz.fiber.core.log.FiberLog;
import sh.fyz.fiber.core.ResponseEntity;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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

    private static final FiberLogger logger = FiberLog.get(ChallengeRegistry.class);

    /** Wrong answers accepted before a challenge is discarded. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private final Map<String, Entry> activeChallenges;

    /**
     * Failure counter guarded by the entry's lock, which also serialises verifications. A lock rather
     * than a monitor: the challenge callbacks run under it and may block, which would pin a virtual
     * thread's carrier on JDK 21.
     */
    private static final class Entry {
        final Challenge challenge;
        final ReentrantLock lock = new ReentrantLock();
        int failedAttempts;

        Entry(Challenge challenge) {
            this.challenge = challenge;
        }
    }

    public ChallengeRegistry() {
        this.activeChallenges = new ConcurrentHashMap<>();
    }

    public Challenge createChallenge(Challenge challenge, ChallengeCallback callback) {
        if (callback != null) {
            challenge.setCallback(callback);
        }
        activeChallenges.put(challenge.getId(), new Entry(challenge));
        return challenge;
    }

    /**
     * Retrieves a challenge by its ID
     * @param challengeId The ID of the challenge to retrieve
     * @return The challenge if found, empty otherwise
     */
    public Optional<Challenge> getChallenge(String challengeId) {
        Entry entry = activeChallenges.get(challengeId);
        return Optional.ofNullable(entry == null ? null : entry.challenge);
    }

    /**
     * Validates a challenge response. A challenge is single-use: it is discarded once
     * completed or expired, and after {@link #MAX_FAILED_ATTEMPTS} wrong answers.
     *
     * @return the {@link ResponseEntity} produced by the challenge, or {@code null} if
     *         the challenge has expired (response already written).
     * @throws ChallengeNotFoundException if the id does not match an active challenge.
     * @throws ChallengeValidationFailedException on internal IO errors.
     */
    public ResponseEntity<Object> validateChallenge(String challengeId, Object response,
                                                    HttpServletRequest request,
                                                    HttpServletResponse httpResponse) {
        Entry entry = activeChallenges.get(challengeId);
        if (entry == null) {
            throw new ChallengeNotFoundException(challengeId);
        }

        entry.lock.lock();
        try {
            // A concurrent verification may have consumed the challenge meanwhile.
            if (activeChallenges.get(challengeId) != entry) {
                throw new ChallengeNotFoundException(challengeId);
            }
            Challenge challenge = entry.challenge;

            if (challenge.isExpired()) {
                activeChallenges.remove(challengeId, entry);
                try {
                    challenge.setStatus(ChallengeStatus.EXPIRED, request, httpResponse);
                } catch (IOException e) {
                    logger.error("Failed to mark challenge {} as expired", challengeId, e);
                    throw new ChallengeValidationFailedException("Failed to expire challenge", e);
                }
                return null;
            }

            if (challenge.validateResponse(response)) {
                activeChallenges.remove(challengeId, entry);
                return challenge.complete(request, httpResponse);
            }

            if (++entry.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                activeChallenges.remove(challengeId, entry);
            }
            try {
                return challenge.fail(request, httpResponse);
            } catch (IOException e) {
                logger.error("Failed to mark challenge {} as failed", challengeId, e);
                throw new ChallengeValidationFailedException("Failed to fail challenge", e);
            }
        } finally {
            entry.lock.unlock();
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
        activeChallenges.values().removeIf(entry -> entry.challenge.isExpired());
    }
}
