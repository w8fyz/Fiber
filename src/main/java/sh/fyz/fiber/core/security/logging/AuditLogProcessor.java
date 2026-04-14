package sh.fyz.fiber.core.security.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.auth.PasswordField;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuditLogProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogProcessor.class);

    @SuppressWarnings("unchecked")
    public static void logAuditEvent(HttpServletRequest req, HttpServletResponse resp, AuditLog auditLog, Method method, Object[] args, Object result) {
        Map<String, Object> logData = new LinkedHashMap<>();

        logData.put("timestamp", new Date().getTime());
        logData.put("ip", req.getRemoteAddr());
        logData.put("userAgent", req.getHeader("User-Agent"));
        logData.put("method", req.getMethod());
        logData.put("uri", req.getRequestURI());
        logData.put("action", auditLog.action());
        logData.put("status", resp.getStatus());

        Map<String, String> queryParams = new HashMap<>();
        req.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                queryParams.put(key, values[0]);
            }
        });
        if (!queryParams.isEmpty()) {
            logData.put("queryParams", queryParams);
        }

        if (method.getDeclaringClass().getAnnotation(sh.fyz.fiber.annotations.request.Controller.class) != null) {
            try {
                String path = method.getAnnotation(sh.fyz.fiber.annotations.request.RequestMapping.class).value();
                Pattern pattern = Pattern.compile("\\{([^}]+)}");
                Matcher matcher = pattern.matcher(path);
                Map<String, String> pathVars = new HashMap<>();

                while (matcher.find()) {
                    String varName = matcher.group(1);
                    String varPattern = path.replace("{" + varName + "}", "([^/]+)");
                    Pattern valuePattern = Pattern.compile(varPattern);
                    Matcher valueMatcher = valuePattern.matcher(req.getRequestURI());
                    if (valueMatcher.find()) {
                        pathVars.put(varName, valueMatcher.group(1));
                    }
                }

                if (!pathVars.isEmpty()) {
                    logData.put("pathVariables", pathVars);
                }
            } catch (Exception e) {
                logger.warn("Could not extract path variables", e);
            }
        }

        if (req.getMethod().equals("POST") || req.getMethod().equals("PUT")) {
            try {
                BufferedReader reader = req.getReader();
                String requestBody = reader.lines().collect(Collectors.joining());
                if (!requestBody.isEmpty()) {
                    try {
                        Object jsonBody = JsonUtil.fromJson(requestBody, Object.class);
                        logData.put("requestBody", jsonBody);
                    } catch (Exception e) {
                        logData.put("requestBody", requestBody);
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not read request body", e);
            }
        }

        if (auditLog.logParameters() && args != null && args.length > 0) {
            Parameter[] parameters = method.getParameters();
            Map<String, Object> methodParams = new HashMap<>();

            for (int i = 0; i < parameters.length && i < args.length; i++) {
                String paramName = parameters[i].getName();
                Object paramValue = args[i];

                if (paramValue != null) {
                    if (auditLog.maskSensitiveData()) {
                        paramValue = maskSensitiveFields(paramValue);
                    }
                    try {
                        String jsonValue = JsonUtil.toJson(paramValue);
                        Object serializedValue = JsonUtil.fromJson(jsonValue, Object.class);
                        methodParams.put(paramName, serializedValue);
                    } catch (Exception e) {
                        methodParams.put(paramName, paramValue.toString());
                    }
                }
            }

            if (!methodParams.isEmpty()) {
                logData.put("parameters", methodParams);
            }
        }

        if (auditLog.logResult() && result != null) {
            try {
                String jsonResult = JsonUtil.toJson(result);
                Object serializedResult = JsonUtil.fromJson(jsonResult, Object.class);
                logData.put("response", serializedResult);
            } catch (Exception e) {
                logData.put("response", result.toString());
            }
        }

        // Collect custom data from AuditContext (set by the endpoint handler)
        Map<String, Object> customData = AuditContext.getAll();
        if (customData != null) {
            logData.put("customData", customData);
        }

        if (auditLog.maskSensitiveData() && logData.containsKey("requestBody")) {
            Object body = logData.get("requestBody");
            if (body instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyMap = (Map<String, Object>) body;
                maskMapFields(bodyMap);
                logData.put("requestBody", bodyMap);
            }
        }

        try {
            String logMessage = JsonUtil.toJson(logData);
            logger.info(logMessage);

            AuditLogService service = FiberServer.get().getAuditLogService();
            if (service != null) {
                Map<String, String> queryParamsMap = logData.containsKey("queryParams") ?
                    (Map<String, String>) logData.get("queryParams") : null;
                Map<String, String> pathVarsMap = logData.containsKey("pathVariables") ?
                    (Map<String, String>) logData.get("pathVariables") : null;
                Map<String, Object> paramsMap = logData.containsKey("parameters") ?
                    (Map<String, Object>) logData.get("parameters") : null;

                service.onAuditLog(new sh.fyz.fiber.core.security.logging.AuditLog(
                    (Long) logData.get("timestamp"),
                    (String) logData.get("ip"),
                    (String) logData.get("userAgent"),
                    (String) logData.get("method"),
                    (String) logData.get("uri"),
                    (String) logData.get("action"),
                    (Integer) logData.get("status"),
                    queryParamsMap,
                    pathVarsMap,
                    logData.get("requestBody"),
                    paramsMap,
                    logData.get("response"),
                    customData,
                    logMessage
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to serialize log data", e);
        } finally {
            AuditContext.clear();
        }
    }

    private static Object maskSensitiveFields(Object value) {
        if (value == null) return null;
        for (Field field : value.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PasswordField.class)) {
                field.setAccessible(true);
                try {
                    field.set(value, "***MASKED***");
                } catch (IllegalAccessException ignored) {}
            }
        }
        return value;
    }

    private static void maskMapFields(Map<String, Object> map) {
        for (String key : map.keySet()) {
            String lower = key.toLowerCase();
            if (lower.contains("password") || lower.contains("secret") || lower.contains("token")) {
                map.put(key, "***MASKED***");
            }
        }
    }
}
