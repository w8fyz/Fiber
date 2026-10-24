package sh.fyz.fiber;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Paths written without a leading {@code /} are joined like rooted ones, whichever way the
 * controller is registered.
 */
public class RelativePathRoutingTest extends IntegrationTestBase {

    @Test
    void aRelativeMappingUnderARootedBaseIsServedAtTheJoinedPath() throws Exception {
        HttpResponse<String> resp = get("/mixed/ping");
        assertEquals(200, resp.statusCode(), resp.body());
        assertTrue(resp.body().contains("mixed"));
    }

    @Test
    void aRelativeControllerRegisteredAsAnInstanceIsServedUnderItsRootedPath() throws Exception {
        HttpResponse<String> ping = get("/relative/ping");
        assertEquals(200, ping.statusCode(), ping.body());
        assertTrue(ping.body().contains("relative"));

        HttpResponse<String> item = get("/relative/items/7");
        assertEquals(200, item.statusCode(), item.body());
        assertTrue(item.body().contains("\"7\""));
    }

    @Test
    void aRelativeControllerRegisteredByClassIsServedUnderItsRootedPath() throws Exception {
        HttpResponse<String> resp = get("/relative-class/ping");
        assertEquals(200, resp.statusCode(), resp.body());
        assertTrue(resp.body().contains("relative-class"));
    }

    @Test
    void aRelativeMappingUnderARootedBaseRegisteredByClassIsNoLongerGluedToIt() throws Exception {
        HttpResponse<String> joined = get("/mixed-class/ping");
        assertEquals(200, joined.statusCode(), joined.body());
        assertTrue(joined.body().contains("mixed-class"));

        assertEquals(404, get("/mixed-classping").statusCode(),
                "Registering by class used to serve the two parts glued together");
    }
}
