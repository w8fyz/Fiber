package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.core.authentication.AuthMiddleware;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.lang.reflect.Parameter;
import java.util.regex.Matcher;

/**
 * Handler pour les paramètres annotés avec @AuthenticatedUser.
 */
public class AuthenticatedUserParameterHandler implements ParameterHandler {
    @Override
    public boolean canHandle(Parameter parameter) {
        return parameter.isAnnotationPresent(AuthenticatedUser.class);
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) {
        Class<?> type = parameter.getType();
        
        if (UserAuth.class.isAssignableFrom(type)) {
            return AuthMiddleware.getCurrentUser(request);
        } else if (FiberServer.get().getAuthService().getUserClass().isAssignableFrom(type)) {
            return FiberServer.get().getAuthService().getUserById(AuthMiddleware.getCurrentUserId(request));
        } else {
            throw new IllegalArgumentException("Invalid type for @AuthenticatedUser: " + type);
        }
    }
} 