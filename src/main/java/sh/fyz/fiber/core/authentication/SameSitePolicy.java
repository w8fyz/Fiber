package sh.fyz.fiber.core.authentication;

public enum SameSitePolicy {
    STRICT("Strict"),
    LAX("Lax"),
    NONE("None");

    private final String value;

    SameSitePolicy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
} 