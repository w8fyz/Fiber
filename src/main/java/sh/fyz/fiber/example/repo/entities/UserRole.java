package sh.fyz.fiber.example.repo.entities;

import sh.fyz.fiber.core.authentication.entities.Role;

public class UserRole extends Role {
    protected UserRole() {
        super("user");
    }

    @Override
    protected void initializePermissions() {
        addPermission("user:read");
    }

    @Override
    public void initializeParentRoles() {
    }
}
