package sh.fyz.fiber.core.challenge.impl;

import sh.fyz.fiber.core.challenge.AbstractChallenge;
import sh.fyz.fiber.core.challenge.Challenge;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

/**
 * Represents a CAPTCHA challenge.
 * This challenge type requires users to solve a simple CAPTCHA.
 */
public class CaptchaChallenge extends AbstractChallenge {
    private static final long EXPIRATION_MINUTES = 10;
    private final String captchaText;
    private final String captchaImage;

    private CaptchaChallenge(String userId, String captchaText, String captchaImage) {
        super("CAPTCHA", userId, Instant.now().plusSeconds(EXPIRATION_MINUTES * 60));
        this.captchaText = captchaText;
        this.captchaImage = captchaImage;
        addMetadata("captchaText", captchaText);
        addMetadata("captchaImage", captchaImage);
    }

    /**
     * Creates a new CaptchaChallenge
     * @param params Map containing challenge parameters
     * @return A new CaptchaChallenge instance
     */
    public static Challenge create(Map<String, Object> params) {
        String userId = (String) params.get("userId");
        if (userId == null) {
            throw new IllegalArgumentException("userId is required for CAPTCHA challenge");
        }
        
        // Generate a random CAPTCHA text (in a real implementation, this would be more complex)
        String captchaText = generateCaptchaText();
        // In a real implementation, this would generate an actual image
        String captchaImage = "data:image/png;base64,..."; // Placeholder for actual image data
        
        return new CaptchaChallenge(userId, captchaText, captchaImage);
    }

    private static String generateCaptchaText() {
        Random random = new Random();
        StringBuilder captcha = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < 6; i++) {
            captcha.append(chars.charAt(random.nextInt(chars.length())));
        }
        return captcha.toString();
    }

    @Override
    public boolean validateResponse(Object response) {
        if (!(response instanceof String)) {
            return false;
        }
        String userResponse = (String) response;
        // In a real implementation, this might be case-insensitive or have other validation rules
        return captchaText.equals(userResponse);
    }

    public String getCaptchaText() {
        return captchaText;
    }

    public String getCaptchaImage() {
        return captchaImage;
    }
} 