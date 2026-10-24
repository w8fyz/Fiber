package sh.fyz.fiber.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.docs.EndpointDoc;
import sh.fyz.fiber.handler.EndpointHandler;
import sh.fyz.fiber.test.TestRelativePathControllers;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndpointPathTest {

    @ParameterizedTest(name = "\"{0}\" + \"{1}\" -> {2}")
    @CsvSource(value = {
            // Rooted paths, the documented style: unchanged.
            "/users,/{id},/users/{id}",
            "/users,/,/users",
            "/users,'',/users",
            "'',/hello,/hello",
            "/,/,/",
            "/api/,/users/,/api/users",
            "/files,/*,/files/*",
            // Either side written without a leading slash.
            "/api,users,/api/users",
            "api,/users,/api/users",
            "api,users,/api/users",
            "'',users,/users",
            "api,'',/api",
            "NULL,users,/users",
    }, nullValues = "NULL")
    void joinsTheBasePathAndTheMappingWithOneSeparator(String basePath, String path, String expected) {
        assertEquals(expected, EndpointHandler.joinPath(basePath, path));
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(value = {
            "/api/users,/api/users",
            "api/users,/api/users",
            "//api//users//,/api/users",
            "/,/",
            "'',/",
            "NULL,/",
    }, nullValues = "NULL")
    void normalizesToARootedPathWithoutATrailingSlash(String path, String expected) {
        assertEquals(expected, EndpointHandler.normalizePath(path));
    }

    @Test
    void theLookupKeyAndTheHandlerRouteAreTheSamePathWhicheverWayItIsRegistered() throws Exception {
        EndpointRegistry registry = new EndpointRegistry(List.of());
        registry.registerController(new TestRelativePathControllers.Mixed());
        registry.registerController(TestRelativePathControllers.MixedByClass.class);
        registry.registerController(new TestRelativePathControllers.Relative());
        registry.registerController(TestRelativePathControllers.RelativeByClass.class);
        registry.registerEndpoint("explicit//ping/", RequestMapping.Method.GET,
                TestRelativePathControllers.Mixed.class.getMethod("ping"), new TestRelativePathControllers.Mixed());

        Map<String, String> routes = new TreeMap<>();
        registry.getEndpoints().forEach((key, handler) -> {
            assertEquals(handler.getRoute() + ":GET", key, "The key must be the route the handler matches");
            assertTrue(handler.matchesPath(handler.getRoute().replace("{id}", "7")),
                    "The handler must match its own route: " + handler.getRoute());
            routes.put(key, handler.getRoute());
        });
        assertEquals(Map.of(
                "/explicit/ping:GET", "/explicit/ping",
                "/mixed-class/ping:GET", "/mixed-class/ping",
                "/mixed/ping:GET", "/mixed/ping",
                "/relative-class/ping:GET", "/relative-class/ping",
                "/relative/items/{id}:GET", "/relative/items/{id}",
                "/relative/ping:GET", "/relative/ping"), routes);
    }

    @Test
    void theApiDocumentationShowsThePathTheEndpointIsServedAt() throws Exception {
        EndpointDoc doc = EndpointDoc.fromMethod(
                TestRelativePathControllers.Relative.class.getMethod("item", String.class), "relative");
        assertEquals("/relative/items/{id}", doc.getPath());
    }
}
