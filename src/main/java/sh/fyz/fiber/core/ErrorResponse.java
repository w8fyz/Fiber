package sh.fyz.fiber.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.util.FiberObjectMapper;

import java.io.IOException;

public class ErrorResponse {
    private final String uri;
    private final int status;
    private final String message;
    private static final FiberObjectMapper objectMapper = new FiberObjectMapper();

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

    public static void send(HttpServletResponse response, String uri, int status, String message) {
        response.setStatus(status);
        response.setContentType("application/json");
        ErrorResponse error = new ErrorResponse(uri, status, message);
        try {
            objectMapper.writeValue(response.getWriter(), error);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
} 