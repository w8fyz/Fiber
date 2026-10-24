package sh.fyz.fiber.core.request;

/**
 * Observes every request that reaches the router once it has been handled: matched endpoints,
 * unmatched paths (404), CORS preflights, rejected requests and requests whose handler failed before
 * the response was committed (500).
 * It is the hook for request metrics and access logs, independent of {@code @AuditLog}.
 *
 * <p>Listeners run synchronously on the request thread, in registration order, after the response
 * has been produced and before the request completes. Keep them cheap (increment a counter, record a
 * timer) and hand anything slow to another thread. A listener that throws, an exception or an error
 * short of a {@link VirtualMachineError}, is logged and skipped: it never alters the response and
 * never prevents the other listeners from running.
 *
 * <p>Requests that Jetty rejects before they reach the router (a malformed URI, oversized headers)
 * are not reported.
 *
 * @see sh.fyz.fiber.FiberServer#addRequestListener(RequestListener)
 */
@FunctionalInterface
public interface RequestListener {

    void onRequestCompleted(CompletedRequest request);
}
