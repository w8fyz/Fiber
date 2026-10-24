package sh.fyz.fiber.core.request;

/** Request attributes the router sets on every request it handles. */
public final class RequestAttributes {

    /** The {@link System#nanoTime()} at which the router received the request ({@link Long}). */
    public static final String START_NANOS = "fiber.requestStartNanos";

    /**
     * The path template of the endpoint that matched the request, e.g. {@code /users/{id}}
     * ({@link String}). Absent when no endpoint matched.
     */
    public static final String ROUTE = "fiber.route";

    private RequestAttributes() {
    }
}
