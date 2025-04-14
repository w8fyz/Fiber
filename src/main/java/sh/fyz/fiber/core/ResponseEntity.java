package sh.fyz.fiber.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.util.FiberObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResponseEntity<T> {
    private final T body;
    private final Map<String, String> headers;
    private int status;
    private String contentType;
    private String uri;

    private ResponseEntity(T body, int status) {
        this.body = body;
        this.status = status;
        this.headers = new HashMap<>();
        this.contentType = "application/json";
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>(body, 200);
    }

    public static <T> ResponseEntity<T> created(T body) {
        return new ResponseEntity<>(body, 201);
    }

    public static <T> ResponseEntity<T> badRequest(T body) {
        return new ResponseEntity<>(body, 400);
    }

    public static <T> ResponseEntity<T> notFound(T body) {
        return new ResponseEntity<>(body, 404);
    }

    public static <T> ResponseEntity<T> notFound() {
        return notFound(null);
    }
    
    public static <T> ResponseEntity<T> noContent() {
        return new ResponseEntity<>(null, 204);
    }

    public static <T> ResponseEntity<T> serverError(T body) {
        return new ResponseEntity<>(body, 500);
    }

    public static <T> ResponseEntity<T> unauthorized(T body) {
        return new ResponseEntity<>(body, 401);
    }

    public static <T> ResponseEntity<T> gone(T body) {
        return new ResponseEntity<>(body, 410);
    }
    public ResponseEntity<T> status(int status) {
        this.status = status;
        return this;
    }

    public ResponseEntity<T> header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public ResponseEntity<T> contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public ResponseEntity<T> uri(String uri) {
        this.uri = uri;
        return this;
    }

    public void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(status);
        response.setContentType(contentType);
        
        // Set headers
        headers.forEach(response::setHeader);

        // Write body if present
        if (body != null) {
            FiberObjectMapper mapper = new FiberObjectMapper();
            try {
                if (body instanceof String) {
                    // Use standardized format for String responses
                    Map<String, Object> responseBody = new HashMap<>();
                    responseBody.put("uri", uri != null ? uri : request.getRequestURI());
                    responseBody.put("status", status);
                    responseBody.put("message", body);
                    mapper.writeValue(response.getWriter(), responseBody);
                } else if (body instanceof byte[]) {
                    response.getOutputStream().write((byte[]) body);
                } else {
                    // For non-String objects, write directly
                    mapper.writeValue(response.getWriter(), body);
                }
            } catch (Exception e) {
                // Handle non-serializable objects
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("uri", uri != null ? uri : request.getRequestURI());
                errorResponse.put("status", 500);
                errorResponse.put("message", "Internal Server Error: Response body contains non-serializable content");
                response.setStatus(500);
                mapper.writeValue(response.getWriter(), errorResponse);
            }
        }
    }
} 