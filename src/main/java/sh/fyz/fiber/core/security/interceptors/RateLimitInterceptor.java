package sh.fyz.fiber.core.security.interceptors;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.exceptions.RateLimitExceededException;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.*;

public class RateLimitInterceptor {

    private static final int MAX_TRACKED_KEYS = 100_000;

    private static final Cache<String, AttemptInfo> fixedAttempts = Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_KEYS)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    private static final Cache<String, SlidingWindowInfo> slidingAttempts = Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_KEYS)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    // Fixed window — all fields accessed under synchronized(this)
    public static class AttemptInfo {
        private int count;
        private Instant windowStart;
        private final long windowSeconds;
        private final int maxAttempts;
        private final String message;

        public AttemptInfo(RateLimit rateLimit) {
            this.windowSeconds = rateLimit.unit().toSeconds(rateLimit.timeout());
            this.maxAttempts = rateLimit.attempts();
            this.message = rateLimit.message();
            this.count = 1;
            this.windowStart = Instant.now();
        }

        public synchronized void incrementAndCheck() {
            resetIfExpired();
            this.count++;
            if (count > maxAttempts && Instant.now().isBefore(windowStart.plusSeconds(windowSeconds))) {
                long elapsed = Instant.now().getEpochSecond() - windowStart.getEpochSecond();
                long retry = Math.max(1, windowSeconds - elapsed);
                throw new RateLimitExceededException(message, retry);
            }
        }

        private void resetIfExpired() {
            if (Instant.now().isAfter(windowStart.plusSeconds(windowSeconds))) {
                count = 1;
                windowStart = Instant.now();
            }
        }
    }

    // Sliding window — all access synchronized
    public static class SlidingWindowInfo {
        private final Deque<Instant> timestamps = new ConcurrentLinkedDeque<>();
        private final long windowSeconds;
        private final int maxAttempts;
        private final String message;

        public SlidingWindowInfo(RateLimit rateLimit) {
            this.windowSeconds = rateLimit.unit().toSeconds(rateLimit.timeout());
            this.maxAttempts = rateLimit.attempts();
            this.message = rateLimit.message();
        }

        public synchronized boolean tryAcquire() {
            evictExpired();
            if (timestamps.size() >= maxAttempts) {
                return false;
            }
            timestamps.addLast(Instant.now());
            return true;
        }

        public synchronized long retryAfterSeconds() {
            Instant oldest = timestamps.peekFirst();
            if (oldest == null) return 0;
            long elapsed = Instant.now().getEpochSecond() - oldest.getEpochSecond();
            return Math.max(1, windowSeconds - elapsed);
        }

        private void evictExpired() {
            Instant cutoff = Instant.now().minusSeconds(windowSeconds);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
        }

        public synchronized void reset() {
            timestamps.clear();
        }
    }

    public static String buildCacheKey(String identifier, Method method, RateLimit rateLimit) {
        // Lowercase the identifier so that "Bob" and "bob" share the same bucket — without
        // this an attacker can multiply the number of allowed attempts by varying case.
        String normalized = identifier == null ? "" : identifier.toLowerCase(Locale.ROOT);
        String bucket = rateLimit.key().isEmpty()
                ? method.getDeclaringClass().getName() + ":" + method.getName()
                : rateLimit.key();
        return normalized + ":" + bucket;
    }

    public static long checkRateLimit(String identifier, Method method) {
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) return -1;

        String cacheKey = buildCacheKey(identifier, method, rateLimit);

        if (rateLimit.slidingWindow()) {
            SlidingWindowInfo info = slidingAttempts.get(cacheKey, k -> new SlidingWindowInfo(rateLimit));
            if (!info.tryAcquire()) {
                throw new RateLimitExceededException(rateLimit.message(), info.retryAfterSeconds());
            }
        } else {
            AttemptInfo existing = fixedAttempts.getIfPresent(cacheKey);
            if (existing == null) {
                fixedAttempts.put(cacheKey, new AttemptInfo(rateLimit));
            } else {
                existing.incrementAndCheck();
            }
        }
        return -1;
    }

    public static void resetRateLimit(String identifier, Method method) {
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) return;

        String cacheKey = buildCacheKey(identifier, method, rateLimit);
        if (rateLimit.slidingWindow()) {
            SlidingWindowInfo info = slidingAttempts.getIfPresent(cacheKey);
            if (info != null) info.reset();
        } else {
            fixedAttempts.invalidate(cacheKey);
        }
    }

    /** Visible for testing -- clear all tracked state. */
    public static void clearAll() {
        fixedAttempts.invalidateAll();
        slidingAttempts.invalidateAll();
    }
}
