package sh.fyz.fiber.example;

import sh.fyz.fiber.core.challenge.Challenge;
import sh.fyz.fiber.core.challenge.ChallengeRegistry;
import sh.fyz.fiber.core.challenge.ChallengeStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Example demonstrating how to use the challenge system.
 */
public class ChallengeExample {
    public static void main(String[] args) {
        // Create a challenge registry
        ChallengeRegistry challengeRegistry = new ChallengeRegistry();

        // Example 1: 2FA Challenge
        System.out.println("Example 1: 2FA Challenge");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", "user123");
        Challenge twoFactorChallenge = challengeRegistry.createChallenge("2FA", params);
        System.out.println("Created 2FA challenge with ID: " + twoFactorChallenge.getId());
        System.out.println("Challenge status: " + twoFactorChallenge.getStatus());
        
        // Simulate validating the challenge
        boolean isValid = challengeRegistry.validateChallenge(twoFactorChallenge.getId(), "123456");
        System.out.println("Challenge validation result: " + isValid);
        System.out.println("Challenge status after validation: " + twoFactorChallenge.getStatus());
        System.out.println();

        // Example 2: Email Verification Challenge
        System.out.println("Example 2: Email Verification Challenge");
        params = new HashMap<>();
        params.put("userId", "user123");
        params.put("email", "user@example.com");
        Challenge emailChallenge = challengeRegistry.createChallenge("EMAIL_VERIFICATION", params);
        System.out.println("Created email verification challenge with ID: " + emailChallenge.getId());
        System.out.println("Challenge status: " + emailChallenge.getStatus());
        
        // Simulate validating the challenge
        isValid = challengeRegistry.validateChallenge(emailChallenge.getId(), "invalid-token");
        System.out.println("Challenge validation result: " + isValid);
        System.out.println("Challenge status after validation: " + emailChallenge.getStatus());
        System.out.println();

        // Example 3: CAPTCHA Challenge
        System.out.println("Example 3: CAPTCHA Challenge");
        params = new HashMap<>();
        params.put("userId", "user123");
        Challenge captchaChallenge = challengeRegistry.createChallenge("CAPTCHA", params);
        System.out.println("Created CAPTCHA challenge with ID: " + captchaChallenge.getId());
        System.out.println("Challenge status: " + captchaChallenge.getStatus());
        
        // Simulate validating the challenge with the correct CAPTCHA text
        String captchaText = (String) captchaChallenge.getMetadata().get("captchaText");
        isValid = challengeRegistry.validateChallenge(captchaChallenge.getId(), captchaText);
        System.out.println("Challenge validation result: " + isValid);
        System.out.println("Challenge status after validation: " + captchaChallenge.getStatus());
        System.out.println();

        // Example 4: Challenge Expiration
        System.out.println("Example 4: Challenge Expiration");
        params = new HashMap<>();
        params.put("userId", "user123");
        Challenge expiredChallenge = challengeRegistry.createChallenge("2FA", params);
        System.out.println("Created challenge with ID: " + expiredChallenge.getId());
        
        // Simulate challenge expiration
        try {
            Thread.sleep(100); // Wait a bit to simulate time passing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        challengeRegistry.cleanupExpiredChallenges();
        System.out.println("Challenge status after cleanup: " + expiredChallenge.getStatus());
    }
} 