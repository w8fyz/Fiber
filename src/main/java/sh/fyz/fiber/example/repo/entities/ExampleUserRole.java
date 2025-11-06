package sh.fyz.fiber.example.repo.entities;

import sh.fyz.fiber.core.authentication.entities.Role;

public class ExampleUserRole extends Role {
    protected ExampleUserRole() {
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
