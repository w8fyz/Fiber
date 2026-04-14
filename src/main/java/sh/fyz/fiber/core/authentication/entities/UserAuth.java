package sh.fyz.fiber.core.authentication.entities;

import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.session.FiberSession;
import sh.fyz.fiber.core.session.SessionContext;
import sh.fyz.fiber.core.session.SessionService;

import java.util.List;

public interface UserAuth {

    Object getId();

    String getRole();

    default FiberSession getSession() {
        FiberSession session = SessionContext.current();
        if (session == null) {
            throw new IllegalStateException("No active session for this request. Make sure SessionService is configured.");
        }
        return session;
    }

    default List<FiberSession> getSessions() {
        return getSessionService().getUserSessions(getId());
    }

    default void invalidateSession(String sessionId) {
        getSessionService().invalidate(sessionId);
    }

    default void invalidateAllSessions() {
        getSessionService().invalidateAllForUser(getId());
    }

    default void invalidateOtherSessions() {
        FiberSession current = SessionContext.current();
        if (current == null) {
            throw new IllegalStateException("No active session for this request. Cannot determine which sessions to keep.");
        }
        getSessionService().invalidateOtherSessions(getId(), current.getSessionId());
    }

    private SessionService getSessionService() {
        SessionService service = FiberServer.get().getSessionService();
        if (service == null) {
            throw new IllegalStateException("SessionService is not configured. Call FiberServer.setSessionService() to enable session management.");
        }
        return service;
    }
}
