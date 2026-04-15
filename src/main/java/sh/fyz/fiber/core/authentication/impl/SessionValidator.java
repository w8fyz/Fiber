package sh.fyz.fiber.core.authentication.impl;

import io.jsonwebtoken.Claims;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.session.FiberSession;
import sh.fyz.fiber.core.session.SessionContext;
import sh.fyz.fiber.core.session.SessionService;

public final class SessionValidator {

    private SessionValidator() {}

    public static boolean validate(Claims claims) {
        String sessionId = claims.get("sessionId", String.class);
        if (sessionId == null) {
            return true;
        }

        SessionService sessionService = FiberServer.get().getSessionService();
        if (sessionService == null) {
            return true;
        }

        FiberSession session = sessionService.getSession(sessionId);
        if (session == null) {
            return false;
        }

        SessionContext.set(session);
        return true;
    }
}
