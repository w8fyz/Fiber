package sh.fyz.fiber.validation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ValidationProcessor {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$");

    public static List<String> validate(Object object) {
        List<String> errors = new ArrayList<>();
        if (object == null) {
            return errors;
        }

        Class<?> clazz = object.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(object);
                validateField(field, value, errors);
            } catch (IllegalAccessException e) {
                errors.add("Failed to access field: " + field.getName());
            }
        }

        return errors;
    }

    private static void validateField(Field field, Object value, List<String> errors) {
        // @NotNull validation
        if (field.isAnnotationPresent(NotNull.class)) {
            if (value == null) {
                errors.add(field.getAnnotation(NotNull.class).message());
            }
        }

        // @NotBlank validation for String fields
        if (field.isAnnotationPresent(NotBlank.class)) {
            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                errors.add(field.getAnnotation(NotBlank.class).message());
            }
        }

        // @Min validation for numeric fields
        if (field.isAnnotationPresent(Min.class)) {
            Min minAnnotation = field.getAnnotation(Min.class);
            if (value != null) {
                if (value instanceof Number) {
                    long longValue = ((Number) value).longValue();
                    if (longValue < minAnnotation.value()) {
                        errors.add(minAnnotation.message().replace("{value}", String.valueOf(minAnnotation.value())));
                    }
                }
            }
        }

        // @Email validation for String fields
        if (field.isAnnotationPresent(Email.class)) {
            if (value instanceof String) {
                String email = (String) value;
                if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
                    errors.add(field.getAnnotation(Email.class).message());
                }
            }
        }
    }
} 