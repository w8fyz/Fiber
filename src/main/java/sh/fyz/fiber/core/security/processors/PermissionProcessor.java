package sh.fyz.fiber.core.security.processors;

import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.security.Permission;
import sh.fyz.fiber.annotations.security.RequireRole;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.entities.Role;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class PermissionProcessor {
    public static Object process(Method method, UserAuth user) {
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = method.getDeclaringClass().getAnnotation(RequireRole.class);
        }
        Permission permission = method.getAnnotation(Permission.class);
        if (permission == null) {
            permission = method.getDeclaringClass().getAnnotation(Permission.class);
        }

        boolean hasRole = true;
        boolean hasPermission = true;

        if (requireRole != null && requireRole.value() != null && !checkRole(user, requireRole)) {
            hasRole = false;
        }
        if (permission != null && permission.value() != null && !checkPermission(user, permission)) {
            hasPermission = false;
        }

        if (!hasRole || !hasPermission) {
            String message = "Access denied, you do not have the required permissions or role to access this resource.";
            return ResponseEntity.unauthorized(message);
        }
        return null;
    }

    private static boolean checkPermission(UserAuth user, Permission permission) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        List<String> permissions = Arrays.asList(permission.value());
        Role userRole = FiberServer.get().getRoleRegistry().getRole(user.getRole());
        return userRole.getPermissions().containsAll(permissions);
    }

    private static boolean checkRole(UserAuth user, RequireRole requireRole) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String role = user.getRole();
        List<String> roles = Arrays.asList(requireRole.value());
        return roles.contains(role);
    }
}
