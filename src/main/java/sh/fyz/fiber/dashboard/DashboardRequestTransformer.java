package sh.fyz.fiber.dashboard;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.Map;

@FunctionalInterface
public interface DashboardRequestTransformer {
    Map<String, Object> transform(Map<String, Object> body, UserAuth user);
}
