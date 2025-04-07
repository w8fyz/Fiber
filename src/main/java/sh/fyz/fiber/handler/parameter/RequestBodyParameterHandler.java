package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.RequestBody;
import sh.fyz.fiber.util.JsonUtil;

import java.lang.reflect.Parameter;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Handler pour les paramètres annotés avec @RequestBody.
 */
public class RequestBodyParameterHandler implements ParameterHandler {
    @Override
    public boolean canHandle(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) throws Exception {
        String body = request.getReader().lines().collect(Collectors.joining());
        return JsonUtil.fromJson(body, parameter.getType());
    }
} 