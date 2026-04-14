package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MiddlewareTest extends IntegrationTestBase {

    @Test
    @Order(1)
    void testMiddlewareSetsAttribute() throws Exception {
        HttpResponse<String> resp = get("/test/middleware-value");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("hello-from-middleware"),
                "Middleware should have set the attribute on the request");
    }

    @Test
    @Order(2)
    void testMiddlewareDoesNotBlock() throws Exception {
        HttpResponse<String> resp = get("/test/hello");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("hello"),
                "Middleware returning true should not block normal requests");
    }
}
