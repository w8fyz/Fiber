package sh.fyz.fiber.unit;

import sh.fyz.fiber.validation.Min;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sh.fyz.fiber.validation.NotBlank;
import sh.fyz.fiber.validation.NotNull;
import sh.fyz.fiber.validation.ValidationInitializer;
import sh.fyz.fiber.validation.ValidationRegistry;
import sh.fyz.fiber.validation.ValidationResult;

import static org.junit.jupiter.api.Assertions.*;

class ValidationHierarchyTest {

    @BeforeAll
    static void init() {
        ValidationInitializer.initialize();
    }

    static class Base {
        @NotBlank(message = "name required")
        public String name;
    }

    static class Child extends Base {
        @NotNull(message = "email required")
        public String email;
    }

    @Test
    void inheritedFieldsAreValidated() {
        Child c = new Child();
        c.name = "   ";        // fails NotBlank
        c.email = null;        // fails NotNull
        ValidationResult result = ValidationRegistry.validate(c);
        assertFalse(result.isValid(), "expected validation to fail");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("name")),
                "expected inherited 'name' field to be validated, got " + result.getErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("email")),
                "expected local 'email' field to be validated, got " + result.getErrors());
    }

    @Test
    void validChildPassesValidation() {
        Child c = new Child();
        c.name = "Alice";
        c.email = "a@b.test";
        assertTrue(ValidationRegistry.validate(c).isValid());
    }

    static class Adult {
        @Min(18)
        public int age;
    }

    @Test
    void minUsesTheAnnotationValue() {
        Adult a = new Adult();
        a.age = 17;
        ValidationResult result = ValidationRegistry.validate(a);
        assertFalse(result.isValid(), "17 must fail @Min(18)");
        assertEquals("Value must be greater than or equal to 18", result.getFirstError());
        a.age = 18;
        assertTrue(ValidationRegistry.validate(a).isValid());
    }
}
