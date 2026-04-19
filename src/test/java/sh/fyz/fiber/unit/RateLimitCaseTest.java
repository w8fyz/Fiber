package sh.fyz.fiber.unit;

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
}
