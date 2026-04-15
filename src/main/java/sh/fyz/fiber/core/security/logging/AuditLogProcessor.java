package sh.fyz.fiber.core.security.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.auth.PasswordField;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuditLogProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogProcessor.class);

    public static final String RAW_BODY_ATTRIBUTE = "fiber.rawBody";

    private static final ExecutorService auditExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @SuppressWarnings("unchecked")
    public static void logAuditEvent(HttpServletRequest req, HttpServletResponse resp, AuditLog auditLog, Method method, Object[] args, Object result) {
        // Capture request data on the request thread
        long timestamp = System.currentTimeMillis();
        String ip = req.getRemoteAddr();
        String userAgent = req.getHeader("User-Agent");
        String httpMethod = req.getMethod();
        String uri = req.getRequestURI();
        int status = resp.getStatus();
        Map<String, String[]> parameterMap = new HashMap<>(req.getParameterMap());
        String rawBody = (String) req.getAttribute(RAW_BODY_ATTRIBUTE);
        Map<String, Object> customData = AuditContext.getAll();

        auditExecutor.submit(() -> {
            try {
                processAuditLog(timestamp, ip, userAgent, httpMethod, uri, status, parameterMap,
                        rawBody, customData, auditLog, method, args, result);
            } catch (Exception e) {
                logger.error("Failed to process audit log", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void processAuditLog(long timestamp, String ip, String userAgent, String httpMethod,
                                         String uri, int status, Map<String, String[]> parameterMap,
                                         String rawBody, Map<String, Object> customData,
                                         AuditLog auditLog, Method method, Object[] args, Object result) {
        Map<String, Object> logData = new LinkedHashMap<>();

        logData.put("timestamp", timestamp);
        logData.put("ip", ip);
        logData.put("userAgent", userAgent);
        logData.put("method", httpMethod);
        logData.put("uri", uri);
        logData.put("action", auditLog.action());
        logData.put("status", status);

        Map<String, String> queryParams = new HashMap<>();
        parameterMap.forEach((key, values) -> {
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
                    Matcher valueMatcher = valuePattern.matcher(uri);
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

        if (("POST".equals(httpMethod) || "PUT".equals(httpMethod)) && rawBody != null && !rawBody.isEmpty()) {
            try {
                Object jsonBody = JsonUtil.fromJson(rawBody, Object.class);
                if (auditLog.maskSensitiveData() && jsonBody instanceof Map) {
                    maskMapFieldsRecursive((Map<String, Object>) jsonBody);
                }
                logData.put("requestBody", jsonBody);
            } catch (Exception e) {
                logData.put("requestBody", rawBody);
            }
        }

        if (auditLog.logParameters() && args != null && args.length > 0) {
            Parameter[] parameters = method.getParameters();
            Map<String, Object> methodParams = new HashMap<>();

            for (int i = 0; i < parameters.length && i < args.length; i++) {
                String paramName = parameters[i].getName();
                Object paramValue = args[i];

                if (paramValue != null) {
                    try {
                        // Clone via JSON round-trip to avoid mutating original objects
                        String jsonValue = JsonUtil.toJson(paramValue);
                        Object clonedValue = JsonUtil.fromJson(jsonValue, Object.class);
                        if (auditLog.maskSensitiveData() && clonedValue instanceof Map) {
                            maskMapFieldsRecursive((Map<String, Object>) clonedValue);
                        }
                        methodParams.put(paramName, clonedValue);
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

        if (customData != null) {
            logData.put("customData", customData);
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
            logger.error("Failed to serialize audit log data", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void maskMapFieldsRecursive(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String lower = entry.getKey().toLowerCase();
            if (lower.contains("password") || lower.contains("secret") || lower.contains("token")) {
                entry.setValue("***MASKED***");
            } else if (entry.getValue() instanceof Map) {
                maskMapFieldsRecursive((Map<String, Object>) entry.getValue());
            } else if (entry.getValue() instanceof List) {
                for (Object item : (List<?>) entry.getValue()) {
                    if (item instanceof Map) {
                        maskMapFieldsRecursive((Map<String, Object>) item);
                    }
                }
            }
        }
    }
}
