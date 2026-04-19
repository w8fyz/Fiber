package sh.fyz.fiber.unit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.challenge.Challenge;
import sh.fyz.fiber.core.challenge.ChallengeCallback;
import sh.fyz.fiber.core.challenge.ChallengeNotFoundException;
import sh.fyz.fiber.core.challenge.ChallengeRegistry;
import sh.fyz.fiber.core.challenge.ChallengeStatus;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeRegistryTest {

    private static class TestChallenge implements Challenge {
        private final String id;
        private final String expected;
        private ChallengeStatus status = ChallengeStatus.PENDING;
        private ChallengeCallback callback;
        boolean expired;

        TestChallenge(String id, String expected) {
            this.id = id;
            this.expected = expected;
        }

        @Override public String getId() { return id; }
        @Override public Object getUserId() { return "user1"; }
        @Override public Instant getCreatedAt() { return Instant.now(); }
        @Override public Instant getExpiresAt() { return Instant.now().plusSeconds(60); }
        @Override public ChallengeStatus getStatus() { return status; }
        @Override public Map<String, Object> getMetadata() { return Map.of(); }
        @Override public boolean validateResponse(Object response) { return expected.equals(response); }
        @Override public ResponseEntity<Object> complete(HttpServletRequest request, HttpServletResponse response) {
            return ResponseEntity.ok("done");
        }
        @Override public ResponseEntity<Object> fail(HttpServletRequest request, HttpServletResponse response) {
            return ResponseEntity.badRequest("failed");
        }
        @Override public boolean isExpired() { return expired; }
        @Override public void setStatus(ChallengeStatus s, HttpServletRequest req, HttpServletResponse resp) {
            this.status = s;
        }
        @Override public ChallengeCallback getCallback() { return callback; }
        @Override public void setCallback(ChallengeCallback callback) { this.callback = callback; }
        @Override public Map<String, Object> asDTO() { return Map.of("id", id); }
    }

    @Test
    void unknownChallengeThrowsNotFound() {
        ChallengeRegistry registry = new ChallengeRegistry();
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        assertThrows(ChallengeNotFoundException.class,
                () -> registry.validateChallenge("missing", "ignored", req, resp));
    }

    @Test
    void validResponseCompletesChallenge() {
        ChallengeRegistry registry = new ChallengeRegistry();
        TestChallenge challenge = new TestChallenge("c1", "42");
        registry.createChallenge(challenge, null);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        ResponseEntity<Object> r = registry.validateChallenge("c1", "42", req, resp);
        assertEquals("done", r.getBody());
    }

    @Test
    void invalidResponseProducesFailure() {
        ChallengeRegistry registry = new ChallengeRegistry();
        TestChallenge challenge = new TestChallenge("c-bad", "42");
        registry.createChallenge(challenge, null);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        ResponseEntity<Object> r = registry.validateChallenge("c-bad", "99", req, resp);
        assertEquals("failed", r.getBody());
    }

    @Test
    void expiredChallengeReturnsNull() {
        ChallengeRegistry registry = new ChallengeRegistry();
        TestChallenge challenge = new TestChallenge("c2", "42");
        challenge.expired = true;
        registry.createChallenge(challenge, null);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
        assertNull(registry.validateChallenge("c2", "42", req, resp));
    }
}
