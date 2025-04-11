package sh.fyz.fiber.core.challenge.impl;

import sh.fyz.fiber.core.challenge.AbstractChallenge;

import java.time.Instant;

public class ExampleChallenge extends AbstractChallenge {

    protected ExampleChallenge(String type, String userId, Instant expiresAt) {
        super(type, userId, expiresAt);
    }

    @Override
    public boolean validateResponse(Object response) {
        String test = (String) response;
        return test.equals("test");
    }
}
