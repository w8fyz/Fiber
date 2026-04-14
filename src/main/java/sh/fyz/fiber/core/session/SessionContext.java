package sh.fyz.fiber.core.session;

public class SessionContext {

    private static final ThreadLocal<FiberSession> CURRENT_SESSION = new ThreadLocal<>();

    public static void set(FiberSession session) {
        CURRENT_SESSION.set(session);
    }

    public static FiberSession current() {
        return CURRENT_SESSION.get();
    }

    public static void clear() {
        CURRENT_SESSION.remove();
    }
}
