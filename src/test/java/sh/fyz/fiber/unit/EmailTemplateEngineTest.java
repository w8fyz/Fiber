package sh.fyz.fiber.unit;

import org.junit.jupiter.api.Test;
import sh.fyz.fiber.annotations.dto.IgnoreDTO;
import sh.fyz.fiber.core.dto.DTOConvertible;
import sh.fyz.fiber.core.email.EmailTemplateEngine;

import java.util.List;
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

    public static class BaseRow extends DTOConvertible {
        protected String id;
    }

    public static class Row extends BaseRow {
        private static final String CONSTANT = "not a column";
        private String name;
        @IgnoreDTO
        private String secret;

        Row(String id, String name, String secret) {
            this.id = id;
            this.name = name;
            this.secret = secret;
        }
    }

    @Test
    void dtoTableEscapesValuesAndUsesTheDtoFields() {
        String table = EmailTemplateEngine.generateTableFromDTOs(
                List.of(new Row("7", "<a href=\"https://evil\">x</a>", "hunter2")), null);
        assertTrue(table.contains("<td>&lt;a href=&quot;https://evil&quot;&gt;x&lt;/a&gt;</td>"), table);
        assertTrue(table.contains("<th>id</th>") && table.contains("<td>7</td>"), "inherited field: " + table);
        assertFalse(table.contains("secret") || table.contains("hunter2"), "@IgnoreDTO field: " + table);
        assertFalse(table.contains("CONSTANT"), "static field: " + table);
    }
}
