package sh.fyz.fiber.example;

import sh.fyz.fiber.core.challenge.Challenge;
import sh.fyz.fiber.core.challenge.ChallengeCallback;
import sh.fyz.fiber.core.challenge.ChallengeRegistry;
import sh.fyz.fiber.core.challenge.ChallengeStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Example demonstrating how to use callbacks with challenges.
 */
public class ChallengeCallbackExample {
    public static void main(String[] args) {
        // Create a challenge registry
        ChallengeRegistry challengeRegistry = new ChallengeRegistry();

        // Example 1: 2FA Challenge with callbacks
        System.out.println("Example 1: 2FA Challenge with callbacks");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", "user123");
        
        // Create a callback for the 2FA challenge
        ChallengeCallback twoFactorCallback = new ChallengeCallback() {
            @Override
            public void onSuccess(Challenge challenge) {
                System.out.println("2FA Challenge succeeded! User " + challenge.getUserId() + " is now authenticated.");
                // In a real application, you might update the user's session or redirect them
            }

            @Override
            public void onFailure(Challenge challenge, String reason) {
                System.out.println("2FA Challenge failed for user " + challenge.getUserId() + ". Reason: " + reason);
                // In a real application, you might show an error message or redirect to a retry page
            }
        };
        
        Challenge twoFactorChallenge = challengeRegistry.createChallenge("2FA", params, twoFactorCallback);
        System.out.println("Created 2FA challenge with ID: " + twoFactorChallenge.getId());
        
        // Simulate validating the challenge with the correct code
        String correctCode = (String) twoFactorChallenge.getMetadata().get("code");
        boolean isValid = challengeRegistry.validateChallenge(twoFactorChallenge.getId(), correctCode);
        System.out.println("Challenge validation result: " + isValid);
        System.out.println();

        // Example 2: Email Verification Challenge with callbacks
        System.out.println("Example 2: Email Verification Challenge with callbacks");
        params = new HashMap<>();
        params.put("userId", "user123");
        params.put("email", "user@example.com");
        
        // Create a callback for the email verification challenge
        ChallengeCallback emailCallback = new ChallengeCallback() {
            @Override
            public void onSuccess(Challenge challenge) {
                System.out.println("Email verification succeeded for " + challenge.getMetadata().get("email"));
                // In a real application, you might update the user's email verification status
            }

            @Override
            public void onFailure(Challenge challenge, String reason) {
                System.out.println("Email verification failed for " + challenge.getMetadata().get("email") + ". Reason: " + reason);
                // In a real application, you might send a new verification email
            }
        };
        
        Challenge emailChallenge = challengeRegistry.createChallenge("EMAIL_VERIFICATION", params, emailCallback);
        System.out.println("Created email verification challenge with ID: " + emailChallenge.getId());
        
        // Simulate validating the challenge with an invalid token
        isValid = challengeRegistry.validateChallenge(emailChallenge.getId(), "invalid-token");
        System.out.println("Challenge validation result: " + isValid);
        System.out.println();

        // Example 3: CAPTCHA Challenge with callbacks
        System.out.println("Example 3: CAPTCHA Challenge with callbacks");
        params = new HashMap<>();
        params.put("userId", "user123");
        
        // Create a callback for the CAPTCHA challenge
        ChallengeCallback captchaCallback = new ChallengeCallback() {
            @Override
            public void onSuccess(Challenge challenge) {
                System.out.println("CAPTCHA verification succeeded for user " + challenge.getUserId());
                // In a real application, you might allow the user to proceed with their action
            }

            @Override
            public void onFailure(Challenge challenge, String reason) {
                System.out.println("CAPTCHA verification failed for user " + challenge.getUserId() + ". Reason: " + reason);
                // In a real application, you might show a new CAPTCHA
            }
        };
        
        Challenge captchaChallenge = challengeRegistry.createChallenge("CAPTCHA", params, captchaCallback);
        System.out.println("Created CAPTCHA challenge with ID: " + captchaChallenge.getId());
        
        // Simulate validating the challenge with the correct CAPTCHA text
        String captchaText = (String) captchaChallenge.getMetadata().get("captchaText");
        isValid = challengeRegistry.validateChallenge(captchaChallenge.getId(), captchaText);
        System.out.println("Challenge validation result: " + isValid);
    }
} 