package sh.fyz.fiber.core.security.logging;

import java.lang.reflect.Method;

public class AuditLog {

    private final String action;
    private final Method sourceMethod;
    private final Object[] parameters;
    private final Object result;
    private final String formated;

    public AuditLog(String action, Method sourceMethod, Object[] parameters, Object result, String formated) {
        this.action = action;
        this.sourceMethod = sourceMethod;
        this.parameters = parameters;
        this.result = result;
        this.formated = formated;
    }

    public Method getSourceMethod() {
        return sourceMethod;
    }

    public String getAction() {
        return action;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public Object getResult() {
        return result;
    }

    public String getFormated() {
        return formated;
    }
}
