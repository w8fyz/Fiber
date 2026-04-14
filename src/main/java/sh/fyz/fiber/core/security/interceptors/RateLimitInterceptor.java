package sh.fyz.fiber.core.security.interceptors;

import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.exceptions.RateLimitExceededException;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.*;

public class RateLimitInterceptor {

    private static final Map<String, AttemptInfo> fixedAttempts = new ConcurrentHashMap<>();
    private static final Map<String, SlidingWindowInfo> slidingAttempts = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ratelimit-cleanup");
        t.setDaemon(true);
        return t;
    });

    static {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            fixedAttempts.entrySet().removeIf(e -> e.getValue().isExpired());
            slidingAttempts.entrySet().removeIf(e -> e.getValue().isExpired());
        }, 1, 1, TimeUnit.MINUTES);
    }

    //  Fixed window 

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

        public void increment() {
            this.count++;
        }

        public boolean isExceeded() {
            return count > maxAttempts && Instant.now().isBefore(windowStart.plusSeconds(windowSeconds));
        }

        public long retryAfterSeconds() {
            long elapsed = Instant.now().getEpochSecond() - windowStart.getEpochSecond();
            return Math.max(1, windowSeconds - elapsed);
        }

        public void resetIfExpired() {
            if (Instant.now().isAfter(windowStart.plusSeconds(windowSeconds))) {
                count = 1;
                windowStart = Instant.now();
            }
        }

        public boolean isExpired() {
            return Instant.now().isAfter(windowStart.plusSeconds(windowSeconds * 2));
        }
    }

    //  Sliding window 

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

        public boolean tryAcquire() {
            evictExpired();
            if (timestamps.size() >= maxAttempts) {
                return false;
            }
            timestamps.addLast(Instant.now());
            return true;
        }

        public long retryAfterSeconds() {
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

        public boolean isExpired() {
            evictExpired();
            return timestamps.isEmpty();
        }

        public void reset() {
            timestamps.clear();
        }
    }

    public static String buildCacheKey(String identifier, Method method, RateLimit rateLimit) {
        String bucket = rateLimit.key().isEmpty()
                ? method.getDeclaringClass().getName() + ":" + method.getName()
                : rateLimit.key();
        return identifier + ":" + bucket;
    }

    /**
     * @return seconds until the client can retry, or -1 if not rate limited
     */
    public static long checkRateLimit(String identifier, Method method) {
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) return -1;

        String cacheKey = buildCacheKey(identifier, method, rateLimit);

        if (rateLimit.slidingWindow()) {
            SlidingWindowInfo info = slidingAttempts.computeIfAbsent(cacheKey, k -> new SlidingWindowInfo(rateLimit));
            if (!info.tryAcquire()) {
                throw new RateLimitExceededException(rateLimit.message(), info.retryAfterSeconds());
            }
        } else {
            fixedAttempts.compute(cacheKey, (k, info) -> {
                if (info == null) {
                    return new AttemptInfo(rateLimit);
                }
                info.resetIfExpired();
                info.increment();
                if (info.isExceeded()) {
                    throw new RateLimitExceededException(rateLimit.message(), info.retryAfterSeconds());
                }
                return info;
            });
        }
        return -1;
    }

    public static void resetRateLimit(String identifier, Method method) {
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) return;

        String cacheKey = buildCacheKey(identifier, method, rateLimit);
        if (rateLimit.slidingWindow()) {
            SlidingWindowInfo info = slidingAttempts.get(cacheKey);
            if (info != null) info.reset();
        } else {
            fixedAttempts.remove(cacheKey);
        }
    }

    /** Visible for testing — clear all tracked state. */
    public static void clearAll() {
        fixedAttempts.clear();
        slidingAttempts.clear();
    }
}
