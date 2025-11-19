package sh.fyz.fiber.dashboard;

import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.dashboard.crud.CrudCapability;
import sh.fyz.fiber.dashboard.crud.DashboardEntityDataProvider;
import sh.fyz.fiber.dashboard.crud.Page;
import sh.fyz.fiber.dashboard.entity.DashboardEntity;
import sh.fyz.fiber.dashboard.entity.DashboardEntityField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DashboardService {

    private final DashboardRegistry registry;

    public DashboardService(DashboardRegistry registry) {
        this.registry = registry;
    }

    public List<Map<String, Object>> listDashboards() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AbstractDashboard d : registry.list()) {
            list.add(toDashboardMeta(d));
        }
        return list;
    }

    public Map<String, Object> getDashboard(String id) {
        AbstractDashboard d = registry.get(id);
        if (d == null) return null;
        return toDashboardMeta(d);
    }

    public Object executeAction(String dashboardId, String actionId, Map<String, Object> body, UserAuth user) throws Exception {
        AbstractDashboard dashboard = requireDashboard(dashboardId);
        DashboardAction action = dashboard.getActions().stream()
            .filter(a -> a.getId().equals(actionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown action: " + actionId));
        if (action.getExecutor() == null) throw new IllegalStateException("Action has no executor: " + actionId);

        Map<String, Object> effectiveBody = body == null ? new HashMap<>() : new HashMap<>(body);

        if (!action.getRequestFilters().isEmpty()) {
            for (DashboardRequestFilter filter : action.getRequestFilters()) {
                if (!filter.allow(effectiveBody, user)) {
                    throw new SecurityException("Request rejected by filter for action " + actionId);
                }
            }
        }

        for (DashboardRequestTransformer transformer : action.getRequestTransformers()) {
            effectiveBody = transformer.transform(effectiveBody, user);
            if (effectiveBody == null) effectiveBody = new HashMap<>();
        }

        Object result = action.getExecutor().execute(effectiveBody, user);

        for (DashboardResponseTransformer transformer : action.getResponseTransformers()) {
            result = transformer.transform(result, user);
        }

        return result;
    }

    // CRUD

    public Map<String, Object> listEntity(String dashboardId, String entityName, Integer page, Integer size, String query) {
        DashboardEntity<?> entity = requireEntity(dashboardId, entityName);
        ensureCapability(entity, query == null ? CrudCapability.LIST : CrudCapability.SEARCH);
        DashboardEntityDataProvider provider = entity.getDataProvider();
        if (provider == null) throw new IllegalStateException("No data provider for entity " + entityName);
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        Page<?> pg = (query == null || query.isBlank()) ? provider.list(p, s) : provider.search(query, p, s);
        Map<String, Object> resp = new HashMap<>();
        resp.put("page", pg.getPage());
        resp.put("size", pg.getSize());
        resp.put("total", pg.getTotal());
        resp.put("items", pg.getItems());
        return resp;
    }

    public Object getEntity(String dashboardId, String entityName, Object id) {
        DashboardEntity<?> entity = requireEntity(dashboardId, entityName);
        ensureCapability(entity, CrudCapability.GET);
        DashboardEntityDataProvider provider = entity.getDataProvider();
        if (provider == null) throw new IllegalStateException("No data provider for entity " + entityName);
        return provider.getById(id);
    }

    public Object createEntity(String dashboardId, String entityName, Map<String, Object> body) {
        DashboardEntity<?> entity = requireEntity(dashboardId, entityName);
        ensureCapability(entity, CrudCapability.CREATE);
        DashboardEntityDataProvider provider = entity.getDataProvider();
        if (provider == null) throw new IllegalStateException("No data provider for entity " + entityName);
        return provider.create(body);
    }

    public Object updateEntity(String dashboardId, String entityName, Object id, Map<String, Object> body) {
        DashboardEntity<?> entity = requireEntity(dashboardId, entityName);
        ensureCapability(entity, CrudCapability.UPDATE);
        DashboardEntityDataProvider provider = entity.getDataProvider();
        if (provider == null) throw new IllegalStateException("No data provider for entity " + entityName);
        return provider.update(id, body);
    }

    public boolean deleteEntity(String dashboardId, String entityName, Object id) {
        DashboardEntity<?> entity = requireEntity(dashboardId, entityName);
        ensureCapability(entity, CrudCapability.DELETE);
        DashboardEntityDataProvider<?> provider = entity.getDataProvider();
        if (provider == null) throw new IllegalStateException("No data provider for entity " + entityName);
        return provider.delete(id);
    }

    // helpers

    private AbstractDashboard requireDashboard(String id) {
        AbstractDashboard d = registry.get(id);
        if (d == null) throw new IllegalArgumentException("Unknown dashboard: " + id);
        return d;
    }

    private DashboardEntity<?> requireEntity(String dashboardId, String entityName) {
        AbstractDashboard d = requireDashboard(dashboardId);
        for (DashboardEntity<?> e : d.getEntities()) {
            if (e.getName().equals(entityName)) return e;
        }
        throw new IllegalArgumentException("Unknown entity: " + entityName + " in dashboard " + dashboardId);
    }

    private void ensureCapability(DashboardEntity<?> e, CrudCapability needed) {
        Set<CrudCapability> caps = e.getCapabilities();
        if (!caps.contains(needed)) {
            throw new IllegalStateException("Entity does not support capability: " + needed);
        }
    }

    private Map<String, Object> toDashboardMeta(AbstractDashboard d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("title", d.getTitle());
        m.put("description", d.getDescription());

        List<Map<String, Object>> actions = new ArrayList<>();
        for (DashboardAction a : d.getActions()) {
            Map<String, Object> am = new HashMap<>();
            am.put("id", a.getId());
            am.put("label", a.getLabel());
            am.put("inputSchema", a.getInputSchema());
            actions.add(am);
        }
        m.put("actions", actions);

        List<Map<String, Object>> entities = new ArrayList<>();
        for (DashboardEntity<?> e : d.getEntities()) {
            Map<String, Object> em = new HashMap<>();
            em.put("name", e.getName());
            em.put("capabilities", e.getCapabilities());
            List<Map<String, Object>> fields = new ArrayList<>();
            for (DashboardEntityField f : e.getFields()) {
                Map<String, Object> fm = new HashMap<>();
                fm.put("name", f.getFieldName());
                fm.put("label", f.getDisplayName());
                fm.put("type", f.getType());
                fm.put("attributes", f.getAttributes());
                fields.add(fm);
            }
            em.put("fields", fields);
            entities.add(em);
        }
        m.put("entities", entities);

        return m;
    }
}
