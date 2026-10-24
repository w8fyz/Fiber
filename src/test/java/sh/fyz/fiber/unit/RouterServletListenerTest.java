package sh.fyz.fiber.unit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.request.CompletedRequest;
import sh.fyz.fiber.core.request.RequestListener;
import sh.fyz.fiber.handler.RouterServlet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * The outcome reported when an exception escapes the router, a path no endpoint can trigger since
 * handler failures are already turned into a 500 response.
 */
class RouterServletListenerTest {

    private final List<CompletedRequest> completed = new CopyOnWriteArrayList<>();
    private final RequestListener recorder = completed::add;

    private static HttpServletRequest request(String method, String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        Map<String, Object> attributes = new HashMap<>();
        when(req.getMethod()).thenReturn(method);
        when(req.getRequestURI()).thenReturn(uri);
        doAnswer(inv -> attributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(req).setAttribute(anyString(), any());
        when(req.getAttribute(anyString())).thenAnswer(inv -> attributes.get(inv.<String>getArgument(0)));
        return req;
    }

    private static EndpointRegistry failingRegistry() {
        EndpointRegistry registry = mock(EndpointRegistry.class);
        when(registry.getEndpoints()).thenThrow(new AssertionError("Intentional routing failure"));
        return registry;
    }

    @Test
    void anEscapingExceptionIsReportedAsAServerError() {
        RouterServlet servlet = new RouterServlet(failingRegistry(), List.of(recorder));
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getStatus()).thenReturn(HttpServletResponse.SC_OK);
        when(resp.isCommitted()).thenReturn(false);

        assertThrows(AssertionError.class, () -> servlet.service(request("GET", "/anything"), resp));

        assertEquals(1, completed.size());
        CompletedRequest request = completed.get(0);
        assertEquals(500, request.status(), "Jetty answers 500 for an exception escaping the servlet");
        assertEquals("GET", request.method());
        assertEquals("/anything", request.path());
        assertNull(request.route());
        assertNotNull(request.requestId());
    }

    @Test
    void anEscapingExceptionKeepsTheStatusAlreadySent() {
        RouterServlet servlet = new RouterServlet(failingRegistry(), List.of(recorder));
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getStatus()).thenReturn(HttpServletResponse.SC_OK);
        when(resp.isCommitted()).thenReturn(true);

        assertThrows(AssertionError.class, () -> servlet.service(request("GET", "/anything"), resp));

        assertEquals(1, completed.size());
        assertEquals(200, completed.get(0).status(), "A committed response has already sent its status");
    }

    @Test
    void rejectsANullListenerList() {
        EndpointRegistry registry = mock(EndpointRegistry.class);
        assertThrows(NullPointerException.class, () -> new RouterServlet(registry, null));
    }
}
