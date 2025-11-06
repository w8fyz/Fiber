package sh.fyz.fiber.dashboard;

import sh.fyz.fiber.dashboard.entity.DashboardEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class AbstractDashboard {

    private final String id;
    private final String title;
    private final String description;
    private final List<DashboardAction> actions;
    private final List<DashboardEntity<?>> entities;

    public AbstractDashboard(String id, String title, String description) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.description = description == null ? "" : description;
        this.actions = new ArrayList<>();
        this.entities = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<DashboardAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public List<DashboardEntity<?>> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    protected void addAction(DashboardAction action) {
        this.actions.add(action);
    }

    protected void addEntity(DashboardEntity<?> entity) {
        this.entities.add(entity);
    }
}
