package sh.fyz.fiber.unit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import sh.fyz.fiber.core.security.processors.RateLimitProcessor;
import org.mockito.Mockito;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.exceptions.RateLimitExceededException;
import sh.fyz.fiber.core.security.interceptors.RateLimitInterceptor;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitCaseTest {

    @RateLimit(attempts = 2, timeout = 60, unit = TimeUnit.SECONDS,
            slidingWindow = false, message = "slow down")
    public void limited() {}

    @AfterEach
    void reset() {
        RateLimitInterceptor.clearAll();
    }

    @Test
    void caseVariationsShareTheSameBucket() throws Exception {
        Method m = RateLimitCaseTest.class.getMethod("limited");

        // Two requests from "bob" burn the quota.
        RateLimitInterceptor.checkRateLimit("bob", m);
        RateLimitInterceptor.checkRateLimit("bob", m);

        // A third request with different casing used to bypass the limit.
        assertThrows(RateLimitExceededException.class,
                () -> RateLimitInterceptor.checkRateLimit("BOB", m));
        assertThrows(RateLimitExceededException.class,
                () -> RateLimitInterceptor.checkRateLimit("Bob", m));
    }

    @RateLimit(attempts = 2, timeout = 60, unit = TimeUnit.SECONDS)
    public static class ClassLevelLimited {
        public void endpoint() {}
    }

    @Test
    void classLevelAnnotationIsEnforced() throws Exception {
        Method m = ClassLevelLimited.class.getMethod("endpoint");
        RateLimitInterceptor.checkRateLimit("alice", m);
        RateLimitInterceptor.checkRateLimit("alice", m);
        assertThrows(RateLimitExceededException.class,
                () -> RateLimitInterceptor.checkRateLimit("alice", m));
    }

    @Test
    void concurrentFirstRequestsShareOneCounter() throws Exception {
        Method m = RateLimitCaseTest.class.getMethod("limited");
        int threads = 32;
        AtomicInteger allowed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch done = new CountDownLatch(threads);
            for (int i = 0; i < threads; i++) {
                pool.execute(() -> {
                    try {
                        start.await();
                        RateLimitInterceptor.checkRateLimit("carol", m);
                        allowed.incrementAndGet();
                    } catch (RateLimitExceededException | InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            done.await();
        } finally {
            pool.shutdown();
        }
        assertEquals(2, allowed.get());
    }

    @RateLimit(attempts = 1, timeout = 60, unit = TimeUnit.SECONDS, perUser = true)
    public void perUser() {}

    @Test
    void perUserLimitIsKeyedByAuthenticatedUser() throws Exception {
        Method m = RateLimitCaseTest.class.getMethod("perUser");
        RateLimit rl = m.getAnnotation(RateLimit.class);
        HttpServletRequest user1 = Mockito.mock(HttpServletRequest.class);
        Mockito.when(user1.getRemoteAddr()).thenReturn("203.0.113.7");
        Mockito.when(user1.getAttribute("userId")).thenReturn(1L);
        HttpServletRequest user2 = Mockito.mock(HttpServletRequest.class);
        Mockito.when(user2.getRemoteAddr()).thenReturn("203.0.113.7");
        Mockito.when(user2.getAttribute("userId")).thenReturn(2L);

        assertNull(RateLimitProcessor.check(rl, m, null, user1));
        assertNotNull(RateLimitProcessor.check(rl, m, null, user1), "same user is limited");
        assertNull(RateLimitProcessor.check(rl, m, null, user2), "other user behind the same IP is not");
    }

    @RateLimit(attempts = 2, timeout = 1, unit = TimeUnit.SECONDS)
    public void shortWindow() {}

    @Test
    void newWindowAllowsTheFullQuota() throws Exception {
        Method m = RateLimitCaseTest.class.getMethod("shortWindow");
        RateLimitInterceptor.checkRateLimit("dave", m);
        RateLimitInterceptor.checkRateLimit("dave", m);
        Thread.sleep(1100);
        RateLimitInterceptor.checkRateLimit("dave", m);
        RateLimitInterceptor.checkRateLimit("dave", m);
        assertThrows(RateLimitExceededException.class,
                () -> RateLimitInterceptor.checkRateLimit("dave", m));
    }
}
