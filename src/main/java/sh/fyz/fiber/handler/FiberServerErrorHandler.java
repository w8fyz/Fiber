package sh.fyz.fiber.handler;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.Callback;
import sh.fyz.fiber.core.log.FiberLog;
import sh.fyz.fiber.core.log.FiberLogger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JSON error page for errors Jetty raises before the request reaches the servlet context:
 * ambiguous or malformed URIs (e.g. an encoded slash), oversized headers, bad request lines.
 * Same body as {@link FiberErrorHandler}.
 */
public class FiberServerErrorHandler extends ErrorHandler {

    private static final FiberLogger logger = FiberLog.get(FiberServerErrorHandler.class);

    public FiberServerErrorHandler() {
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
        writer.write(FiberErrorHandler.errorJson(FiberErrorHandler.urlWithoutQuery(request), code, message, cause));
    }

}
