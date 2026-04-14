package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ResponseTest extends IntegrationTestBase {

    @Test
    @Order(1)
    void testJsonResponse() throws Exception {
        HttpResponse<String> resp = get("/test/hello");
        assertEquals(200, resp.statusCode());
        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertTrue(contentType.contains("application/json") || contentType.contains("text/"),
                "Response should have JSON content type");
        assertTrue(resp.body().contains("\"message\""));
    }

    @Test
    @Order(2)
    void testBinaryResponse() throws Exception {
        HttpResponse<byte[]> resp = getBytes("/test/bytes");
        assertEquals(200, resp.statusCode());
        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("image/png", contentType);
        assertEquals(4, resp.body().length);
        assertEquals((byte) 0x89, resp.body()[0]);
    }

    @Test
    @Order(3)
    void testNullResponse() throws Exception {
        HttpResponse<String> resp = get("/test/null");
        // null return should not cause an error
        assertTrue(resp.statusCode() >= 200 && resp.statusCode() < 500,
                "Null response should not cause server error, got: " + resp.statusCode());
    }

    @Test
    @Order(4)
    void testCustomStatusCode() throws Exception {
        HttpResponse<String> resp = get("/test/status/404");
        assertEquals(404, resp.statusCode());
    }
}
