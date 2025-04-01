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

        double minValue = 0.0; // Default value, can be overridden by actual annotation
        if (value.doubleValue() < minValue) {
            return ValidationResult.invalid("Value must be greater than or equal to " + minValue);
        }

        return ValidationResult.valid();
    }

    @Override
    public Class<? extends java.lang.annotation.Annotation> getAnnotationType() {
        return Min.class;
    }
} 