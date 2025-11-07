package sh.fyz.fiber.dashboard.entity;

import java.util.HashMap;
import java.util.Map;

public class DashboardEntityField {

    private final String fieldName;
    private final String displayName;
    private String type;

    private final Map<String, Object> attributes;

    public DashboardEntityField(String fieldName, String displayName, String type) {
        this.fieldName = fieldName;
        this.displayName = displayName;
        this.type = type;
        this.attributes = new HashMap<>();
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void set(String attributeName, Object value) {
        this.attributes.put(attributeName, value);
    }

    public Object getAttribute(String attributeName) {
        return this.attributes.get(attributeName);
    }
}
