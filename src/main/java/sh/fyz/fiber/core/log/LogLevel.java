package sh.fyz.fiber.core.log;

public enum LogLevel {
    OFF(Integer.MAX_VALUE),
    ERROR(40),
    WARN(30),
    INFO(20),
    DEBUG(10),
    TRACE(0);

    public final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public boolean enables(LogLevel target) {
        return target.severity >= this.severity;
    }
}
