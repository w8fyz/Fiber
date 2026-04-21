package sh.fyz.fiber.core.authentication.oauth2.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import sh.fyz.architect.entities.IdentifiableEntity;

import java.util.UUID;

/**
 * Persisted per-user OAuth2 provider tokens (access + refresh).
 *
 * <p>One row per {@code (userId, providerId)}. Access and refresh tokens are
 * stored encrypted (see {@code TokenCrypto}); never store plaintext in this
 * entity.
 */
@Entity
@Table(
        name = "user_oauth2_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_provider", columnNames = {"user_id", "provider_id"})
)
public class UserOAuth2Token implements IdentifiableEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    @Column(name = "access_token", length = 4096)
    private String accessToken;

    @Column(name = "refresh_token", length = 4096)
    private String refreshToken;

    @Column(name = "token_type", length = 32)
    private String tokenType;

    @Column(name = "scope", length = 512)
    private String scope;

    /** Absolute expiry in ms epoch. Null = unknown / non-expiring. */
    @Column(name = "expires_at")
    private Long expiresAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public UserOAuth2Token() {
    }

    public UserOAuth2Token(Object userId, String providerId) {
        this.id = UUID.randomUUID().toString();
        this.userId = String.valueOf(userId);
        this.providerId = providerId;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @Override
    public Object getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isExpired() {
        return expiresAt != null && System.currentTimeMillis() >= expiresAt;
    }

    public boolean isExpiringWithin(long skewMillis) {
        return expiresAt != null && System.currentTimeMillis() + skewMillis >= expiresAt;
    }
}
