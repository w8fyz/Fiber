package sh.fyz.fiber.core.security.logging;

import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.auth.PasswordField;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AuditLogProcessor {

    public static void logAuditEvent(AuditLog auditLog, Method method, Object[] args, Object result) {

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("LOG: ").append(auditLog.action());

        if (auditLog.logParameters()) {
            String parameters = Arrays.stream(args)
                .map(arg -> auditLog.maskSensitiveData() ? maskSensitiveData(arg) : arg)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
            logMessage.append(" - Parameters: [").append(parameters).append("]");
        }

        if (auditLog.logResult() && result != null) {
            String resultStr = auditLog.maskSensitiveData() ? maskSensitiveData(result).toString() : result.toString();
            logMessage.append(" - Result: ").append(resultStr);
        }

        AuditLogService service = FiberServer.get().getAuditLogService();
        if (service != null) {
            service.onAuditLog(new sh.fyz.fiber.core.security.logging.AuditLog(
                auditLog.action(),
                method,
                args,
                result,
                logMessage.toString()
            ));
        }
    }

    private static Object maskSensitiveData(Object data) {
        if (data == null) return null;
        Class<?> dataClass = data.getClass();
        Map<String, Object> dataMap = new HashMap<>();
        Field[] fields = dataClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(PasswordField.class)) {
                field.setAccessible(true);
                try {
                    Object value = field.get(data);
                    if (value != null) {
                        dataMap.put(field.getName(), "****");
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            dataMap.put(field.getName(), field.getType().isArray() ? Arrays.toString((Object[]) data) : data);
        }
        return dataMap;
    }
} 