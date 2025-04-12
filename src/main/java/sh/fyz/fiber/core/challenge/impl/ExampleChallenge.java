package sh.fyz.fiber.core.challenge.impl;

import sh.fyz.fiber.core.challenge.AbstractChallenge;
import sh.fyz.fiber.core.challenge.Challenge;

import java.time.Instant;
import java.util.Map;

public class ExampleChallenge extends AbstractChallenge {

    protected ExampleChallenge(String userId) {
        super("CACA", userId, Instant.now().plusSeconds(2 * 3600));
    }

    @Override
    public boolean validateResponse(Object response) {
        String test = (String) response;
        return test.equals("test");
    }

    public static Challenge create(Map<String, Object> params) {
        String userId = (String) params.get("userId");
        if (userId == null) {
            throw new IllegalArgumentException("userId is required for 2FA challenge");
        }
        return new ExampleChallenge(userId);
    }
}
