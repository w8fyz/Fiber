package sh.fyz.fiber.handler.parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.log.FiberLogger;
import sh.fyz.fiber.core.log.FiberLog;
import sh.fyz.fiber.annotations.params.RequestBody;
import sh.fyz.fiber.core.security.logging.AuditLogProcessor;
import sh.fyz.fiber.handler.ParameterResolver;
import sh.fyz.fiber.util.JsonUtil;
import sh.fyz.fiber.validation.ValidationRegistry;
import sh.fyz.fiber.validation.ValidationResult;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Parameter;
import java.util.regex.Matcher;

public class RequestBodyParameterHandler implements ParameterHandler {

    private static final FiberLogger logger = FiberLog.get(RequestBodyParameterHandler.class);

    @Override
    public boolean canHandle(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object handle(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Matcher pathMatcher) throws Exception {
        // Unlike multipart parts, a JSON body has no size limit of its own: hold it to maxRequestSize.
        long limit = FiberServer.get().getMaxRequestSize();
        if (limit >= 0 && request.getContentLengthLong() > limit) {
            throw tooLarge(limit);
        }
        String body;
        try {
            // Read verbatim: joining lines would drop the line breaks between JSON tokens.
            body = read(request.getReader(), limit);
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

    /** Reads the whole body, failing once it exceeds {@code limit} characters (no limit when negative). */
    private static String read(Reader reader, long limit) throws IOException, ParameterResolver.ResolveException {
        StringBuilder body = new StringBuilder();
        char[] buffer = new char[8192];
        int n;
        while ((n = reader.read(buffer)) != -1) {
            body.append(buffer, 0, n);
            if (limit >= 0 && body.length() > limit) {
                throw tooLarge(limit);
            }
        }
        return body.toString();
    }

    private static ParameterResolver.ResolveException tooLarge(long limit) {
        return new ParameterResolver.ResolveException("Request body exceeds " + limit + " bytes",
                HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    }
}
