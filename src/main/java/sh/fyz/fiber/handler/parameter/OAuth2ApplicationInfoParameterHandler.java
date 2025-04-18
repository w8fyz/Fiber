package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;
import sh.fyz.fiber.core.authentication.impl.BasicAuthenticator;
import sh.fyz.fiber.FiberServer;

import java.lang.reflect.Parameter;
import java.util.regex.Matcher;

public class OAuth2ApplicationInfoParameterHandler implements ParameterHandler {
    @Override
    public boolean canHandle(Parameter parameter) {
        return parameter.getType().equals(OAuth2ApplicationInfo.class);
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) {
        BasicAuthenticator authenticator = FiberServer.get().getBasicAuthenticator();
        if (authenticator != null) {
            return authenticator.authenticate(request);
        }
        return null;
    }
} 