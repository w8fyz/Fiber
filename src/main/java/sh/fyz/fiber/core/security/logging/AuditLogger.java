package sh.fyz.fiber.core.security.logging;

import sh.fyz.fiber.core.security.annotations.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

    public static void logAuditEvent(AuditLog auditLog, Method method, Object[] args, Object result) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("AUDIT: ").append(auditLog.action());

        if (auditLog.logParameters()) {
            String parameters = Arrays.stream(args)
                .map(arg -> auditLog.maskSensitiveData() ? maskSensitiveData(arg) : arg)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
            logMessage.append(" - Parameters: [").append(parameters).append("]");
        }

        if (auditLog.logResult() && result != null) {
            String resultStr = auditLog.maskSensitiveData() ? maskSensitiveData(result) : result.toString();
            logMessage.append(" - Result: ").append(resultStr);
        }

        logger.info(logMessage.toString());
    }

    private static String maskSensitiveData(Object data) {
        if (data == null) return null;
        
        String str = data.toString();
        if (str.length() <= 4) return "****";
        
        // Mask sensitive data like passwords, tokens, etc.
        if (str.toLowerCase().contains("password") || 
            str.toLowerCase().contains("token") || 
            str.toLowerCase().contains("apikey") ||
            str.toLowerCase().contains("secret")) {
            return "****";
        }
        
        // For other data, show first and last 2 characters
        return str.substring(0, 2) + "****" + str.substring(str.length() - 2);
    }
} 