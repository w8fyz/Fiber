package sh.fyz.fiber.core.security.logging;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.request.RequestAttributes;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.core.upload.UploadedFile;
import sh.fyz.fiber.util.JsonUtil;
import sh.fyz.fiber.core.log.FiberLogger;
import sh.fyz.fiber.core.log.FiberLog;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronously serialises audit events to SLF4J and {@link AuditLogService}.
 *
 * <p>Hot paths cache per-method reflection (parameters) and reuse the Fiber
 * shared executor when available so that a single audit-heavy endpoint does
 * not spawn thousands of ad-hoc threads.</p>
 */
public class AuditLogProcessor {
    private static final FiberLogger logger = FiberLog.get(AuditLogProcessor.class);

    public static final String RAW_BODY_ATTRIBUTE = "fiber.rawBody";

    // Per-method cache to avoid repeated reflection.
    private static final Map<Method, Parameter[]> parameterCache = new ConcurrentHashMap<>();

    private static final ExecutorService fallbackExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private static ExecutorService resolveExecutor() {
        try {
            FiberServer server = FiberServer.get();
            if (server != null) {
                return server.getAuditExecutor();
            }
        } catch (Exception e) {
            FiberLog.handleSilent(e);
        }
        return fallbackExecutor;
    }

    /**
     * Records no path variables: only the endpoint knows the pattern the request was routed by.
     *
     * @deprecated use the overload taking the request's path variables
     */
    @Deprecated
    public static void logAuditEvent(HttpServletRequest req, HttpServletResponse resp, AuditLog auditLog, Method method, Object[] args, Object result) {
        logAuditEvent(req, resp, auditLog, method, args, result, Map.of());
    }

    /**
     * @param pathVariables the values of the route's {@code {name}} segments, as the endpoint matched
     *                      them; recorded under {@code pathVariables} when not empty
     */
    @SuppressWarnings("unchecked")
    public static void logAuditEvent(HttpServletRequest req, HttpServletResponse resp, AuditLog auditLog, Method method,
                                     Object[] args, Object result, Map<String, String> pathVariables) {
        // Copied in order: the record lists the variables as the path does.
        Map<String, String> recordedPathVariables = pathVariables == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(pathVariables);
        long timestamp = System.currentTimeMillis();
        long durationMs = elapsedMillis(req);
        String ip = req.getRemoteAddr();
        String userAgent = req.getHeader("User-Agent");
        String httpMethod = req.getMethod();
        String uri = req.getRequestURI();
        int status = resp.getStatus();
        Map<String, String[]> parameterMap = new HashMap<>(req.getParameterMap());
        String rawBody = (String) req.getAttribute(RAW_BODY_ATTRIBUTE);
        Map<String, Object> customData = AuditContext.getAll();
        // The request/response objects are recycled once the request completes and are
        // not serialisable anyway: never hand them to the asynchronous processing below.
        // Uploads are summarised: serialising them would open their temp file (getInputStream).
        Object[] loggedArgs = args == null ? null : args.clone();
        if (loggedArgs != null) {
            for (int i = 0; i < loggedArgs.length; i++) {
                if (loggedArgs[i] instanceof ServletRequest || loggedArgs[i] instanceof ServletResponse) {
                    loggedArgs[i] = null;
                } else if (loggedArgs[i] instanceof UploadedFile file) {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("filename", file.getOriginalFilename());
                    summary.put("contentType", file.getContentType());
                    summary.put("size", file.getSize());
                    loggedArgs[i] = summary;
                }
            }
        }

        resolveExecutor().submit(() -> {
            try {
                processAuditLog(timestamp, durationMs, ip, userAgent, httpMethod, uri, status, parameterMap,
                        recordedPathVariables, rawBody, customData, auditLog, method, loggedArgs, result);
            } catch (Exception e) {
                logger.error("Failed to process audit log", e);
            }
        });
    }

    /** Time since the router received the request, or {@code -1} when the request did not go through it. */
    private static long elapsedMillis(HttpServletRequest req) {
        Object startNanos = req.getAttribute(RequestAttributes.START_NANOS);
        if (startNanos instanceof Long start) {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        }
        return -1L;
    }

    @SuppressWarnings("unchecked")
    private static void processAuditLog(long timestamp, long durationMs, String ip, String userAgent, String httpMethod,
                                         String uri, int status, Map<String, String[]> parameterMap,
                                         Map<String, String> pathVariables,
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
        if (durationMs >= 0) {
            logData.put("durationMs", durationMs);
        }

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

        if (!pathVariables.isEmpty()) {
            logData.put("pathVariables", pathVariables);
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
                    durationMs,
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

    /** Visible for testing — clears the per-method reflection cache. */
    public static void clearCaches() {
        parameterCache.clear();
    }
}
