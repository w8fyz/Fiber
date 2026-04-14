package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.util.JsonUtil;

import java.io.IOException;

public class ResponseWriter {

    public static void write(Object result, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (resp.isCommitted()) {
            return;
        }

        if (result instanceof ResponseEntity<?> entity) {
            entity.write(req, resp);
        } else if (result != null) {
            resp.setContentType("application/json");
            resp.getWriter().write(JsonUtil.toJson(result));
        }
    }
}
