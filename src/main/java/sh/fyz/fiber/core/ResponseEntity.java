package sh.fyz.fiber.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResponseEntity<T> {
    private final T body;
    private final Map<String, String> headers;
    private int status;
    private String contentType;

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

    public static <T> ResponseEntity<T> notFound() {
        return new ResponseEntity<>(null, 404);
    }
    public static <T> ResponseEntity<T> noContent() {
        return new ResponseEntity<>(null, 204);
    }

    public static <T> ResponseEntity<T> serverError(T body) {
        return new ResponseEntity<>(body, 500);
    }

    public static <T> ResponseEntity<T> unauthorized(T reason) {
        return new ResponseEntity<>(reason, 401);
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

    public void write(HttpServletResponse response) throws IOException {
        response.setStatus(status);
        response.setContentType(contentType);
        
        // Set headers
        headers.forEach(response::setHeader);

        // Write body if present
        if (body != null) {
            if (contentType.equals("application/json")) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(response.getWriter(), body);
            } else if (body instanceof byte[]) {
                response.getOutputStream().write((byte[]) body);
            } else {
                response.getWriter().write(body.toString());
            }
        }
    }
} 