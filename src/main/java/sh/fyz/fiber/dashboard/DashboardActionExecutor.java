package sh.fyz.fiber.dashboard;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.Map;

@FunctionalInterface
public interface DashboardActionExecutor {
    Object execute(Map<String, Object> body, UserAuth authenticatedUser) throws Exception;
}
