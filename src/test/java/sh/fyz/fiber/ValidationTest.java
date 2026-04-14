package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ValidationTest extends IntegrationTestBase {

    @Test
    @Order(1)
    void testValidRequestBody() throws Exception {
        HttpResponse<String> resp = post("/test/body", "{\"username\":\"valid\",\"email\":\"valid@test.com\"}");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("valid"));
    }

    @Test
    @Order(2)
    void testInvalidRequestBodyBlankUsername() throws Exception {
        HttpResponse<String> resp = post("/test/body", "{\"username\":\"\",\"email\":\"valid@test.com\"}");
        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(3)
    void testInvalidRequestBodyBadEmail() throws Exception {
        HttpResponse<String> resp = post("/test/body", "{\"username\":\"valid\",\"email\":\"not-an-email\"}");
        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(4)
    void testRequiredParamMissing() throws Exception {
        HttpResponse<String> resp = get("/test/echo");
        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(5)
    void testMinValidationNegativeAge() throws Exception {
        HttpResponse<String> resp = get("/test/echo-validated?name=test&age=-1");
        assertEquals(400, resp.statusCode());
    }

    @Test
    @Order(6)
    void testMinValidationValidAge() throws Exception {
        HttpResponse<String> resp = get("/test/echo-validated?name=test&age=5");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("test"));
        assertTrue(resp.body().contains("5"));
    }

    @Test
    @Order(7)
    void testOptionalParamAbsent() throws Exception {
        HttpResponse<String> resp = get("/test/echo?name=test");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("test"));
        assertFalse(resp.body().contains("\"age\""));
    }
}
