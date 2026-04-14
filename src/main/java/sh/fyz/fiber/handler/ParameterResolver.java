package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.handler.parameter.ParameterHandler;
import sh.fyz.fiber.handler.parameter.ParameterHandlerRegistry;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.regex.Matcher;

public class ParameterResolver {

    public static Object[] resolve(Method method, HttpServletRequest req, HttpServletResponse resp,
                                   Matcher pathMatcher, UserAuth authenticatedUser) throws ResolveException {
        Object[] args = new Object[method.getParameterCount()];
        String path = req.getRequestURI();

        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = method.getParameters()[i];
            if (parameter.isAnnotationPresent(AuthenticatedUser.class)) {
                args[i] = authenticatedUser;
                continue;
            }

            ParameterHandler handler = ParameterHandlerRegistry.findHandler(parameter);
            if (handler == null) {
                throw new ResolveException("No handler found for parameter: " + parameter.getName(),
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            try {
                args[i] = handler.handle(parameter, req, resp, pathMatcher);
            } catch (IllegalArgumentException e) {
                throw new ResolveException(e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
            } catch (Exception e) {
                throw new ResolveException("Internal server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
        return args;
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
