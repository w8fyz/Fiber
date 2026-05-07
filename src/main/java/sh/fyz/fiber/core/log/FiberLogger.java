package sh.fyz.fiber.core.log;

public interface FiberLogger {

    String name();

    LogLevel effectiveLevel();

    boolean isEnabled(LogLevel level);

    void log(LogLevel level, String message);

    void log(LogLevel level, String pattern, Object arg);

    void log(LogLevel level, String pattern, Object arg1, Object arg2);

    void log(LogLevel level, String pattern, Object... args);

    void log(LogLevel level, Throwable t, String message);

    void log(LogLevel level, Throwable t, String pattern, Object... args);

    default boolean isErrorEnabled() { return isEnabled(LogLevel.ERROR); }
    default boolean isWarnEnabled()  { return isEnabled(LogLevel.WARN);  }
    default boolean isInfoEnabled()  { return isEnabled(LogLevel.INFO);  }
    default boolean isDebugEnabled() { return isEnabled(LogLevel.DEBUG); }
    default boolean isTraceEnabled() { return isEnabled(LogLevel.TRACE); }

    default void error(String msg) { log(LogLevel.ERROR, msg); }
    default void error(String pattern, Object arg) { log(LogLevel.ERROR, pattern, arg); }
    default void error(String pattern, Object a, Object b) { log(LogLevel.ERROR, pattern, a, b); }
    default void error(String pattern, Object... args) { log(LogLevel.ERROR, pattern, args); }
    default void error(String msg, Throwable t) { log(LogLevel.ERROR, t, msg); }

    default void warn(String msg) { log(LogLevel.WARN, msg); }
    default void warn(String pattern, Object arg) { log(LogLevel.WARN, pattern, arg); }
    default void warn(String pattern, Object a, Object b) { log(LogLevel.WARN, pattern, a, b); }
    default void warn(String pattern, Object... args) { log(LogLevel.WARN, pattern, args); }
    default void warn(String msg, Throwable t) { log(LogLevel.WARN, t, msg); }

    default void info(String msg) { log(LogLevel.INFO, msg); }
    default void info(String pattern, Object arg) { log(LogLevel.INFO, pattern, arg); }
    default void info(String pattern, Object a, Object b) { log(LogLevel.INFO, pattern, a, b); }
    default void info(String pattern, Object... args) { log(LogLevel.INFO, pattern, args); }
    default void info(String msg, Throwable t) { log(LogLevel.INFO, t, msg); }

    default void debug(String msg) { log(LogLevel.DEBUG, msg); }
    default void debug(String pattern, Object arg) { log(LogLevel.DEBUG, pattern, arg); }
    default void debug(String pattern, Object a, Object b) { log(LogLevel.DEBUG, pattern, a, b); }
    default void debug(String pattern, Object... args) { log(LogLevel.DEBUG, pattern, args); }
    default void debug(String msg, Throwable t) { log(LogLevel.DEBUG, t, msg); }

    default void trace(String msg) { log(LogLevel.TRACE, msg); }
    default void trace(String pattern, Object arg) { log(LogLevel.TRACE, pattern, arg); }
    default void trace(String pattern, Object a, Object b) { log(LogLevel.TRACE, pattern, a, b); }
    default void trace(String pattern, Object... args) { log(LogLevel.TRACE, pattern, args); }
    default void trace(String msg, Throwable t) { log(LogLevel.TRACE, t, msg); }
}
