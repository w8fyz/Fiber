package sh.fyz.fiber.unit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.challenge.AbstractChallenge;
import sh.fyz.fiber.core.challenge.ChallengeRegistry;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AbstractChallengeTest {

    private static class CodeChallenge extends AbstractChallenge {
        CodeChallenge() {
            super("user1", Instant.now().plusSeconds(60));
        }

        @Override
        public boolean validateResponse(Object response) {
            return "42".equals(response);
        }
    }

    @Test
    void challengeWithoutCallbackAnswersItsOutcomeInsteadOfExpired() {
        ChallengeRegistry registry = new ChallengeRegistry();
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);

        CodeChallenge failing = new CodeChallenge();
        registry.createChallenge(failing, null);
        ResponseEntity<Object> wrong = registry.validateChallenge(failing.getId(), "0", req, resp);
        assertNotNull(wrong, "null is reported as an expired challenge (410)");
        assertEquals(400, wrong.getStatus());

        CodeChallenge passing = new CodeChallenge();
        registry.createChallenge(passing, null);
        ResponseEntity<Object> right = registry.validateChallenge(passing.getId(), "42", req, resp);
        assertNotNull(right, "null is reported as an expired challenge (410)");
        assertEquals(200, right.getStatus());
    }
}
