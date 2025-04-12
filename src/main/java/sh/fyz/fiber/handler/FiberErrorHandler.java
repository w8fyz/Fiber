package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class FiberErrorHandler extends ErrorHandler {

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int status = response.getStatus();
        response.setStatus(status); // keep original status
        response.setContentType("application/json");

        String message = (String) request.getAttribute("jakarta.servlet.error.message");
        if (message == null || message.isEmpty()) {
            message = "Unexpected error";
        }

        String json = String.format("""
                    {
                      "url": "%s",
                      "status": %d,
                      "message": "%s"
                    }
                    """,
                request.getRequestURL(),
                status,
                message
        );

        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
        baseRequest.setHandled(true);
    }

}
