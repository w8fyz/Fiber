package sh.fyz.fiber.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.ee11.servlet.ErrorHandler;
import org.eclipse.jetty.ee11.servlet.ServletContextRequest;
import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.http.HttpURI;
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

/**
 * JSON error page for errors raised inside the servlet context (sendError, uncaught throwables).
 * Errors Jetty raises before the request reaches the context (bad URI, oversized headers...)
 * are rendered by {@link FiberServerErrorHandler}.
 */
public class FiberErrorHandler extends ErrorHandler {

    private static final FiberLogger logger = FiberLog.get(FiberErrorHandler.class);
    private static final FiberObjectMapper MAPPER = new FiberObjectMapper();
    // Jetty renders the page into a fixed-size buffer and truncates it on overflow.
    private static final int MAX_MESSAGE_LENGTH = 1024;

    public FiberErrorHandler() {
        setShowStacks(false);
        setShowCauses(false);
        setShowOrigin(false);
    }

    // Jetty only writes a body for GET/POST/HEAD by default; a JSON API answers errors on any
    // method. HEAD bodies and no-body statuses (204, 304) are still dropped by Jetty.
    @Override
    public boolean errorPageForMethod(String method) {
        return true;
    }

    // Always JSON, whatever the Accept header asks for. "application/json" with UTF-8 is always
    // acceptable, and ee11's handle() completes the callback itself.
    @Override
    protected void generateResponse(Request request, Response response, int code, String message,
                                    Throwable cause, Callback callback) throws IOException {
        logger.warn("jetty error {} {} status={}", request.getMethod(), request.getHttpURI().getPath(), code);
        generateAcceptableResponse(request, response, callback, "application/json",
                List.of(StandardCharsets.UTF_8), code, message, cause);
    }

    @Override
    protected void writeErrorJson(Request request, PrintWriter writer, int code, String message, Throwable cause) {
        ServletContextRequest servletRequest = Request.asInContext(request, ServletContextRequest.class);
        HttpServletRequest httpRequest = servletRequest != null ? servletRequest.getServletApiRequest() : null;
        String url = httpRequest != null
                ? httpRequest.getRequestURL().toString()
                : urlWithoutQuery(request);
        writer.write(errorJson(url, code, message, cause));
    }

    static String urlWithoutQuery(Request request) {
        return HttpURI.build(request.getHttpURI()).query(null).asString();
    }

    static String errorJson(String url, int code, String message, Throwable cause) {
        // For an uncaught throwable Jetty uses cause.toString() as the message: never echo it.
        // HttpException reasons (e.g. "Ambiguous URI path separator") are meant for the client.
        boolean internalCause = cause != null && !(cause instanceof HttpException);
        if (internalCause || message == null || message.isEmpty()) {
            message = "Unexpected error";
        } else if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", url);
        body.put("status", code);
        body.put("message", message);

        try {
            return MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            return "{\"status\":" + code + "}";
        }
    }

}
