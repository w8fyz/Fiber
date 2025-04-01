package sh.fyz.fiber.validation.validators;

import sh.fyz.fiber.validation.NotNull;
import sh.fyz.fiber.validation.ValidationResult;
import sh.fyz.fiber.validation.Validator;

public class NotNullValidator implements Validator<Object> {
    @Override
    public ValidationResult validate(Object value, java.lang.annotation.Annotation annotation) {
        if (value == null) {
            NotNull notNullAnnotation = (NotNull) annotation;
            return ValidationResult.invalid(notNullAnnotation.message());
        }

        return ValidationResult.valid();
    }

    @Override
    public Class<? extends java.lang.annotation.Annotation> getAnnotationType() {
        return NotNull.class;
    }
} 