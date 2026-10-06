package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;
import sh.fyz.fiber.core.log.FiberLog;
import sh.fyz.fiber.handler.parameter.ParameterHandler;
import sh.fyz.fiber.handler.parameter.ParameterHandlerRegistry;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.regex.Matcher;

public class ParameterResolver {

    public static Object[] resolve(Method method, HttpServletRequest req, HttpServletResponse resp,
                                   Matcher pathMatcher, UserAuth authenticatedUser) throws ResolveException {
        Parameter[] parameters = method.getParameters();
        return resolve(parameters, findHandlers(parameters), req, resp, pathMatcher, authenticatedUser, null);
    }

    /**
     * Resolve the arguments with handlers looked up once per endpoint (see {@link #findHandlers}).
     * The user and the OAuth2 application already authenticated by the security pipeline are
     * injected directly instead of being authenticated a second time.
     */
    static Object[] resolve(Parameter[] parameters, ParameterHandler[] handlers,
                            HttpServletRequest req, HttpServletResponse resp, Matcher pathMatcher,
                            UserAuth authenticatedUser, OAuth2ApplicationInfo authenticatedApp) throws ResolveException {
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(AuthenticatedUser.class)) {
                args[i] = authenticatedUser;
                continue;
            }
            if (authenticatedApp != null && parameter.getType() == OAuth2ApplicationInfo.class) {
                args[i] = authenticatedApp;
                continue;
            }

            ParameterHandler handler = handlers[i];
            if (handler == null) {
                throw new ResolveException("No handler found for parameter: " + parameter.getName(),
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            try {
                args[i] = handler.handle(parameter, req, resp, pathMatcher);
            } catch (ResolveException e) {
                // A handler that already chose its status (e.g. 413).
                throw e;
            } catch (IllegalArgumentException e) {
                throw new ResolveException(e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
            } catch (Exception e) {
                FiberLog.handle(e, "param resolution failed for {}", parameter.getName());
                throw new ResolveException("Internal server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return args;
    }

    /** @return the handler of each parameter, {@code null} where none applies. */
    static ParameterHandler[] findHandlers(Parameter[] parameters) {
        ParameterHandler[] handlers = new ParameterHandler[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            handlers[i] = ParameterHandlerRegistry.findHandler(parameters[i]);
        }
        return handlers;
    }

    public static class ResolveException extends Exception {
        private final int statusCode;

        public ResolveException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
