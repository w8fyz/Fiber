package sh.fyz.fiber.core.authentication.oauth2;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.oauth2.entities.UserOAuth2Token;
import sh.fyz.fiber.util.TokenCrypto;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Persists per-user OAuth2 provider tokens and serves as the authoritative
 * source of access tokens for outgoing provider API calls.
 *
 * <p>Purpose: eliminate redundant {@code /oauth2/token} POSTs against providers
 * (notably Discord, which rate-limits aggressively). After the first login the
 * full token response — access + refresh + expiry — is stored encrypted and
 * reused. When the access token expires, a single refresh-token call renews
 * it; the authorization code flow is only needed if the refresh fails.
 *
 * <p>Both access and refresh tokens are encrypted at rest via
 * {@link TokenCrypto}.
 */
public class UserOAuth2TokenService {

    private static final Logger logger = LoggerFactory.getLogger(UserOAuth2TokenService.class);
    private static final long CLOCK_SKEW_MILLIS = 30_000L;

    private final GenericRepository<UserOAuth2Token> repository;
    private final Cache<String, Optional<UserOAuth2Token>> cache;
    private final ScheduledExecutorService cleanupExecutor;

    public UserOAuth2TokenService(GenericRepository<UserOAuth2Token> repository) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();

        ScheduledExecutorService shared = null;
        try {
            shared = FiberServer.get().getSharedExecutor();
        } catch (Exception ignored) {
            // FiberServer not initialised — fall back to a private virtual-thread executor.
        }
        if (shared != null) {
            this.cleanupExecutor = shared;
        } else {
            this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = Thread.ofVirtual().name("oauth2-token-cleanup-").unstarted(r);
                t.setDaemon(true);
                return t;
            });
        }
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredWithoutRefresh, 1, 1, TimeUnit.HOURS);
    }

    /**
     * Upsert tokens for the given (user, provider) pair. Access and refresh
     * tokens from {@code response} are encrypted before persistence.
     */
    public UserOAuth2Token saveOrUpdate(Object userId, String providerId, OAuth2TokenResponse response) {
        if (userId == null || providerId == null || response == null) {
            return null;
        }
        UserOAuth2Token token = loadRaw(userId, providerId).orElseGet(() -> new UserOAuth2Token(userId, providerId));

        token.setAccessToken(encryptNullable(response.getAccessToken()));
        if (response.getRefreshToken() != null) {
            token.setRefreshToken(encryptNullable(response.getRefreshToken()));
        }
        token.setTokenType(response.getTokenType());
        token.setScope(response.getScope());
        if (response.getExpiresIn() != null) {
            token.setExpiresAt(System.currentTimeMillis() + response.getExpiresIn() * 1000L);
        }
        token.setUpdatedAt(System.currentTimeMillis());

        repository.save(token);
        cache.put(cacheKey(userId, providerId), Optional.of(token));
        return token;
    }

    /**
     * @return the stored token row, if any. Caller typically wants
     *     {@link #getValidAccessToken} instead.
     */
    public Optional<UserOAuth2Token> find(Object userId, String providerId) {
        if (userId == null || providerId == null) {
            return Optional.empty();
        }
        Optional<UserOAuth2Token> cached = cache.get(cacheKey(userId, providerId),
                k -> loadRaw(userId, providerId));
        return cached == null ? Optional.empty() : cached;
    }

    /**
     * Returns a valid (decrypted) access token for the user, refreshing it if
     * necessary.
     *
     * @return the decrypted access token, or {@code null} if no row exists or
     *     refresh failed (caller should route the user through the full auth
     *     flow again in that case).
     */
    public String getValidAccessToken(Object userId, OAuth2Provider<?> provider) {
        if (userId == null || provider == null) {
            return null;
        }
        UserOAuth2Token token = find(userId, provider.getProviderId()).orElse(null);
        if (token == null) {
            return null;
        }
        if (!token.isExpiringWithin(CLOCK_SKEW_MILLIS)) {
            return decryptNullable(token.getAccessToken());
        }
        String refresh = decryptNullable(token.getRefreshToken());
        if (refresh == null) {
            return null;
        }
        try {
            OAuth2TokenResponse refreshed = provider.refreshAccessToken(refresh);
            if (refreshed == null || refreshed.getAccessToken() == null) {
                return null;
            }
            saveOrUpdate(userId, provider.getProviderId(), refreshed);
            return refreshed.getAccessToken();
        } catch (RuntimeException e) {
            logger.warn("[Fiber] OAuth2 refresh failed for user={} provider={}: {}",
                    userId, provider.getProviderId(), e.getMessage());
            return null;
        }
    }

    public void deleteByUserAndProvider(Object userId, String providerId) {
        if (userId == null || providerId == null) {
            return;
        }
        UserOAuth2Token existing = loadRaw(userId, providerId).orElse(null);
        if (existing != null) {
            repository.delete(existing);
        }
        cache.invalidate(cacheKey(userId, providerId));
    }

    public void deleteByUser(Object userId) {
        if (userId == null) {
            return;
        }
        List<UserOAuth2Token> rows = repository.query()
                .where("userId", String.valueOf(userId))
                .findAll();
        for (UserOAuth2Token row : rows) {
            repository.delete(row);
            cache.invalidate(cacheKey(row.getUserId(), row.getProviderId()));
        }
    }

    /**
     * Remove rows whose access token has expired AND whose refresh token is
     * missing — they're unusable and just take space. Rows with a refresh
     * token are kept even past access-token expiry so the next call can
     * refresh them.
     */
    public void cleanupExpiredWithoutRefresh() {
        try {
            List<UserOAuth2Token> rows = repository.query().findAll();
            long now = System.currentTimeMillis();
            for (UserOAuth2Token row : rows) {
                Long exp = row.getExpiresAt();
                boolean expired = exp != null && now >= exp;
                boolean noRefresh = row.getRefreshToken() == null || row.getRefreshToken().isBlank();
                if (expired && noRefresh) {
                    repository.delete(row);
                    cache.invalidate(cacheKey(row.getUserId(), row.getProviderId()));
                }
            }
        } catch (Exception e) {
            logger.warn("[Fiber] OAuth2 token cleanup failed: {}", e.getMessage());
        }
    }

    private Optional<UserOAuth2Token> loadRaw(Object userId, String providerId) {
        try {
            UserOAuth2Token row = repository.query()
                    .where("userId", String.valueOf(userId))
                    .where("providerId", providerId)
                    .findFirst();
            return Optional.ofNullable(row);
        } catch (Exception e) {
            logger.warn("[Fiber] OAuth2 token lookup failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String cacheKey(Object userId, String providerId) {
        return String.valueOf(userId) + "|" + providerId;
    }

    private static String encryptNullable(String value) {
        return value == null ? null : TokenCrypto.encrypt(value);
    }

    private static String decryptNullable(String value) {
        return value == null ? null : TokenCrypto.decrypt(value);
    }

    // Visible for tests.
    void evictFromCache(Object userId, String providerId) {
        cache.invalidate(cacheKey(userId, providerId));
    }

    // Visible for tests / consumers who want to compare stored state.
    boolean isCached(Object userId, String providerId) {
        return Objects.nonNull(cache.getIfPresent(cacheKey(userId, providerId)));
    }
}
