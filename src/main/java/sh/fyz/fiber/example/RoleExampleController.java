package sh.fyz.fiber.example;

import sh.fyz.fiber.annotations.Controller;
import sh.fyz.fiber.annotations.Permission;
import sh.fyz.fiber.annotations.RequireRole;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.RoleRegistry;
import sh.fyz.fiber.core.authentication.entities.Role;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.annotations.AuthenticatedUser;

import java.util.HashMap;
import java.util.Map;

/**
 * Example controller demonstrating the role and permission system.
 */
@Controller("/roles")
public class RoleExampleController {

    /**
     * This endpoint requires the "admin" role.
     */
    @RequireRole("admin")
    @RequestMapping(value = "/admin-only", method = RequestMapping.Method.GET)
    public ResponseEntity<String> adminOnlyEndpoint() {
        return ResponseEntity.ok("You have access to the admin-only endpoint");
    }

    /**
     * This endpoint requires the "moderator" role.
     */
    @RequireRole("moderator")
    @RequestMapping(value = "/moderator-only", method = RequestMapping.Method.GET)
    public ResponseEntity<String> moderatorOnlyEndpoint() {
        return ResponseEntity.ok("You have access to the moderator-only endpoint");
    }

    /**
     * This endpoint requires the "manage_users" permission.
     */
    @Permission("manage_users")
    @RequestMapping(value = "/manage-users", method = RequestMapping.Method.GET)
    public ResponseEntity<String> manageUsersEndpoint() {
        return ResponseEntity.ok("You have permission to manage users");
    }

    /**
     * This endpoint requires both the "admin" role and the "manage_roles" permission.
     */
    @RequireRole("admin")
    @Permission("manage_roles")
    @RequestMapping(value = "/manage-roles", method = RequestMapping.Method.GET)
    public ResponseEntity<String> manageRolesEndpoint() {
        return ResponseEntity.ok("You have permission to manage roles");
    }

    /**
     * This endpoint requires the "view_content" permission.
     */
    @Permission("view_content")
    @RequestMapping(value = "/view-content", method = RequestMapping.Method.GET)
    public ResponseEntity<String> viewContentEndpoint() {
        return ResponseEntity.ok("You have permission to view content");
    }

    /**
     * This endpoint requires the "create_content" permission.
     */
    @Permission("create_content")
    @RequestMapping(value = "/create-content", method = RequestMapping.Method.POST)
    public ResponseEntity<String> createContentEndpoint(@AuthenticatedUser UserAuth user) {
        return ResponseEntity.ok("Content created by user: " + user.getUsername());
    }

    /**
     * This endpoint requires multiple permissions.
     */
    @Permission({"edit_content", "publish_content"})
    @RequestMapping(value = "/edit-and-publish", method = RequestMapping.Method.PUT)
    public ResponseEntity<String> editAndPublishEndpoint() {
        return ResponseEntity.ok("Content edited and published");
    }

    /**
     * This endpoint demonstrates how to get the current user's role and permissions.
     */
    @RequestMapping(value = "/my-permissions", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, Object>> myPermissionsEndpoint(@AuthenticatedUser UserAuth user) {
        String userRole = user.getRole();
        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("role", userRole);
        
        // Get permissions from the role registry
        RoleRegistry roleRegistry = sh.fyz.fiber.FiberServer.get().getRoleRegistry();
        Role role = roleRegistry.getRole(userRole);
        if (role != null) {
            result.put("permissions", role.getPermissions());
        }
        
        return ResponseEntity.ok(result);
    }
} 