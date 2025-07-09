package sh.fyz.fiber.core.security.processors;

import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.security.Permission;
import sh.fyz.fiber.annotations.security.RequireRole;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthMiddleware;
import sh.fyz.fiber.core.authentication.entities.Role;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.interceptors.RateLimitInterceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class PermissionProcessor {
    public static Object process(Method method, Object[] args, HttpServletRequest request) {
        //System.out.println("Processing method: " + method.getName());

        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        Permission permission = method.getAnnotation(Permission.class);

        //System.out.println("RequireRole annotation: " + requireRole);
        //System.out.println("Permission annotation: " + permission);

        boolean hasRole = true;
        boolean hasPermission = true;

        UserAuth user = AuthMiddleware.getCurrentUser(request);
        //System.out.println("Current user: " + user);

        if (requireRole != null && requireRole.value() != null && !checkRole(user, requireRole)) {
            hasRole = false;
        }
        if (permission != null && permission.value() != null && !checkPermission(user, permission)) {
            hasPermission = false;
        }

        if (!hasRole || !hasPermission) {
            String message = "Access denied, you do not have the required permissions or role to access this resource.";
            //System.out.println("Access denied: hasRole=" + hasRole + ", hasPermission=" + hasPermission);
            return ResponseEntity.unauthorized(message);
        }

        //System.out.println("Access granted.");
        return null;
    }

    private static boolean checkPermission(UserAuth user, Permission permission) {
        //System.out.println("Checking permissions for user: " + user);
        if (user == null) {
            //System.out.println("User is null, permission check failed.");
            return false;
        }
        if (user.getRole() == null) {
            //System.out.println("User role is null, permission check failed.");
            return false;
        }
        List<String> permissions = Arrays.asList(permission.value());
        Role userRole = FiberServer.get().getRoleRegistry().getRole(user.getRole());
        //System.out.println("User role: " + userRole + ", Required permissions: " + permissions);
        boolean result = userRole.getPermissions().containsAll(permissions);
        //System.out.println("Permission check result: " + result);
        return result;
    }

    private static boolean checkRole(UserAuth user, RequireRole requireRole) {
        //System.out.println("Checking roles for user: " + user);
        if (user == null) {
            //System.out.println("User is null, role check failed.");
            return false;
        }
        if (user.getRole() == null) {
            //System.out.println("User role is null, role check failed.");
            return false;
        }
        String role = user.getRole();
        List<String> roles = Arrays.asList(requireRole.value());
        //System.out.println("User role: " + role + ", Required roles: " + roles);
        boolean result = role != null && roles.contains(role);
        //System.out.println("Role check result: " + result);
        return result;
    }
}