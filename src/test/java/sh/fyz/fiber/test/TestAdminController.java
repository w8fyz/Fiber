package sh.fyz.fiber.test;

import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.security.AuthType;
import sh.fyz.fiber.annotations.security.RequireRole;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.Map;

@Controller("/admin")
public class TestAdminController {

    @RequestMapping(value = "/stats", method = RequestMapping.Method.GET)
    @AuthType({AuthScheme.COOKIE, AuthScheme.BEARER})
    @RequireRole("admin")
    public Map<String, Object> stats(@AuthenticatedUser UserAuth user) {
        return Map.of("admin", true, "userId", user.getId());
    }
}
