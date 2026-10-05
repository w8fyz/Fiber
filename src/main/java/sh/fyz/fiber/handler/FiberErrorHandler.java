package sh.fyz.fiber.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.ee11.servlet.ErrorHandler;
import org.eclipse.jetty.ee11.servlet.ServletContextRequest;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import sh.fyz.fiber.core.log.FiberLog;
import sh.fyz.fiber.core.log.FiberLogger;
import sh.fyz.fiber.util.FiberObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FiberErrorHandler extends ErrorHandler {

    private static final FiberLogger logger = FiberLog.get(FiberErrorHandler.class);
    private static final FiberObjectMapper MAPPER = new FiberObjectMapper();

    public FiberErrorHandler() {
        setShowStacks(false);
        setShowCauses(false);
        setShowOrigin(false);
    }

    // Always JSON, whatever the Accept header asks for.
    @Override
    protected void generateResponse(Request request, Response response, int code, String message,
                                    Throwable cause, Callback callback) throws IOException {
        logger.warn("jetty error {} {} status={}", request.getMethod(), request.getHttpURI().getPath(), code);
        if (!generateAcceptableResponse(request, response, callback, "application/json",
                List.of(StandardCharsets.UTF_8), code, message, cause)) {
            callback.succeeded();
        }
    }

    @Override
    protected void writeErrorJson(Request request, PrintWriter writer, int code, String message, Throwable cause) {
        if (message == null || message.isEmpty()) {
            message = "Unexpected error";
        }

        ServletContextRequest servletRequest = Request.asInContext(request, ServletContextRequest.class);
        HttpServletRequest httpRequest = servletRequest != null ? servletRequest.getServletApiRequest() : null;
        String url = httpRequest != null
                ? httpRequest.getRequestURL().toString()
                : request.getHttpURI().asString();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", url);
        body.put("status", code);
        body.put("message", message);

        try {
            writer.write(MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            writer.write("{\"status\":" + code + "}");
        }
    }

}
