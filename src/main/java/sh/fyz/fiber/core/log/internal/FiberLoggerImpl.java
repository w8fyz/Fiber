package sh.fyz.fiber.core.log.internal;

import sh.fyz.fiber.core.log.FiberLog;
import sh.fyz.fiber.core.log.FiberLogger;
import sh.fyz.fiber.core.log.LogContext;
import sh.fyz.fiber.core.log.LogEvent;
import sh.fyz.fiber.core.log.LogLevel;

public final class FiberLoggerImpl implements FiberLogger {

    private final String name;
    private volatile LogLevel effective;

    public FiberLoggerImpl(String name, LogLevel effective) {
        this.name = name;
        this.effective = effective;
    }

    public void setEffectiveLevel(LogLevel level) {
        this.effective = level;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public LogLevel effectiveLevel() {
        return effective;
    }

    @Override
    public boolean isEnabled(LogLevel level) {
        return level.severity >= effective.severity;
    }

    @Override
    public void log(LogLevel level, String message) {
        if (level.severity < effective.severity) return;
        emit(level, message, null);
    }

    @Override
    public void log(LogLevel level, String pattern, Object arg) {
        if (level.severity < effective.severity) return;
        MessageFormatter.Result r = MessageFormatter.format(pattern, arg);
        emit(level, r.message(), r.throwable());
    }

    @Override
    public void log(LogLevel level, String pattern, Object a, Object b) {
        if (level.severity < effective.severity) return;
        MessageFormatter.Result r = MessageFormatter.format(pattern, a, b);
        emit(level, r.message(), r.throwable());
    }

    @Override
    public void log(LogLevel level, String pattern, Object... args) {
        if (level.severity < effective.severity) return;
        MessageFormatter.Result r = MessageFormatter.format(pattern, args);
        emit(level, r.message(), r.throwable());
    }

    @Override
    public void log(LogLevel level, Throwable t, String message) {
        if (level.severity < effective.severity) return;
        emit(level, message, t);
    }

    @Override
    public void log(LogLevel level, Throwable t, String pattern, Object... args) {
        if (level.severity < effective.severity) return;
        MessageFormatter.Result r = MessageFormatter.format(pattern, t, args);
        emit(level, r.message(), r.throwable() != null ? r.throwable() : t);
    }

    private void emit(LogLevel level, String message, Throwable throwable) {
        LogEvent event = new LogEvent(
                System.currentTimeMillis(),
                level,
                name,
                message,
                throwable,
                Thread.currentThread().getName(),
                LogContext.requestId(),
                LogContext.snapshot()
        );
        FiberLog.emit(event);
    }
}
