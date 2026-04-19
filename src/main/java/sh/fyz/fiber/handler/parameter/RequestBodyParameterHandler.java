package sh.fyz.fiber.handler.parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.fyz.fiber.annotations.params.RequestBody;
import sh.fyz.fiber.core.security.logging.AuditLogProcessor;
import sh.fyz.fiber.util.JsonUtil;
import sh.fyz.fiber.validation.ValidationRegistry;
import sh.fyz.fiber.validation.ValidationResult;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class RequestBodyParameterHandler implements ParameterHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestBodyParameterHandler.class);

    @Override
    public boolean canHandle(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) throws Exception {
        String body;
        try {
            body = request.getReader().lines().collect(Collectors.joining());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read request body", e);
        }
        request.setAttribute(AuditLogProcessor.RAW_BODY_ATTRIBUTE, body);

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Request body is empty");
        }

        Object deserializedObject;
        try {
            deserializedObject = JsonUtil.fromJson(body, parameter.getType());
        } catch (JsonProcessingException e) {
            logger.debug("Invalid JSON body for parameter {}: {}", parameter.getName(), e.getOriginalMessage());
            throw new IllegalArgumentException("Invalid JSON request body: " + e.getOriginalMessage());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not parse request body", e);
        }

        if (deserializedObject == null) {
            throw new IllegalArgumentException("Request body deserialised to null");
        }

        ValidationResult validationResult = ValidationRegistry.validate(deserializedObject);
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException("Validation failed: " + String.join(", ", validationResult.getErrors()));
        }

        return deserializedObject;
    }
}
