package sh.fyz.fiber.dashboard;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

@FunctionalInterface
public interface DashboardResponseTransformer {
    Object transform(Object response, UserAuth user);
}
