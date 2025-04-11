package sh.fyz.fiber.core.challenge.impl;

import sh.fyz.fiber.core.challenge.AbstractChallenge;
import sh.fyz.fiber.core.challenge.Challenge;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an Email Verification challenge.
 * This challenge type requires users to verify their email address by clicking a link.
 */
public class EmailVerificationChallenge extends AbstractChallenge {
    private static final long EXPIRATION_HOURS = 24;
    private final String verificationToken;
    private final String email;

    private EmailVerificationChallenge(String userId, String email, String verificationToken) {
        super("EMAIL_VERIFICATION", userId, Instant.now().plusSeconds(EXPIRATION_HOURS * 3600));
        this.verificationToken = verificationToken;
        this.email = email;
        addMetadata("verificationToken", verificationToken);
        addMetadata("email", email);
    }

    /**
     * Creates a new EmailVerificationChallenge
     * @param params Map containing challenge parameters
     * @return A new EmailVerificationChallenge instance
     */
    public static Challenge create(Map<String, Object> params) {
        String userId = (String) params.get("userId");
        String email = (String) params.get("email");
        
        if (userId == null) {
            throw new IllegalArgumentException("userId is required for email verification challenge");
        }
        if (email == null) {
            throw new IllegalArgumentException("email is required for email verification challenge");
        }
        
        return new EmailVerificationChallenge(userId, email, UUID.randomUUID().toString());
    }

    @Override
    public boolean validateResponse(Object response) {
        if (!(response instanceof String)) {
            return false;
        }
        String token = (String) response;
        return verificationToken.equals(token);
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public String getEmail() {
        return email;
    }
} 