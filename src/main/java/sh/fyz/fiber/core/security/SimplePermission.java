package sh.fyz.fiber.core.security;

import java.util.Objects;

/**
 * Implémentation simple d'une permission.
 */
public class SimplePermission implements Permission {
    private final String name;

    public SimplePermission(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean implies(Permission other) {
        if (!(other instanceof SimplePermission)) {
            return false;
        }
        return this.name.equals(other.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimplePermission that = (SimplePermission) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "SimplePermission{" +
                "name='" + name + '\'' +
                '}';
    }
} 