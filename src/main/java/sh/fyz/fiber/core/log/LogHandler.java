package sh.fyz.fiber.core.log;

public interface LogHandler {

    void emit(LogEvent event);

    default LogLevel minLevel() {
        return LogLevel.TRACE;
    }

    default boolean prefersSync() {
        return false;
    }

    default void close() {
    }
}
