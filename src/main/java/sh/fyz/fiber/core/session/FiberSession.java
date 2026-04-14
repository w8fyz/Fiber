package sh.fyz.fiber.core.session;

import jakarta.persistence.*;
import sh.fyz.architect.entities.IdentifiableEntity;

import java.util.UUID;

@Entity
@Table(name = "fiber_sessions")
public class FiberSession implements IdentifiableEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "last_accessed_at", nullable = false)
    private long lastAccessedAt;

    @Column(name = "expires_at", nullable = false)
    private long expiresAt;

    @Column(nullable = false)
    private boolean active;

    public FiberSession() {}

    public FiberSession(Object userId, String ipAddress, String userAgent, long ttlMillis) {
        this.id = UUID.randomUUID().toString();
        this.userId = String.valueOf(userId);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedAt = this.createdAt;
        this.expiresAt = this.createdAt + ttlMillis;
        this.active = true;
    }

    @Override
    public Object getId() {
        return id;
    }

    public String getSessionId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(long lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isValid() {
        return active && !isExpired();
    }
}
