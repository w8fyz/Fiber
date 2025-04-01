package sh.fyz.fiber.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ErrorResponse {
    private final String uri;
    private final int status;
    private final String message;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ErrorResponse(String uri, int status, String message) {
        this.uri = uri;
        this.status = status;
        this.message = message;
    }

    public String getUri() {
        return uri;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public static void send(HttpServletResponse response, String uri, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        ErrorResponse error = new ErrorResponse(uri, status, message);
        objectMapper.writeValue(response.getWriter(), error);
    }
} 