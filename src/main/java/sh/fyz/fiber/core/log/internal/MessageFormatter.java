package sh.fyz.fiber.core.log.internal;

public final class MessageFormatter {

    private MessageFormatter() {
    }

    public record Result(String message, Throwable throwable) {
    }

    public static Result format(String pattern, Object... args) {
        if (args == null || args.length == 0) {
            return new Result(pattern, null);
        }
        int placeholders = countPlaceholders(pattern);
        Object last = args[args.length - 1];
        Throwable throwable = (last instanceof Throwable t && args.length > placeholders) ? t : null;
        int argLimit = throwable != null ? args.length - 1 : args.length;
        return new Result(interpolate(pattern, args, argLimit), throwable);
    }

    public static Result format(String pattern, Object arg) {
        if (arg instanceof Throwable t && countPlaceholders(pattern) == 0) {
            return new Result(pattern, t);
        }
        return new Result(interpolate(pattern, new Object[]{arg}, 1), null);
    }

    public static Result format(String pattern, Object a, Object b) {
        Throwable throwable = null;
        int limit = 2;
        if (b instanceof Throwable t && countPlaceholders(pattern) <= 1) {
            throwable = t;
            limit = 1;
        }
        return new Result(interpolate(pattern, new Object[]{a, b}, limit), throwable);
    }

    public static Result format(String pattern) {
        return new Result(pattern, null);
    }

    public static Result format(String pattern, Throwable t, Object... args) {
        return new Result(interpolate(pattern, args, args.length), t);
    }

    private static int countPlaceholders(String pattern) {
        if (pattern == null) return 0;
        int count = 0;
        for (int i = 0; i + 1 < pattern.length(); i++) {
            if (pattern.charAt(i) == '{' && pattern.charAt(i + 1) == '}') {
                count++;
                i++;
            } else if (pattern.charAt(i) == '\\' && i + 2 < pattern.length()
                    && pattern.charAt(i + 1) == '{' && pattern.charAt(i + 2) == '}') {
                i += 2;
            }
        }
        return count;
    }

    private static String interpolate(String pattern, Object[] args, int argLimit) {
        if (pattern == null) return "null";
        if (args == null || argLimit == 0) return pattern;
        StringBuilder sb = new StringBuilder(pattern.length() + 32);
        int argIdx = 0;
        int i = 0;
        int len = pattern.length();
        while (i < len) {
            char c = pattern.charAt(i);
            if (c == '\\' && i + 2 < len && pattern.charAt(i + 1) == '{' && pattern.charAt(i + 2) == '}') {
                sb.append("{}");
                i += 3;
            } else if (c == '{' && i + 1 < len && pattern.charAt(i + 1) == '}') {
                if (argIdx < argLimit) {
                    appendArg(sb, args[argIdx++]);
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static void appendArg(StringBuilder sb, Object arg) {
        if (arg == null) {
            sb.append("null");
            return;
        }
        try {
            if (arg.getClass().isArray()) {
                sb.append(arrayToString(arg));
            } else {
                sb.append(arg);
            }
        } catch (Throwable t) {
            sb.append("[FAILED toString: ").append(t.getClass().getSimpleName()).append(']');
        }
    }

    private static String arrayToString(Object array) {
        if (array instanceof Object[] objs) return java.util.Arrays.deepToString(objs);
        if (array instanceof int[] ints) return java.util.Arrays.toString(ints);
        if (array instanceof long[] longs) return java.util.Arrays.toString(longs);
        if (array instanceof byte[] bytes) return java.util.Arrays.toString(bytes);
        if (array instanceof short[] shorts) return java.util.Arrays.toString(shorts);
        if (array instanceof float[] floats) return java.util.Arrays.toString(floats);
        if (array instanceof double[] doubles) return java.util.Arrays.toString(doubles);
        if (array instanceof char[] chars) return java.util.Arrays.toString(chars);
        if (array instanceof boolean[] bools) return java.util.Arrays.toString(bools);
        return array.toString();
    }
}
