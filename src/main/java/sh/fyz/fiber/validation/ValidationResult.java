package sh.fyz.fiber.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, Collections.singletonList(error));
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, new ArrayList<>(errors));
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
} 