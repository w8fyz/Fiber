package sh.fyz.fiber.validation;

import sh.fyz.fiber.validation.validators.EmailValidator;
import sh.fyz.fiber.validation.validators.MinValidator;
import sh.fyz.fiber.validation.validators.NotBlankValidator;
import sh.fyz.fiber.validation.validators.NotNullValidator;

public class ValidationInitializer {
    public static void initialize() {
        ValidationRegistry.register(new NotNullValidator());
        ValidationRegistry.register(new NotBlankValidator());
        ValidationRegistry.register(new MinValidator());
        ValidationRegistry.register(new EmailValidator());
    }
} 