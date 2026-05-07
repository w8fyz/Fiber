package sh.fyz.fiber.core.log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public final class LogContext {

    private static final ThreadLocal<RequestContext> CTX = new ThreadLocal<>();

    private LogContext() {
    }

    public record RequestContext(String requestId, Map<String, String> mdc) {
        public RequestContext {
            if (mdc == null) mdc = Map.of();
        }
    }

    public static RequestContext current() {
        return CTX.get();
    }

    public static String requestId() {
        RequestContext ctx = CTX.get();
        return ctx == null ? null : ctx.requestId();
    }

    public static Map<String, String> snapshot() {
        RequestContext ctx = CTX.get();
        if (ctx == null || ctx.mdc().isEmpty()) return null;
        return Collections.unmodifiableMap(ctx.mdc());
    }

    public static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public static void put(String key, String value) {
        RequestContext ctx = CTX.get();
        if (ctx == null) return;
        if (ctx.mdc() instanceof HashMap<String, String> mutable) {
            mutable.put(key, value);
        }
    }

    public static String get(String key) {
        RequestContext ctx = CTX.get();
        if (ctx == null) return null;
        return ctx.mdc().get(key);
    }

    public static void runWith(String requestId, Runnable r) {
        RequestContext prev = CTX.get();
        CTX.set(new RequestContext(requestId, new HashMap<>()));
        try {
            r.run();
        } finally {
            if (prev == null) CTX.remove(); else CTX.set(prev);
        }
    }

    public static <T> T callWith(String requestId, Callable<T> c) throws Exception {
        RequestContext prev = CTX.get();
        CTX.set(new RequestContext(requestId, new HashMap<>()));
        try {
            return c.call();
        } finally {
            if (prev == null) CTX.remove(); else CTX.set(prev);
        }
    }

    public static void clear() {
        CTX.remove();
    }
}
