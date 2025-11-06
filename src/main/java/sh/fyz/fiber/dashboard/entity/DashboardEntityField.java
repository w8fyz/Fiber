package sh.fyz.fiber.dashboard.entity;

public class DashboardEntityField {

    private final String fieldName;
    private final String displayName;
    private final String type;

    // Constraints
    private boolean required;
    private Integer min;
    private boolean email;

    public DashboardEntityField(String fieldName, String displayName, String type) {
        this.fieldName = fieldName;
        this.displayName = displayName;
        this.type = type;
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

    public boolean isRequired() { return required; }
    public Integer getMin() { return min; }
    public boolean isEmail() { return email; }

    public void setRequired(boolean required) { this.required = required; }
    public void setMin(Integer min) { this.min = min; }
    public void setEmail(boolean email) { this.email = email; }
}
