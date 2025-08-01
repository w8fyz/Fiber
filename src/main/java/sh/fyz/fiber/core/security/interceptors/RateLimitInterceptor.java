package sh.fyz.fiber.core.security.interceptors;

import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.exceptions.RateLimitExceededException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.lang.reflect.Method;

public class RateLimitInterceptor {
    private static final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public static class AttemptInfo {
        private int count;
        private Instant firstAttempt;
        private Instant lastAttempt;
        private final RateLimit rateLimit;

        public AttemptInfo(RateLimit rateLimit) {
            this.rateLimit = rateLimit;
            this.count = 1;
            this.firstAttempt = Instant.now();
            this.lastAttempt = Instant.now();
        }

        @Override
        public String toString() {
            return "AttemptInfo{" +
                    "count=" + count +
                    ", firstAttempt=" + firstAttempt +
                    ", lastAttempt=" + lastAttempt +
                    ", rateLimit=" + rateLimit +
                    '}';
        }

        public void increment() {
            this.count++;
            this.lastAttempt = Instant.now();
        }

        public boolean isExceeded() {
            if (count > rateLimit.attempts()) {
                return Instant.now().isBefore(firstAttempt.plusSeconds(rateLimit.unit().toSeconds(rateLimit.timeout())));
            }
            return false;
        }

        public void resetIfExpired() {
            if (Instant.now().isAfter(firstAttempt.plusSeconds(rateLimit.unit().toSeconds(rateLimit.timeout())))) {
                count = 1;
                firstAttempt = Instant.now();
                lastAttempt = Instant.now();
            }
        }
    }

    public static void checkRateLimit(String key, Method method) {
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return;
        }
        //System.out.println("PUMP");
        for (AttemptInfo attemptInfo : attempts.values()) {
            //System.out.println(attemptInfo);
        }
        //System.out.println("EPUMP");
        //System.out.println("Checking RATELIMIT : ");
        String cacheKey = key + ":" + method.getDeclaringClass().getName() + ":" + method.getName();
        //System.out.println("Cache Key: " + cacheKey);
        attempts.compute(cacheKey, (k, info) -> {
            if (info == null) {
                //System.out.println("Creating new AttemptInfo for key: " + k);
                AttemptInfo newInfo = new AttemptInfo(rateLimit);
                //System.out.println(newInfo);
                return newInfo;
            }
            //System.out.println("Current Attempts: " + info.count);
            
            info.resetIfExpired();
            info.increment();
            
            if (info.isExceeded()) {
                //System.out.println("Exceeded Rate Limit: " + info.count);
                throw new RateLimitExceededException(rateLimit.message());
            }
            
            return info;
        });
    }

    public static void resetRateLimit(String key, Method method) {
        String cacheKey = key + ":" + method.getDeclaringClass().getName() + ":" + method.getName();
        attempts.remove(cacheKey);
    }
} 