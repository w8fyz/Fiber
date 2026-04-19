package sh.fyz.fiber.core.security.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asynchronously serialises audit events to SLF4J and {@link AuditLogService}.
 *
 * <p>Hot paths cache per-method reflection (controller path + compiled regex)
 * and reuse the Fiber shared executor when available so that a single
 * audit-heavy endpoint does not spawn thousands of ad-hoc threads.</p>
 */
public class AuditLogProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogProcessor.class);

    public static final String RAW_BODY_ATTRIBUTE = "fiber.rawBody";

    private static final Pattern PATH_VAR_PATTERN = Pattern.compile("\\{([^}]+)}");

    // Per-method caches to avoid repeated annotation lookup / regex compilation.
    private static final Map<Method, MethodPathInfo> methodPathCache = new ConcurrentHashMap<>();
    private static final Map<Method, Parameter[]> parameterCache = new ConcurrentHashMap<>();

    private static final ExecutorService fallbackExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private record MethodPathInfo(List<String> variableNames, Pattern pathPattern) {
        static final MethodPathInfo NONE = new MethodPathInfo(List.of(), null);
    }

    private static ExecutorService resolveExecutor() {
        try {
            FiberServer server = FiberServer.get();
            if (server != null && server.getSharedExecutor() != null) {
                return server.getSharedExecutor();
            }
        } catch (Exception ignored) {
        }
        return fallbackExecutor;
    }

    @SuppressWarnings("unchecked")
    public static void logAuditEvent(HttpServletRequest req, HttpServletResponse resp, AuditLog auditLog, Method method, Object[] args, Object result) {
        long timestamp = System.currentTimeMillis();
        String ip = req.getRemoteAddr();
        String userAgent = req.getHeader("User-Agent");
        String httpMethod = req.getMethod();
        String uri = req.getRequestURI();
        int status = resp.getStatus();
        Map<String, String[]> parameterMap = new HashMap<>(req.getParameterMap());
        String rawBody = (String) req.getAttribute(RAW_BODY_ATTRIBUTE);
        Map<String, Object> customData = AuditContext.getAll();

        resolveExecutor().submit(() -> {
            try {
                processAuditLog(timestamp, ip, userAgent, httpMethod, uri, status, parameterMap,
                        rawBody, customData, auditLog, method, args, result);
            } catch (Exception e) {
                logger.error("Failed to process audit log", e);
            }
        });
    }

    private static MethodPathInfo pathInfoFor(Method method) {
        return methodPathCache.computeIfAbsent(method, m -> {
            if (m.getDeclaringClass().getAnnotation(Controller.class) == null) {
                return MethodPathInfo.NONE;
            }
            RequestMapping rm = m.getAnnotation(RequestMapping.class);
            if (rm == null || rm.value() == null || rm.value().isBlank()) {
                return MethodPathInfo.NONE;
            }
            String path = rm.value();
            Matcher matcher = PATH_VAR_PATTERN.matcher(path);
            List<String> vars = new ArrayList<>();
            while (matcher.find()) {
                vars.add(matcher.group(1));
            }
            if (vars.isEmpty()) {
                return MethodPathInfo.NONE;
            }
            String regex = PATH_VAR_PATTERN.matcher(path).replaceAll("([^/]+)");
            return new MethodPathInfo(List.copyOf(vars), Pattern.compile(regex));
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

        if (!parameterMap.isEmpty()) {
            Map<String, String> queryParams = new HashMap<>(parameterMap.size());
            parameterMap.forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    queryParams.put(key, values[0]);
                }
            });
            if (!queryParams.isEmpty()) {
                logData.put("queryParams", queryParams);
            }
        }

        MethodPathInfo info = pathInfoFor(method);
        if (info.pathPattern() != null) {
            try {
                Matcher m = info.pathPattern().matcher(uri);
                if (m.find()) {
                    Map<String, String> pathVars = new HashMap<>();
                    for (int i = 0; i < info.variableNames().size() && i < m.groupCount(); i++) {
                        pathVars.put(info.variableNames().get(i), m.group(i + 1));
                    }
                    if (!pathVars.isEmpty()) {
                        logData.put("pathVariables", pathVars);
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not extract path variables", e);
            }
        }

        if (("POST".equals(httpMethod) || "PUT".equals(httpMethod) || "PATCH".equals(httpMethod))
                && rawBody != null && !rawBody.isEmpty()) {
            try {
                Object jsonBody = JsonUtil.fromJson(rawBody, Object.class);
                if (auditLog.maskSensitiveData() && jsonBody instanceof Map) {
                    maskMapFieldsRecursive((Map<String, Object>) jsonBody);
                }
                logData.put("requestBody", jsonBody);
            } catch (Exception e) {
                // Non-JSON body: store a truncated preview so we don't dump megabytes.
                logData.put("requestBody", rawBody.length() > 4096 ? rawBody.substring(0, 4096) + "…" : rawBody);
            }
        }

        if (auditLog.logParameters() && args != null && args.length > 0) {
            Parameter[] parameters = parameterCache.computeIfAbsent(method, Method::getParameters);
            Map<String, Object> methodParams = new HashMap<>();

            for (int i = 0; i < parameters.length && i < args.length; i++) {
                Object paramValue = args[i];
                if (paramValue == null) continue;

                String paramName = parameters[i].getName();
                try {
                    if (auditLog.maskSensitiveData()) {
                        // Only round-trip through JSON when masking is required.
                        String jsonValue = JsonUtil.toJson(paramValue);
                        Object clonedValue = JsonUtil.fromJson(jsonValue, Object.class);
                        if (clonedValue instanceof Map) {
                            maskMapFieldsRecursive((Map<String, Object>) clonedValue);
                        }
                        methodParams.put(paramName, clonedValue);
                    } else {
                        methodParams.put(paramName, paramValue);
                    }
                } catch (Exception e) {
                    methodParams.put(paramName, paramValue.toString());
                }
            }

            if (!methodParams.isEmpty()) {
                logData.put("parameters", methodParams);
            }
        }

        if (auditLog.logResult() && result != null) {
            try {
                if (auditLog.maskSensitiveData()) {
                    String jsonResult = JsonUtil.toJson(result);
                    Object serializedResult = JsonUtil.fromJson(jsonResult, Object.class);
                    if (serializedResult instanceof Map) {
                        maskMapFieldsRecursive((Map<String, Object>) serializedResult);
                    }
                    logData.put("response", serializedResult);
                } else {
                    logData.put("response", result);
                }
            } catch (Exception e) {
                logData.put("response", result.toString());
            }
        }

        if (customData != null && !customData.isEmpty()) {
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
            String lower = entry.getKey().toLowerCase(Locale.ROOT);
            if (lower.contains("password") || lower.contains("secret") || lower.contains("token")
                    || lower.contains("authorization") || lower.contains("api_key") || lower.contains("apikey")) {
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

    /** Visible for testing — clears per-method reflection caches. */
    public static void clearCaches() {
        methodPathCache.clear();
        parameterCache.clear();
    }
}
