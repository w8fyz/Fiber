package sh.fyz.fiber.unit;

import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.email.EmailTemplateEngine;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateEngineTest {

    @Test
    void variableSubstitutionWorks() {
        String result = EmailTemplateEngine.processTemplate(
                "Hello {name}!", Map.of("name", "Alice"));
        assertEquals("Hello Alice!", result);
    }

    @Test
    void missingVariableBecomesEmpty() {
        String result = EmailTemplateEngine.processTemplate(
                "Hello {name}!", Map.of());
        assertEquals("Hello !", result);
    }

    @Test
    void dollarSignInValueIsNotInterpreted() {
        // Regression: before Matcher.quoteReplacement, a value containing $1 would be
        // interpreted as a backreference and either crash or leak adjacent content.
        String result = EmailTemplateEngine.processTemplate(
                "Amount: {amount}", Map.of("amount", "$1,000.00"));
        assertEquals("Amount: $1,000.00", result);
    }

    @Test
    void backslashInValueIsNotInterpreted() {
        String result = EmailTemplateEngine.processTemplate(
                "Path: {path}", Map.of("path", "C:\\Users\\Alice"));
        assertEquals("Path: C:\\Users\\Alice", result);
    }

    @Test
    void nullTemplateReturnedAsIs() {
        assertNull(EmailTemplateEngine.processTemplate(null, Map.of()));
    }
}
