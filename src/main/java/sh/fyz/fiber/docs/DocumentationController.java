package sh.fyz.fiber.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.Controller;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.ErrorResponse;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller("/docs")
public class DocumentationController {
    private final ObjectMapper objectMapper;
    private final List<EndpointDoc> endpoints;
    private final Map<String, String> mimeTypes;

    public DocumentationController() {
        this.objectMapper = new ObjectMapper();
        this.endpoints = new ArrayList<>();
        this.mimeTypes = new HashMap<>();
        mimeTypes.put("html", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("json", "application/json");
    }

    public void registerController(Class<?> controllerClass) {
        String basePath = controllerClass.getAnnotation(Controller.class).value();
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                endpoints.add(EndpointDoc.fromMethod(method, basePath));
            }
        }
    }

    @RequestMapping(value = "/api", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, Object>> getApiDocs() {
        Map<String, Object> response = new HashMap<>();
        response.put("endpoints", endpoints);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/ui", method = RequestMapping.Method.GET)
    public void getSwaggerUI(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("Loading index.html...");
        byte[] content = readResource("docs/index.html");
        if (content != null) {
            System.out.println("Found index.html, size: " + content.length);
            response.setContentType("text/html");
            response.getOutputStream().write(content);
        } else {
            System.err.println("index.html not found!");
            ErrorResponse.send(response, request.getRequestURI(), HttpServletResponse.SC_NOT_FOUND, "Documentation UI not found");
        }
    }

    @RequestMapping(value = "/css/*", method = RequestMapping.Method.GET)
    public void getCSS(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) {
            ErrorResponse.send(response, request.getRequestURI(), HttpServletResponse.SC_NOT_FOUND, "CSS file not found");
            return;
        }
        
        System.out.println("Loading CSS: " + path);
        byte[] content = readResource("docs/css" + path);
        if (content != null) {
            System.out.println("Found CSS file, size: " + content.length);
            response.setContentType("text/css");
            response.getOutputStream().write(content);
        } else {
            System.err.println("CSS file not found: " + path);
            ErrorResponse.send(response, request.getRequestURI(), HttpServletResponse.SC_NOT_FOUND, "CSS file not found: " + path);
        }
    }

    @RequestMapping(value = "/js/*", method = RequestMapping.Method.GET)
    public void getJavaScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) {
            ErrorResponse.send(response, request.getRequestURI(), HttpServletResponse.SC_NOT_FOUND, "JavaScript file not found");
            return;
        }
        
        System.out.println("Loading JS: " + path);
        byte[] content = readResource("docs/js" + path);
        if (content != null) {
            System.out.println("Found JS file, size: " + content.length);
            response.setContentType("application/javascript");
            response.getOutputStream().write(content);
        } else {
            System.err.println("JS file not found: " + path);
            ErrorResponse.send(response, request.getRequestURI(), HttpServletResponse.SC_NOT_FOUND, "JavaScript file not found: " + path);
        }
    }

    private byte[] readResource(String resourcePath) {
        try {
            System.out.println("Attempting to load resource: " + resourcePath);
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                System.err.println("Resource not found: " + resourcePath);
                return null;
            }
            byte[] content = inputStream.readAllBytes();
            System.out.println("Successfully loaded resource: " + resourcePath + ", size: " + content.length);
            return content;
        } catch (IOException e) {
            System.err.println("Error reading resource: " + resourcePath);
            e.printStackTrace();
            return null;
        }
    }
} 