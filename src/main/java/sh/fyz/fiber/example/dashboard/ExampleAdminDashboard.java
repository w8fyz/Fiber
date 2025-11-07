package sh.fyz.fiber.example.dashboard;

import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.dashboard.*;
import sh.fyz.fiber.dashboard.crud.CrudCapability;
import sh.fyz.fiber.dashboard.crud.DashboardEntityDataProvider;
import sh.fyz.fiber.dashboard.entity.DashboardEntity;
import sh.fyz.fiber.example.ExampleMain;
import sh.fyz.fiber.example.repo.entities.ExampleUser;

import java.util.HashMap;
import java.util.Map;

public class ExampleAdminDashboard extends AbstractDashboard {

    public ExampleAdminDashboard() {
        super("admin-example", "Admin Example", "Demonstrates actions, filters, and transforms");

        addEntity(new DashboardEntity<>(ExampleUser.class, "User")
                .withCapabilities(CrudCapability.values())
                .withDataProvider(new GenericDashboardEntityDataProvider<>(
                        ExampleMain.exampleUserRepository
                )));

        addAction(new DashboardAction(
            "Create User",
            Map.of("email", "string", "name", "string", "role", "string"),
            (body, user) -> {
                Map<String, Object> created = new HashMap<>();
                created.put("id", 123);
                created.put("email", body.get("email"));
                created.put("name", body.get("name"));
                created.put("role", body.get("role"));
                created.put("createdBy", user.getId());
                return created;
            }
        )
        .addRequestFilter((body, user) -> user != null && user.getRole().equals("ADMIN"))
        .addRequestTransformer((body, user) -> {
            if (body.containsKey("email")) {
                Object v = body.get("email");
                if (v instanceof String) body.put("email", ((String) v).trim().toLowerCase());
            }
            return body;
        })
        .addResponseTransformer((response, user) -> response)
        );

        addAction(new DashboardAction(
            "Delete User",

            Map.of("id", "number"),
            (body, user) -> {
                return Map.of("deleted", true, "id", body.get("id"));
            }
        ));
    }
}
