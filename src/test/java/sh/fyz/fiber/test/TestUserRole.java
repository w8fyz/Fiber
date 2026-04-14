package sh.fyz.fiber.test;

import sh.fyz.fiber.core.authentication.entities.Role;

public class TestUserRole extends Role {
    public TestUserRole() { super("user"); }

    @Override
    protected void initializePermissions() {
        addPermission("user:read");
    }

    @Override
    public void initializeParentRoles() {}
}
