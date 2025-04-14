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
import java.util.stream.IntStream;

public class AuditLogProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogProcessor.class);

    @SuppressWarnings("unchecked")
    public static void logAuditEvent(HttpServletRequest req, HttpServletResponse resp, AuditLog auditLog, Method method, Object[] args, Object result) {
        Map<String, Object> logData = new LinkedHashMap<>();
        
        // Basic request info
        logData.put("timestamp", new Date().getTime());
        logData.put("ip", req.getRemoteAddr());
        logData.put("userAgent", req.getHeader("User-Agent"));
        logData.put("method", req.getMethod());
        logData.put("uri", req.getRequestURI());
        logData.put("action", auditLog.action());
        logData.put("status", resp.getStatus());

        // Query Parameters
        Map<String, String> queryParams = new HashMap<>();
        req.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                queryParams.put(key, values[0]);
            }
        });
        if (!queryParams.isEmpty()) {
            logData.put("queryParams", queryParams);
        }

        // Path Variables
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

        // Request Body (for POST/PUT methods)
        if (req.getMethod().equals("POST") || req.getMethod().equals("PUT")) {
            try {
                BufferedReader reader = req.getReader();
                String requestBody = reader.lines().collect(Collectors.joining());
                if (!requestBody.isEmpty()) {
                    // Try to parse as JSON for better formatting
                    try {
                        Object jsonBody = JsonUtil.fromJson(requestBody, Object.class);
                        logData.put("requestBody", jsonBody);
                    } catch (Exception e) {
                        // If not valid JSON, store as string
                        logData.put("requestBody", requestBody);
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not read request body", e);
            }
        }

        // Method Parameters
        if (auditLog.logParameters() && args != null && args.length > 0) {
            Parameter[] parameters = method.getParameters();
            Map<String, Object> methodParams = new HashMap<>();
            
            for (int i = 0; i < parameters.length && i < args.length; i++) {
                String paramName = parameters[i].getName();
                Object paramValue = args[i];
                
                if (paramValue != null) {
                    // Convert the parameter value to a serializable format
                    try {
                        // Try to convert to JSON and back to ensure it's serializable
                        String jsonValue = JsonUtil.toJson(paramValue);
                        Object serializedValue = JsonUtil.fromJson(jsonValue, Object.class);
                        methodParams.put(paramName, serializedValue);
                    } catch (Exception e) {
                        // If serialization fails, use toString()
                        methodParams.put(paramName, paramValue.toString());
                    }
                }
            }
            
            if (!methodParams.isEmpty()) {
                logData.put("parameters", methodParams);
            }
        }

        // Response
        if (auditLog.logResult() && result != null) {
            try {
                // Try to convert to JSON and back to ensure it's serializable
                String jsonResult = JsonUtil.toJson(result);
                Object serializedResult = JsonUtil.fromJson(jsonResult, Object.class);
                logData.put("response", serializedResult);
            } catch (Exception e) {
                // If serialization fails, use toString()
                logData.put("response", result.toString());
            }
        }

        // Log the final JSON
        try {
            String logMessage = JsonUtil.toJson(logData);
            logger.info(logMessage);

            // Send to AuditLogService if configured
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
                    logMessage
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to serialize log data", e);
        }
    }

} 