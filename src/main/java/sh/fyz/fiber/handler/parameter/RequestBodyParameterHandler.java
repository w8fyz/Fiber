package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.params.RequestBody;
import sh.fyz.fiber.util.JsonUtil;
import sh.fyz.fiber.validation.ValidationRegistry;
import sh.fyz.fiber.validation.ValidationResult;

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
        try {
            Object deserializedObject = JsonUtil.fromJson(body, parameter.getType());
            
            // Validate the deserialized object
            ValidationResult validationResult = ValidationRegistry.validate(deserializedObject);
            if (!validationResult.isValid()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Validation failed: " + String.join(", ", validationResult.getErrors()));
                return null;
            }
            
            return deserializedObject;
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON Request Body");
            return null;
        }
    }
} 