package sh.fyz.fiber.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationRegistry {
    private static final Map<Class<? extends Annotation>, Validator<?>> validators = new HashMap<>();

    public static void register(Validator<?> validator) {
        validators.put(validator.getAnnotationType(), validator);
    }

    @SuppressWarnings("unchecked")
    public static ValidationResult validate(Object value) {
        if (value == null) {
            return ValidationResult.valid();
        }

        List<String> errors = new ArrayList<>();
        for (Field field : value.getClass().getDeclaredFields()) {
            for (Annotation annotation : field.getAnnotations()) {
                Validator<?> validator = validators.get(annotation.annotationType());
                if (validator != null) {
                    field.setAccessible(true);
                    try {
                        Object fieldValue = field.get(value);
                        ValidationResult fieldResult = ((Validator<Object>) validator).validate(fieldValue, annotation);
                        if (!fieldResult.isValid()) {
                            errors.addAll(fieldResult.getErrors());
                        }
                    } catch (IllegalAccessException e) {
                        errors.add("Failed to access field: " + field.getName());
                    }
                }
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