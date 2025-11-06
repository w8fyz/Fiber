package sh.fyz.fiber.dashboard;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardRegistry {

    private final Map<String, AbstractDashboard> dashboards = new LinkedHashMap<>();

    public void register(AbstractDashboard dashboard) {
        dashboards.put(dashboard.getId(), dashboard);
    }

    public AbstractDashboard get(String id) {
        return dashboards.get(id);
    }

    public Collection<AbstractDashboard> list() {
        return Collections.unmodifiableCollection(dashboards.values());
    }
}
