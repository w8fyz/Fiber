package sh.fyz.fiber.core.log.handlers;

import sh.fyz.fiber.core.log.LogEvent;
import sh.fyz.fiber.core.log.LogHandler;
import sh.fyz.fiber.core.log.LogLevel;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public final class ConsoleLogHandler implements LogHandler {

    private static final DateTimeFormatter TS = DateTimeFormatter
            .ofPattern("HH:mm:ss.SSS", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private static final String ESC = String.valueOf((char) 27);
    private static final String RESET = ESC + "[0m";
    private static final String DIM = ESC + "[2m";
    private static final String RED = ESC + "[31m";
    private static final String YELLOW = ESC + "[33m";
    private static final String GREEN = ESC + "[32m";
    private static final String CYAN = ESC + "[36m";
    private static final String GRAY = ESC + "[37m";

    private final boolean useColor;

    public ConsoleLogHandler() {
        this(detectColorSupport());
    }

    public ConsoleLogHandler(boolean useColor) {
        this.useColor = useColor;
    }

    private static boolean detectColorSupport() {
        if (System.getenv("NO_COLOR") != null) return false;
        String prop = System.getProperty("fiber.log.color");
        if ("true".equalsIgnoreCase(prop)) return true;
        if ("false".equalsIgnoreCase(prop)) return false;
        return System.console() != null;
    }

    @Override
    public void emit(LogEvent event) {
        PrintStream out = (event.level() == LogLevel.ERROR || event.level() == LogLevel.WARN)
                ? System.err : System.out;

        StringBuilder sb = new StringBuilder(128);
        appendColored(sb, DIM, TS.format(Instant.ofEpochMilli(event.epochMillis())));
        sb.append(' ');
        appendColored(sb, levelColor(event.level()), padLevel(event.level()));
        sb.append(' ');
        appendColored(sb, GRAY, "[" + event.threadName() + "]");
        sb.append(' ');
        appendColored(sb, CYAN, shortCategory(event.category()));
        if (event.requestId() != null) {
            sb.append(' ');
            appendColored(sb, DIM, "rid=" + event.requestId());
        }
        Map<String, String> mdc = event.mdc();
        if (mdc != null && !mdc.isEmpty()) {
            sb.append(' ');
            appendColored(sb, DIM, mdc.toString());
        }
        sb.append(" - ").append(event.message());
        if (event.throwable() != null) {
            sb.append(System.lineSeparator()).append(stackTrace(event.throwable()));
        }
        out.println(sb);
    }

    private void appendColored(StringBuilder sb, String color, String text) {
        if (useColor) sb.append(color).append(text).append(RESET);
        else sb.append(text);
    }

    private static String levelColor(LogLevel level) {
        return switch (level) {
            case ERROR -> RED;
            case WARN -> YELLOW;
            case INFO -> GREEN;
            case DEBUG -> CYAN;
            case TRACE -> DIM;
            default -> RESET;
        };
    }

    private static String padLevel(LogLevel level) {
        String name = level.name();
        return switch (name.length()) {
            case 4 -> name + " ";
            case 5 -> name;
            default -> (name + "     ").substring(0, 5);
        };
    }

    private static String shortCategory(String category) {
        if (category == null) return "?";
        int last = category.lastIndexOf('.');
        return last < 0 ? category : category.substring(last + 1);
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
