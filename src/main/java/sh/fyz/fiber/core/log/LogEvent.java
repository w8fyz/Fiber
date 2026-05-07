package sh.fyz.fiber.core.log;

import java.util.Map;

public record LogEvent(
        long epochMillis,
        LogLevel level,
        String category,
        String message,
        Throwable throwable,
        String threadName,
        String requestId,
        Map<String, String> mdc
) {
}
