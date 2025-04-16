package sh.fyz.fiber.core.security.csrf;

import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.middleware.impl.CsrfMiddleware;

@Controller("/internal/csrf")
public class CsrfController {

    @RequestMapping(value = "/token", method = RequestMapping.Method.GET)
    public ResponseEntity<String> getCsrfToken(HttpServletResponse response) {
        CsrfMiddleware.generateNewToken(response);
        return ResponseEntity.ok("OK");
    }

}
