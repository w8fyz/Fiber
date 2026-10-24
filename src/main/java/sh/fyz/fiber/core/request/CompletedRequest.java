package sh.fyz.fiber.core.request;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;

/**
 * A request the router has finished handling, as reported to every {@link RequestListener}.
 *
 * @param requestId       the id the request is logged under ({@code LogContext.requestId()})
 * @param method          the HTTP method
 * @param path            the request URI, without the query string
 * @param route           the path template of the endpoint that handled the request, as declared by
 *                        {@code @Controller} and {@code @RequestMapping} (e.g. {@code /users/{id}}),
 *                        or {@code null} when no endpoint matched (unknown path, CORS preflight).
 *                        Unlike {@code path}, its set of values is bounded, which makes it the key to
 *                        aggregate on
 * @param status          the response status when the router finished. A client that disconnects
 *                        while the response is sent, or a failure Jetty detects once the router has
 *                        returned, is not reflected here
 * @param startedAtMillis when the router received the request, in epoch milliseconds
 * @param durationNanos   the time spent from receiving the request to producing its response
 * @param request         the request itself, for anything else a listener needs (headers, client
 *                        address through {@code HttpUtil.getClientIpAddress}, attributes). Only valid
 *                        for the duration of {@link RequestListener#onRequestCompleted}: the
 *                        container recycles it once the request completes
 */
public record CompletedRequest(
        String requestId,
        String method,
        String path,
        String route,
        int status,
        long startedAtMillis,
        long durationNanos,
        HttpServletRequest request
) {

    public Duration duration() {
        return Duration.ofNanos(durationNanos);
    }

    /** Whether an endpoint handled the request, i.e. {@link #route()} is known. */
    public boolean matched() {
        return route != null;
    }

    /**
     * Leaves the request out: the container renders it with its full URI, query string included,
     * and a query string can carry credentials (reset links, tokens) that {@link #path()} omits.
     */
    @Override
    public String toString() {
        return "CompletedRequest[requestId=" + requestId
                + ", method=" + method
                + ", path=" + path
                + ", route=" + route
                + ", status=" + status
                + ", startedAtMillis=" + startedAtMillis
                + ", durationNanos=" + durationNanos + "]";
    }
}
