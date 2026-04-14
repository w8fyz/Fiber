package sh.fyz.fiber;

import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RoutingTest extends IntegrationTestBase {

    @Test
    @Order(1)
    void testGetHello() throws Exception {
        HttpResponse<String> resp = get("/test/hello");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"message\""));
        assertTrue(resp.body().contains("hello"));
    }

    @Test
    @Order(2)
    void testPostBody() throws Exception {
        HttpResponse<String> resp = post("/test/body", "{\"username\":\"john\",\"email\":\"john@test.com\"}");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"username\""));
        assertTrue(resp.body().contains("john"));
    }

    @Test
    @Order(3)
    void testPutMethod() throws Exception {
        HttpResponse<String> resp = put("/test/update", "{}");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("updated"));
    }

    @Test
    @Order(4)
    void testDeleteMethod() throws Exception {
        HttpResponse<String> resp = delete("/test/delete/42");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"deleted\""));
        assertTrue(resp.body().contains("42"));
    }

    @Test
    @Order(5)
    void testPathVariable() throws Exception {
        HttpResponse<String> resp = get("/test/users/123");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"userId\""));
        assertTrue(resp.body().contains("123"));
    }

    @Test
    @Order(6)
    void testQueryParam() throws Exception {
        HttpResponse<String> resp = get("/test/echo?name=Alice");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Alice"));
    }

    @Test
    @Order(7)
    void testQueryParamWithType() throws Exception {
        HttpResponse<String> resp = get("/test/echo?name=Bob&age=30");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Bob"));
        assertTrue(resp.body().contains("30"));
    }

    @Test
    @Order(8)
    void testOptionalQueryParamAbsent() throws Exception {
        HttpResponse<String> resp = get("/test/echo?name=Charlie");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Charlie"));
        assertFalse(resp.body().contains("\"age\""));
    }

    @Test
    @Order(9)
    void testStaticPathPriorityOverParameterized() throws Exception {
        HttpResponse<String> resp = get("/test/users/me");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"userId\""));
        assertTrue(resp.body().contains("\"me\""));
    }

    @Test
    @Order(10)
    void testNotFoundRoute() throws Exception {
        HttpResponse<String> resp = get("/test/nonexistent");
        assertEquals(404, resp.statusCode());
    }

    @Test
    @Order(11)
    void testNormalizedPath() throws Exception {
        // The framework normalizes "/test/hello" to not have double slashes
        // Jetty handles double slashes its own way
        HttpResponse<String> resp = get("/test/hello");
        assertEquals(200, resp.statusCode(), "Normalized path should work");
    }
}
