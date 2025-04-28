package sh.fyz.fiber.core.challenge.impl;

import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.dto.IgnoreDTO;
import sh.fyz.fiber.core.challenge.AbstractChallenge;
import sh.fyz.fiber.core.challenge.Challenge;
import sh.fyz.fiber.example.email.ExampleVerifyMailEmail;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an Email Verification challenge.
 * This challenge type requires users to verify their email address by clicking a link.
 */
public class EmailVerificationChallenge extends AbstractChallenge {
    @IgnoreDTO
    private static final long EXPIRATION_HOURS = 24;
    @IgnoreDTO
    private final String verificationToken;
    @IgnoreDTO
    private final String email;

    private EmailVerificationChallenge(Object userId, String email, String verificationToken) {
        super("EMAIL_VERIFICATION", userId, Instant.now().plusSeconds(EXPIRATION_HOURS * 3600));
        this.verificationToken = verificationToken;
        this.email = email;
        addMetadata("verificationToken", verificationToken);
        addMetadata("email", email);

        FiberServer.get().getEmailService().sendEmail(new ExampleVerifyMailEmail(email, verificationToken));
    }

    /**
     * Creates a new EmailVerificationChallenge
     * @param params Map containing challenge parameters
     * @return A new EmailVerificationChallenge instance
     */
    public static Challenge create(Map<String, Object> params) {
        Object userId = params.get("userId");
        String email = (String) params.get("email");
        
        if (userId == null) {
            throw new IllegalArgumentException("userId is required for email verification challenge");
        }
        if (email == null) {
            throw new IllegalArgumentException("email is required for email verification challenge");
        }
        
        return new EmailVerificationChallenge(userId, email, "code");
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