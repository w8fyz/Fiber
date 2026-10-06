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

    @Test
    void singleLineStyleBlockIsKept() {
        String html = "<html><head><style>p { color: red; } @media (max-width: 600px) { p { color: blue; } }</style></head>"
                + "<body><p>Hi</p></body></html>";
        String out = EmailCssUtils.convertCssToInline(html);
        assertTrue(out.contains("@media"), out);
    }

    @Test
    void quotesAndReplacementCharactersDoNotCorruptTheMarkup() {
        String html = "<style>\np { font-family: \"Helvetica Neue\"; }\n.price { color: red; }\n</style>"
                + "<p data-x=\"$1\">A</p><span class=\"price\">$2 \\o/</span>";
        String out = EmailCssUtils.convertCssToInline(html);
        assertTrue(out.contains("<p data-x=\"$1\" style=\"font-family:'Helvetica Neue';\">A</p>"), out);
        assertTrue(out.contains("<span class=\"price\" style=\"color:red;\">$2 \\o/</span>"), out);
    }

    @Test
    void classSelectorDoesNotMatchHyphenatedClass() {
        String html = "<style>\n.btn { color: red; }\n</style><a class=\"btn-primary\">Go</a><a class=\"x btn\">Ok</a>";
        String out = EmailCssUtils.convertCssToInline(html);
        assertTrue(out.contains("<a class=\"btn-primary\">Go</a>"), out);
        assertTrue(out.contains("<a class=\"x btn\" style=\"color:red;\">Ok</a>"), out);
    }
}
