package sh.fyz.fiber.test;

import sh.fyz.fiber.annotations.params.PathVariable;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.core.security.annotations.AuditLog;

import java.util.Map;

/** An audited endpoint with a path variable in its base path and one leading its mapping. */
@Controller("/audit-vars/{tenant}")
public class TestAuditPathController {

    @RequestMapping(value = "/{id}", method = RequestMapping.Method.GET)
    @AuditLog(action = "PATH_VARS", logParameters = false, logResult = false)
    public Map<String, String> item(@PathVariable("tenant") String tenant, @PathVariable("id") String id) {
        return Map.of("tenant", tenant, "id", id);
    }
}
