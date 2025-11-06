package sh.fyz.fiber.dashboard;

import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.annotations.params.Param;
import sh.fyz.fiber.annotations.params.RequestBody;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.params.PathVariable;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.List;
import java.util.Map;

@Controller("/dashboards")
public class DashboardController {

    private DashboardService service() {
        return FiberServer.get().getDashboardService();
    }

    @RequestMapping(value = "", method = RequestMapping.Method.GET)
    public ResponseEntity<List<Map<String, Object>>> listDashboards() {
        return ResponseEntity.ok(service().listDashboards());
    }

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, Object>> dashboardDetails(@PathVariable("id") String id) {
        Map<String, Object> d = service().getDashboard(id);
        if (d == null) return ResponseEntity.notFound();
        return ResponseEntity.ok(d);
    }

    @RequestMapping(value = "/{id}/actions/{actionId}", method = RequestMapping.Method.POST)
    public ResponseEntity<Object> executeAction(
            @PathVariable("id") String id,
            @PathVariable("actionId") String actionId,
            @RequestBody Map<String, Object> body,
            @AuthenticatedUser UserAuth user
    ) throws Exception {
        Object result = service().executeAction(id, actionId, body, user);
        return ResponseEntity.ok(result);
    }

    // CRUD endpoints

    @RequestMapping(value = "/{id}/entities/{entity}/list", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, Object>> listEntity(
            @PathVariable("id") String id,
            @PathVariable("entity") String entity,
            @Param(value = "page", required = false) Integer page,
            @Param(value = "size", required = false) Integer size,
            @Param(value = "q", required = false) String q
    ) {
        return ResponseEntity.ok(service().listEntity(id, entity, page, size, q));
    }

    @RequestMapping(value = "/{id}/entities/{entity}/{entityId}", method = RequestMapping.Method.GET)
    public ResponseEntity<Object> getEntity(
            @PathVariable("id") String id,
            @PathVariable("entity") String entity,
            @PathVariable("entityId") String entityId
    ) {
        Object result = service().getEntity(id, entity, entityId);
        if (result == null) return ResponseEntity.notFound();
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/{id}/entities/{entity}", method = RequestMapping.Method.POST)
    public ResponseEntity<Object> createEntity(
            @PathVariable("id") String id,
            @PathVariable("entity") String entity,
            @RequestBody Map<String, Object> body
    ) {
        Object result = service().createEntity(id, entity, body);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/{id}/entities/{entity}/{entityId}", method = RequestMapping.Method.PUT)
    public ResponseEntity<Object> updateEntity(
            @PathVariable("id") String id,
            @PathVariable("entity") String entity,
            @PathVariable("entityId") String entityId,
            @RequestBody Map<String, Object> body
    ) {
        Object result = service().updateEntity(id, entity, entityId, body);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/{id}/entities/{entity}/{entityId}", method = RequestMapping.Method.DELETE)
    public ResponseEntity<Map<String, Object>> deleteEntity(
            @PathVariable("id") String id,
            @PathVariable("entity") String entity,
            @PathVariable("entityId") String entityId
    ) {
        boolean ok = service().deleteEntity(id, entity, entityId);
        return ResponseEntity.ok(Map.of("deleted", ok));
    }
}
