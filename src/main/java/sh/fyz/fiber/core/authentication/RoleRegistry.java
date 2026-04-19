package sh.fyz.fiber.core.authentication;

import sh.fyz.fiber.core.authentication.entities.Role;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoleRegistry {
    private final Map<String, Role> roles;
    private final Map<String, Class<? extends Role>> roleClasses;
    private volatile boolean frozen = false;

    public RoleRegistry() {
        this.roles = new ConcurrentHashMap<>();
        this.roleClasses = new ConcurrentHashMap<>();
    }

    /**
     * Freeze the registry. Subsequent calls to {@link #registerRoleClass} or
     * {@link #registerRoleClasses} throw {@link IllegalStateException}.
     * Called automatically by {@code FiberServer.start()}.
     */
    public synchronized void freeze() {
        this.frozen = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Register a role class in the system
     * @param roleClass The role class to register
     */
    public synchronized void registerRoleClass(Class<? extends Role> roleClass) {
        if (frozen) {
            throw new IllegalStateException(
                    "Roles cannot be registered after FiberServer has started");
        }
        try {
            Role role = createRoleInstance(roleClass);
            String identifier = role.getIdentifier();
            roleClasses.put(identifier, roleClass);
            roles.put(identifier, role);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register role class: " + roleClass.getName(), e);
        }
    }

    /**
     * Register multiple role classes in the system
     * @param roleClasses The role classes to register
     */
    @SafeVarargs
    public final synchronized void registerRoleClasses(Class<? extends Role>... roleClasses) {
        if (frozen) {
            throw new IllegalStateException(
                    "Roles cannot be registered after FiberServer has started");
        }
        for (Class<? extends Role> roleClass : roleClasses) {
            registerRoleClass(roleClass);
        }
        setupParentRoles();
    }
    
    /**
     * Set up parent roles for all registered roles
     */
    private void setupParentRoles() {
        // Get all registered roles
        for (Role role : roles.values()) {
            // Get the role class
            Class<? extends Role> roleClass = roleClasses.get(role.getIdentifier());
            
            // Create a new instance to get parent role information
            try {
                Role tempRole = createRoleInstance(roleClass);
                
                // Get parent roles from the temporary instance
                for (Role parentRole : tempRole.getParentRoles()) {
                    // Find the actual parent role instance in the registry
                    Role actualParentRole = roles.get(parentRole.getIdentifier());
                    if (actualParentRole != null) {
                        // Add the parent role to the actual role instance
                        role.addParentRole(actualParentRole);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to set up parent roles for: " + role.getIdentifier(), e);
            }
        }
    }

    /**
     * Get a role by its identifier
     */
    public Role getRole(String identifier) {
        return roles.get(identifier);
    }

    /**
     * Check if a role exists
     */
    public boolean hasRole(String identifier) {
        return roles.containsKey(identifier);
    }

    /**
     * Check if a role has a specific permission
     */
    public boolean hasPermission(String roleIdentifier, String permission) {
        Role role = getRole(roleIdentifier);
        return role != null && role.hasPermission(permission);
    }

    /**
     * Check if a role has all the specified permissions
     */
    public boolean hasAllPermissions(String roleIdentifier, Set<String> permissions) {
        Role role = getRole(roleIdentifier);
        return role != null && role.hasAllPermissions(permissions);
    }

    /**
     * Get all permissions for a role
     */
    public Set<String> getRolePermissions(String roleIdentifier) {
        Role role = getRole(roleIdentifier);
        return role != null ? role.getPermissions() : Set.of();
    }

    /**
     * Create a new instance of a role class
     */
    private Role createRoleInstance(Class<? extends Role> roleClass) 
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<? extends Role> constructor = roleClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
} 