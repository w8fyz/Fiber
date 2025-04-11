package sh.fyz.fiber.core.challenge.impl;

import sh.fyz.fiber.core.challenge.AbstractChallenge;
import sh.fyz.fiber.core.challenge.Challenge;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

/**
 * Represents a Two-Factor Authentication challenge.
 * This challenge type requires users to enter a code sent to their device.
 */
public class TwoFactorChallenge extends AbstractChallenge {
    private static final int CODE_LENGTH = 6;
    private static final long EXPIRATION_MINUTES = 5;
    private final String code;

    private TwoFactorChallenge(String userId, String code) {
        super("2FA", userId, Instant.now().plusSeconds(EXPIRATION_MINUTES * 60));
        this.code = code;
        addMetadata("code", code);
    }

    /**
     * Creates a new TwoFactorChallenge
     * @param params Map containing challenge parameters
     * @return A new TwoFactorChallenge instance
     */
    public static Challenge create(Map<String, Object> params) {
        String userId = (String) params.get("userId");
        if (userId == null) {
            throw new IllegalArgumentException("userId is required for 2FA challenge");
        }
        return new TwoFactorChallenge(userId, generateCode());
    }

    private static String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    @Override
    public boolean validateResponse(Object response) {
        if (!(response instanceof String)) {
            return false;
        }
        String userCode = (String) response;
        return code.equals(userCode);
    }

    public String getCode() {
        return code;
    }
} 