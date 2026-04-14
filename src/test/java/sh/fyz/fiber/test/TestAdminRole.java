package sh.fyz.fiber.test;

import sh.fyz.fiber.core.authentication.entities.Role;

public class TestAdminRole extends Role {
    public TestAdminRole() { super("admin"); }

    @Override
    protected void initializePermissions() {
        addPermission("admin:manage");
    }

    @Override
    public void initializeParentRoles() {
        addParentRole(new TestUserRole());
    }
}
