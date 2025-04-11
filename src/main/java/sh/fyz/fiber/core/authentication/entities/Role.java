package sh.fyz.fiber.core.authentication.entities;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for roles in the system with an identifier, permissions, and hierarchical relationships.
 * Specific roles should extend this class and define their permissions and parent roles.
 */
public abstract class Role {
    private final String identifier;
    private final Set<String> permissions;
    private final Set<Role> parentRoles;

    protected Role(String identifier) {
        this.identifier = identifier;
        this.permissions = new HashSet<>();
        this.parentRoles = new HashSet<>();
        
        // Initialize permissions and parent roles
        initializePermissions();
        initializeParentRoles();
    }

    /**
     * Initialize permissions for this role.
     * Override this method in subclasses to define role-specific permissions.
     */
    protected abstract void initializePermissions();

    /**
     * Initialize parent roles for this role.
     * Override this method in subclasses to define role hierarchy.
     */
    public abstract void initializeParentRoles();

    /**
     * Get the unique identifier of the role
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Add a permission to this role
     */
    protected void addPermission(String permission) {
        permissions.add(permission);
    }

    /**
     * Add multiple permissions to this role
     */
    protected void addPermissions(Set<String> permissions) {
        this.permissions.addAll(permissions);
    }

    /**
     * Get all permissions for this role
     */
    public Set<String> getPermissions() {
        return new HashSet<>(permissions);
    }

    /**
     * Add a parent role to establish hierarchy
     */
    public void addParentRole(Role parentRole) {
        parentRoles.add(parentRole);
    }

    /**
     * Get all parent roles
     */
    public Set<Role> getParentRoles() {
        return new HashSet<>(parentRoles);
    }

    /**
     * Check if this role has a specific permission
     */
    public boolean hasPermission(String permission) {
        if (permissions.contains(permission)) {
            return true;
        }
        // Check parent roles recursively
        for (Role parent : parentRoles) {
            if (parent.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this role has all the specified permissions
     */
    public boolean hasAllPermissions(Set<String> requiredPermissions) {
        return requiredPermissions.stream().allMatch(this::hasPermission);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return identifier.equals(role.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
} 