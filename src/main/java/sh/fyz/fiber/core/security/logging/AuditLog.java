package sh.fyz.fiber.core.security.logging;

import java.lang.reflect.Method;
import java.util.Map;

public class AuditLog {
    private final long timestamp;
    private final String ip;
    private final String userAgent;
    private final String method;
    private final String uri;
    private final String action;
    private final int status;
    private final Map<String, String> queryParams;
    private final Map<String, String> pathVariables;
    private final Object requestBody;
    private final Map<String, Object> parameters;
    private final Object response;
    private final String rawLog;

    public AuditLog(
            long timestamp,
            String ip,
            String userAgent,
            String method,
            String uri,
            String action,
            int status,
            Map<String, String> queryParams,
            Map<String, String> pathVariables,
            Object requestBody,
            Map<String, Object> parameters,
            Object response,
            String rawLog
    ) {
        this.timestamp = timestamp;
        this.ip = ip;
        this.userAgent = userAgent;
        this.method = method;
        this.uri = uri;
        this.action = action;
        this.status = status;
        this.queryParams = queryParams;
        this.pathVariables = pathVariables;
        this.requestBody = requestBody;
        this.parameters = parameters;
        this.response = response;
        this.rawLog = rawLog;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getAction() {
        return action;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getPathVariables() {
        return pathVariables;
    }

    public Object getRequestBody() {
        return requestBody;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Object getResponse() {
        return response;
    }

    public String getRawLog() {
        return rawLog;
    }

    @Override
    public String toString() {
        return rawLog;
    }
}
