package sh.fyz.fiber.unit;

import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.dto.DTOConvertible;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DTOConvertibleTest {

    static class Item extends DTOConvertible {
        private static final long serialVersionUID = 1L;
        private static final Object SHARED = new Object();
        private final String name = "widget";
    }

    @Test
    void staticFieldsAreNotExported() {
        Map<String, Object> dto = new Item().asDTO();
        assertEquals(Map.of("name", "widget"), dto);
    }
}
