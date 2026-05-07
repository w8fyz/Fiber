package sh.fyz.fiber.core.log;

import sh.fyz.fiber.core.log.internal.EarlyEventBuffer;
import sh.fyz.fiber.core.log.internal.FiberLoggerImpl;
import sh.fyz.fiber.core.log.internal.LevelTable;
import sh.fyz.fiber.core.log.internal.LogDispatcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FiberLog {

    private static final ConcurrentMap<String, FiberLoggerImpl> LOGGERS = new ConcurrentHashMap<>();
    private static final LevelTable LEVELS = new LevelTable(LogLevel.INFO);
    private static final LogDispatcher DISPATCHER = new LogDispatcher();
    private static final EarlyEventBuffer EARLY = new EarlyEventBuffer(256);

    private static final ClassValue<FiberLogger> CLASS_LOGGERS = new ClassValue<>() {
        @Override
        protected FiberLogger computeValue(Class<?> type) {
            return FiberLog.get(type.getName());
        }
    };

    private FiberLog() {
    }

    public static FiberLogger get(Class<?> type) {
        return CLASS_LOGGERS.get(type);
    }

    public static FiberLogger get(String name) {
        FiberLoggerImpl existing = LOGGERS.get(name);
        if (existing != null) return existing;
        FiberLoggerImpl created = new FiberLoggerImpl(name, LEVELS.resolve(name));
        FiberLoggerImpl prior = LOGGERS.putIfAbsent(name, created);
        return prior != null ? prior : created;
    }

    public static void setRootLevel(LogLevel level) {
        LEVELS.setRoot(level);
        refreshAll();
    }

    public static LogLevel getRootLevel() {
        return LEVELS.getRoot();
    }

    public static void setCategoryLevel(String category, LogLevel level) {
        LEVELS.set(category, level);
        refreshAll();
    }

    public static void addHandler(LogHandler handler) {
        DISPATCHER.addHandler(handler);
        EARLY.drainTo(DISPATCHER);
    }

    public static void removeHandler(LogHandler handler) {
        DISPATCHER.removeHandler(handler);
    }

    public static boolean hasHandlers() {
        return DISPATCHER.handlerCount() > 0;
    }

    public static void shutdown() {
        DISPATCHER.shutdown();
    }

    public static void emit(LogEvent event) {
        if (DISPATCHER.handlerCount() == 0) {
            EARLY.offer(event);
            return;
        }
        DISPATCHER.dispatch(event);
    }

    public static void handle(Throwable t, String pattern, Object... args) {
        FiberLogger logger = callerLogger();
        LogLevel level = severityFor(t);
        logger.log(level, t, pattern, args);
    }

    public static void handle(Throwable t, String message) {
        FiberLogger logger = callerLogger();
        logger.log(severityFor(t), t, message);
    }

    public static void handleSilent(Throwable t) {
        FiberLogger logger = callerLogger();
        if (!logger.isEnabled(LogLevel.TRACE)) return;
        logger.log(LogLevel.TRACE, t, "swallowed: {}", t.toString());
    }

    private static LogLevel severityFor(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof IllegalStateException) {
            return LogLevel.WARN;
        }
        return LogLevel.ERROR;
    }

    private static FiberLogger callerLogger() {
        Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(s -> s.skip(2).findFirst().map(StackWalker.StackFrame::getDeclaringClass).orElse(FiberLog.class));
        return get(caller);
    }

    private static void refreshAll() {
        for (FiberLoggerImpl impl : LOGGERS.values()) {
            impl.setEffectiveLevel(LEVELS.resolve(impl.name()));
        }
    }
}
