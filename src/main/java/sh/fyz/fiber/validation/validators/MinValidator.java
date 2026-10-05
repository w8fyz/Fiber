package sh.fyz.fiber.validation.validators;

import sh.fyz.fiber.validation.Min;
import sh.fyz.fiber.validation.ValidationResult;
import sh.fyz.fiber.validation.Validator;

import java.lang.annotation.Annotation;

public class MinValidator implements Validator<Number> {
    @Override
    public ValidationResult validate(Number value, Annotation annotation) {
        if (value == null) {
            return ValidationResult.valid();
        }

        Min min = (Min) annotation;
        if (value.doubleValue() < min.value()) {
            return ValidationResult.invalid(min.message().replace("{value}", String.valueOf(min.value())));
        }

        return ValidationResult.valid();
    }

    @Override
    public Class<? extends java.lang.annotation.Annotation> getAnnotationType() {
        return Min.class;
    }
} 