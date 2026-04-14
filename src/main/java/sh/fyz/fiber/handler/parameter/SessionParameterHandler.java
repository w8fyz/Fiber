package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.params.CurrentSession;
import sh.fyz.fiber.core.session.FiberSession;
import sh.fyz.fiber.core.session.SessionContext;

import java.lang.reflect.Parameter;
import java.util.regex.Matcher;

public class SessionParameterHandler implements ParameterHandler {

    @Override
    public boolean canHandle(Parameter parameter) {
        return parameter.isAnnotationPresent(CurrentSession.class)
                || parameter.getType() == FiberSession.class;
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) {
        FiberSession session = SessionContext.current();
        if (session == null && parameter.isAnnotationPresent(CurrentSession.class)) {
            throw new IllegalArgumentException("No active session available. Make sure SessionService is configured and the user is authenticated.");
        }
        return session;
    }
}
