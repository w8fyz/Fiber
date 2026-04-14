package sh.fyz.fiber.core.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.util.HttpUtil;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionService {

    private final GenericRepository<FiberSession> repository;
    private final long sessionTtlMillis;
    private final ScheduledExecutorService cleanupExecutor;
    private final Cache<String, Optional<FiberSession>> sessionCache;

    public SessionService(GenericRepository<FiberSession> repository, long sessionTtlMillis) {
        this.repository = repository;
        this.sessionTtlMillis = sessionTtlMillis;

        this.sessionCache = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fiber-session-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.HOURS);
    }

    public SessionService(GenericRepository<FiberSession> repository) {
        this(repository, 7 * 24 * 60 * 60 * 1000L);
    }

    public FiberSession createSession(UserAuth user, HttpServletRequest request) {
        String ipAddress = HttpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        FiberSession session = new FiberSession(user.getId(), ipAddress, userAgent, sessionTtlMillis);
        repository.save(session);
        sessionCache.put(session.getSessionId(), Optional.of(session));
        return session;
    }

    public FiberSession getSession(String sessionId) {
        if (sessionId == null) return null;
        Optional<FiberSession> cached = sessionCache.get(sessionId, k -> {
            FiberSession s = repository.findById(k);
            return Optional.ofNullable(s);
        });
        if (cached == null || cached.isEmpty()) return null;
        FiberSession session = cached.get();
        if (!session.isValid()) {
            sessionCache.invalidate(sessionId);
            return null;
        }
        return session;
    }

    public List<FiberSession> getUserSessions(Object userId) {
        return repository.query()
                .where("userId", String.valueOf(userId))
                .where("active", true)
                .findAll()
                .stream()
                .filter(FiberSession::isValid)
                .toList();
    }

    public void invalidate(String sessionId) {
        FiberSession session = repository.findById(sessionId);
        if (session != null) {
            session.setActive(false);
            repository.save(session);
        }
        sessionCache.invalidate(sessionId);
    }

    public void invalidateAllForUser(Object userId) {
        List<FiberSession> sessions = repository.query()
                .where("userId", String.valueOf(userId))
                .where("active", true)
                .findAll();
        for (FiberSession session : sessions) {
            session.setActive(false);
            repository.save(session);
            sessionCache.invalidate(session.getSessionId());
        }
    }

    public void invalidateOtherSessions(Object userId, String currentSessionId) {
        List<FiberSession> sessions = repository.query()
                .where("userId", String.valueOf(userId))
                .where("active", true)
                .findAll();
        for (FiberSession session : sessions) {
            if (!session.getSessionId().equals(currentSessionId)) {
                session.setActive(false);
                repository.save(session);
                sessionCache.invalidate(session.getSessionId());
            }
        }
    }

    public void touchSession(String sessionId) {
        FiberSession session = repository.findById(sessionId);
        if (session != null && session.isValid()) {
            session.setLastAccessedAt(System.currentTimeMillis());
            repository.save(session);
            sessionCache.put(sessionId, Optional.of(session));
        }
    }

    public void cleanupExpired() {
        List<FiberSession> expired = repository.query()
                .where("active", true)
                .findAll()
                .stream()
                .filter(FiberSession::isExpired)
                .toList();
        for (FiberSession session : expired) {
            session.setActive(false);
            repository.save(session);
            sessionCache.invalidate(session.getSessionId());
        }
    }

    public long getSessionTtlMillis() {
        return sessionTtlMillis;
    }
}
