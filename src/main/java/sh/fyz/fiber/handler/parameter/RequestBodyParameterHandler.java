package sh.fyz.fiber.handler.parameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.params.RequestBody;
import sh.fyz.fiber.util.JsonUtil;
import sh.fyz.fiber.validation.ValidationRegistry;
import sh.fyz.fiber.validation.ValidationResult;

import sh.fyz.fiber.core.security.logging.AuditLogProcessor;

import java.lang.reflect.Parameter;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class RequestBodyParameterHandler implements ParameterHandler {
    @Override
    public boolean canHandle(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) throws Exception {
        String body = request.getReader().lines().collect(Collectors.joining());
        request.setAttribute(AuditLogProcessor.RAW_BODY_ATTRIBUTE, body);
        try {
            Object deserializedObject = JsonUtil.fromJson(body, parameter.getType());
            
            ValidationResult validationResult = ValidationRegistry.validate(deserializedObject);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException("Validation failed: " + String.join(", ", validationResult.getErrors()));
            }
            
            return deserializedObject;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON Request Body");
        }
    }
}
