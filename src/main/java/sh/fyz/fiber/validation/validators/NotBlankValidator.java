package sh.fyz.fiber.validation.validators;

import sh.fyz.fiber.validation.NotBlank;
import sh.fyz.fiber.validation.ValidationResult;
import sh.fyz.fiber.validation.Validator;

public class NotBlankValidator implements Validator<String> {
    @Override
    public ValidationResult validate(String value, java.lang.annotation.Annotation annotation) {
        if (value == null || value.trim().isEmpty()) {
            NotBlank notBlankAnnotation = (NotBlank) annotation;
            return ValidationResult.invalid(notBlankAnnotation.message());
        }

        return ValidationResult.valid();
    }

    @Override
    public Class<? extends java.lang.annotation.Annotation> getAnnotationType() {
        return NotBlank.class;
    }
} 