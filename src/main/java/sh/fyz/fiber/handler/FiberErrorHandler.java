package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import sh.fyz.fiber.util.FiberObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class FiberErrorHandler extends ErrorHandler {

    private static final FiberObjectMapper MAPPER = new FiberObjectMapper();

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int status = response.getStatus();
        response.setStatus(status);
        response.setContentType("application/json");

        String message = (String) request.getAttribute("jakarta.servlet.error.message");
        if (message == null || message.isEmpty()) {
            message = "Unexpected error";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", request.getRequestURL().toString());
        body.put("status", status);
        body.put("message", message);

        PrintWriter writer = response.getWriter();
        MAPPER.writeValue(writer, body);
        writer.flush();
        baseRequest.setHandled(true);
    }

}
