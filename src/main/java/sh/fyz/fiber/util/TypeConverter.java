package sh.fyz.fiber.util;

public final class TypeConverter {

    private TypeConverter() {}

    public static Object convert(String value, Class<?> type) {
        if (value == null) {
            return null;
        }

        if (type == String.class) {
            return value;
        } else if (type == Integer.class || type == int.class) {
            return Integer.parseInt(value);
        } else if (type == Long.class || type == long.class) {
            return Long.parseLong(value);
        } else if (type == Double.class || type == double.class) {
            return Double.parseDouble(value);
        } else if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}
