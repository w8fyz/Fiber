package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DtoTest extends IntegrationTestBase {

    @Test
    @Order(1)
    void testDtoConvertibleFields() throws Exception {
        HttpResponse<String> resp = get("/test/dto");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"name\""));
        assertTrue(resp.body().contains("\"test\""));
        assertTrue(resp.body().contains("\"age\""));
        assertTrue(resp.body().contains("25"));
    }

    @Test
    @Order(2)
    void testIgnoreDtoFieldExcluded() throws Exception {
        HttpResponse<String> resp = get("/test/dto");
        assertEquals(200, resp.statusCode());
        assertFalse(resp.body().contains("\"secret\""), "Field with @IgnoreDTO should not appear in response");
        assertFalse(resp.body().contains("hidden"), "Value of @IgnoreDTO field should not appear");
    }
}
