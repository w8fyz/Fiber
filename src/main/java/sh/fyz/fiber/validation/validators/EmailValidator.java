package sh.fyz.fiber.validation.validators;

import sh.fyz.fiber.validation.Email;
import sh.fyz.fiber.validation.ValidationResult;
import sh.fyz.fiber.validation.Validator;

import java.lang.annotation.Annotation;
import java.util.regex.Pattern;

public class EmailValidator implements Validator<String> {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    @Override
    public ValidationResult validate(String value, Annotation annotation) {
        if (value == null) {
            return ValidationResult.invalid("Value cannot be null");
        }

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            return ValidationResult.invalid("Invalid email format");
        }

        return ValidationResult.valid();
    }

    @Override
    public Class<? extends java.lang.annotation.Annotation> getAnnotationType() {
        return Email.class;
    }
} 