package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.util.FiberObjectMapper;

import java.io.IOException;

public class ResponseWriter {

    private static final FiberObjectMapper MAPPER = new FiberObjectMapper();

    public static void write(Object result, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (resp.isCommitted()) {
            return;
        }

        if (result instanceof ResponseEntity<?> entity) {
            entity.write(req, resp);
        } else if (result != null) {
            Object serializable = ResponseEntity.prepareForSerialization(result);
            byte[] json = MAPPER.writeValueAsBytes(serializable);
            resp.setContentType("application/json");
            resp.setContentLength(json.length);
            resp.getOutputStream().write(json);
            resp.getOutputStream().flush();
        }
    }
}
