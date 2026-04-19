package sh.fyz.fiber.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ValidationRegistry {
    private static final Map<Class<? extends Annotation>, Validator<?>> validators = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<FieldValidationInfo>> fieldCache = new ConcurrentHashMap<>();

    private record FieldValidationInfo(Field field, List<Annotation> validatableAnnotations) {}

    public static void register(Validator<?> validator) {
        validators.put(validator.getAnnotationType(), validator);
    }

    private static List<FieldValidationInfo> getFieldInfo(Class<?> clazz) {
        return fieldCache.computeIfAbsent(clazz, c -> {
            List<FieldValidationInfo> infos = new ArrayList<>();
            // Walk the type hierarchy so inherited fields are also validated. Aligned
            // with DTOConvertible.getCachedFields().
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    List<Annotation> validatable = new ArrayList<>();
                    for (Annotation annotation : field.getAnnotations()) {
                        if (validators.containsKey(annotation.annotationType())) {
                            validatable.add(annotation);
                        }
                    }
                    if (!validatable.isEmpty()) {
                        field.setAccessible(true);
                        infos.add(new FieldValidationInfo(field, Collections.unmodifiableList(validatable)));
                    }
                }
                current = current.getSuperclass();
            }
            return Collections.unmodifiableList(infos);
        });
    }

    @SuppressWarnings("unchecked")
    public static ValidationResult validate(Object value) {
        if (value == null) {
            return ValidationResult.valid();
        }

        List<String> errors = new ArrayList<>();
        for (FieldValidationInfo info : getFieldInfo(value.getClass())) {
            try {
                Object fieldValue = info.field().get(value);
                for (Annotation annotation : info.validatableAnnotations()) {
                    Validator<?> validator = validators.get(annotation.annotationType());
                    if (validator != null) {
                        ValidationResult fieldResult = ((Validator<Object>) validator).validate(fieldValue, annotation);
                        if (!fieldResult.isValid()) {
                            errors.addAll(fieldResult.getErrors());
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                errors.add("Failed to access field: " + info.field().getName());
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    @SuppressWarnings("unchecked")
    public static ValidationResult validateParameter(Parameter parameter, Object value) {
        for (Annotation annotation : parameter.getAnnotations()) {
            Validator<?> validator = validators.get(annotation.annotationType());
            if (validator != null) {
                ValidationResult result = ((Validator<Object>) validator).validate(value, annotation);
                if (!result.isValid()) {
                    return result;
                }
            }
        }
        return ValidationResult.valid();
    }
}
