package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.regex.Matcher;

/**
 * Handler pour les paramètres de type HttpServletRequest et HttpServletResponse.
 */
public class ServletParameterHandler implements ParameterHandler {
    @Override
    public boolean canHandle(Parameter parameter) {
        Class<?> type = parameter.getType();
        return type == HttpServletRequest.class || type == HttpServletResponse.class;
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) {
        Class<?> type = parameter.getType();
        if (type == HttpServletRequest.class) {
            return request;
        } else if (type == HttpServletResponse.class) {
            return response;
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + type);
    }
} 