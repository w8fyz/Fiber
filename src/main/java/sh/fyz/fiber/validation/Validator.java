package sh.fyz.fiber.validation;

import java.lang.annotation.Annotation;

public interface Validator<T> {
    ValidationResult validate(T value, Annotation annotation);
    Class<? extends Annotation> getAnnotationType();
} 