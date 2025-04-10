package sh.fyz.fiber.core.security;

import java.util.*;

/**
 * Gestionnaire de la hiérarchie des rôles et de leurs permissions associées.
 */
public class RoleHierarchy {
    private final Map<String, Set<String>> roleHierarchy;
    private final Map<String, Set<Permission>> rolePermissions;

    public RoleHierarchy() {
        this.roleHierarchy = new HashMap<>();
        this.rolePermissions = new HashMap<>();
    }

    /**
     * Ajoute une implication de rôle (higherRole implique lowerRole)
     * @param higherRole Le rôle supérieur
     * @param lowerRole Le rôle inférieur
     */
    public void addRoleImplication(String higherRole, String lowerRole) {
        roleHierarchy.computeIfAbsent(higherRole, k -> new HashSet<>()).add(lowerRole);
    }

    /**
     * Ajoute une permission à un rôle
     * @param role Le rôle
     * @param permission La permission à ajouter
     */
    public void addPermissionToRole(String role, Permission permission) {
        rolePermissions.computeIfAbsent(role, k -> new HashSet<>()).add(permission);
    }

    /**
     * Vérifie si un utilisateur avec les rôles donnés a un rôle spécifique
     * @param userRoles Les rôles de l'utilisateur
     * @param requiredRole Le rôle requis
     * @return true si l'utilisateur a le rôle requis ou un rôle supérieur
     */
    public boolean hasRole(Set<String> userRoles, String requiredRole) {
        if (userRoles.contains(requiredRole)) {
            return true;
        }

        return userRoles.stream().anyMatch(userRole ->
            hasImpliedRole(userRole, requiredRole, new HashSet<>())
        );
    }

    /**
     * Vérifie si un utilisateur avec les rôles donnés a une permission spécifique
     * @param userRoles Les rôles de l'utilisateur
     * @param requiredPermission La permission requise
     * @return true si l'utilisateur a la permission requise
     */
    public boolean hasPermission(Set<String> userRoles, Permission requiredPermission) {
        return userRoles.stream().anyMatch(role -> {
            Set<Permission> permissions = getAllPermissions(role, new HashSet<>());
            return permissions.stream().anyMatch(permission -> permission.implies(requiredPermission));
        });
    }

    private boolean hasImpliedRole(String currentRole, String targetRole, Set<String> visited) {
        if (visited.contains(currentRole)) {
            return false;
        }
        visited.add(currentRole);

        Set<String> impliedRoles = roleHierarchy.get(currentRole);
        if (impliedRoles == null) {
            return false;
        }

        if (impliedRoles.contains(targetRole)) {
            return true;
        }

        return impliedRoles.stream()
            .anyMatch(role -> hasImpliedRole(role, targetRole, visited));
    }

    private Set<Permission> getAllPermissions(String role, Set<String> visited) {
        if (visited.contains(role)) {
            return Collections.emptySet();
        }
        visited.add(role);

        Set<Permission> permissions = new HashSet<>(rolePermissions.getOrDefault(role, Collections.emptySet()));
        
        Set<String> impliedRoles = roleHierarchy.get(role);
        if (impliedRoles != null) {
            impliedRoles.forEach(impliedRole ->
                permissions.addAll(getAllPermissions(impliedRole, visited))
            );
        }

        return permissions;
    }
} 