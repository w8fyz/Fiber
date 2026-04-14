package sh.fyz.fiber.core.security.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local context for attaching custom data to audit logs.
 * Use this inside any endpoint method annotated with @AuditLog to enrich the log entry.
 *
 * Example:
 *   AuditContext.put("paymentId", payment.getId());
 *   AuditContext.put("amount", payment.getAmount());
 */
public class AuditContext {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    public static void put(String key, Object value) {
        CONTEXT.get().put(key, value);
    }

    public static Object get(String key) {
        return CONTEXT.get().get(key);
    }

    public static Map<String, Object> getAll() {
        Map<String, Object> data = CONTEXT.get();
        return data.isEmpty() ? null : new HashMap<>(data);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
