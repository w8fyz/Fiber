package sh.fyz.fiber.unit;

import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.email.EmailCssUtils;

import static org.junit.jupiter.api.Assertions.*;

class EmailCssUtilsTest {

    @Test
    void inliningKeepsRulesItCannotApply() {
        String html = "<html><head><style>\n.btn { color: red; }\n@media (max-width: 600px) { .btn { width: 100%; } }\n</style></head>"
                + "<body><a class=\"btn\">Go</a></body></html>";
        String out = EmailCssUtils.convertCssToInline(html);
        assertTrue(out.contains("style=\"color:red;\""), out);
        assertEquals(1, out.split("style=\"", -1).length - 1, "exactly one inline style: " + out);
        assertTrue(out.contains("@media"), "the <style> block must survive for media queries: " + out);
    }
}
