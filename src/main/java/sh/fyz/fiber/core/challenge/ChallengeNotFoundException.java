package sh.fyz.fiber.core.challenge;

/**
 * Thrown when a challenge ID submitted to {@link ChallengeRegistry#validateChallenge}
 * does not match any active challenge (unknown, expired and pruned, or never created).
 */
public class ChallengeNotFoundException extends RuntimeException {
    public ChallengeNotFoundException(String challengeId) {
        super("Challenge not found: " + challengeId);
    }
}
