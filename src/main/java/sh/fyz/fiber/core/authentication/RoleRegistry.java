package sh.fyz.fiber.core.authentication;

import sh.fyz.fiber.core.authentication.entities.Role;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for managing roles and their hierarchies in the system.
 */
public class RoleRegistry {
    private final Map<String, Role> roles;
    private final Map<String, Class<? extends Role>> roleClasses;

    public RoleRegistry() {
        this.roles = new HashMap<>();
        this.roleClasses = new HashMap<>();
    }

    /**
     * Register a role class in the system
     * @param roleClass The role class to register
     */
    public void registerRoleClass(Class<? extends Role> roleClass) {
        try {
            // Create an instance to get the identifier
            Role role = createRoleInstance(roleClass);
            String identifier = role.getIdentifier();
            
            // Store the role class
            roleClasses.put(identifier, roleClass);
            
            // Create and store the role instance
            roles.put(identifier, role);
            
            System.out.println("Registered role: " + identifier);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register role class: " + roleClass.getName(), e);
        }
    }

    /**
     * Register multiple role classes in the system
     * @param roleClasses The role classes to register
     */
    public void registerRoleClasses(Class<? extends Role>... roleClasses) {
        // First, register all roles
        for (Class<? extends Role> roleClass : roleClasses) {
            registerRoleClass(roleClass);
        }
        
        // Then, set up parent roles
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
                System.err.println("Failed to set up parent roles for: " + role.getIdentifier());
                e.printStackTrace();
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