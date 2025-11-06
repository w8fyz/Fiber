package sh.fyz.fiber.dashboard;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.Map;

public interface DashboardRequestFilter {
    boolean allow(Map<String, Object> body, UserAuth user);
}

