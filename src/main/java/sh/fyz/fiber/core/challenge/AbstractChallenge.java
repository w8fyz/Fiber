package sh.fyz.fiber.core.challenge;

import com.fasterxml.jackson.databind.util.JSONPObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.dto.DTOConvertible;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract base class for challenges that implements common functionality.
 * Specific challenge types should extend this class.
 */
public abstract class AbstractChallenge extends DTOConvertible implements Challenge {
    private final String id;
    private final String type;
    private final Object userId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private ChallengeStatus status;
    private final Map<String, Object> metadata;
    private ChallengeCallback callback;

    protected AbstractChallenge(String type, Object userId, Instant expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.userId = userId;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.status = ChallengeStatus.PENDING;
        this.metadata = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Object getUserId() {
        return userId;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public ChallengeStatus getStatus() {
        return status;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public ResponseEntity<Object> complete(HttpServletRequest request, HttpServletResponse response) {
        this.status = ChallengeStatus.COMPLETED;
        if (callback != null) {
            return callback.onSuccess(this, request, response);
        }
        return null;
    }

    @Override
    public ResponseEntity<Object> fail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.status = ChallengeStatus.FAILED;
        if (callback != null) {
            return callback.onFailure(this, "INVALID_RESPONSE", request, response);
        }
        return null;
    }

    @Override
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    @Override
    public void setStatus(ChallengeStatus status, HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.status = status;
        if (status == ChallengeStatus.EXPIRED && callback != null) {
            callback.onFailure(this, "EXPIRED", request, response);
        }
    }

    @Override
    public ChallengeCallback getCallback() {
        return callback;
    }

    @Override
    public void setCallback(ChallengeCallback callback) {
        this.callback = callback;
    }

    protected void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
}