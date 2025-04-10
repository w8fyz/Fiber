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
        
        // Format the endpoints in a way that's compatible with the UI
        List<Map<String, Object>> formattedEndpoints = new ArrayList<>();
        for (EndpointDoc endpoint : endpoints) {
            Map<String, Object> formattedEndpoint = new HashMap<>();
            formattedEndpoint.put("path", endpoint.getPath());
            formattedEndpoint.put("method", endpoint.getMethod().toString());
            formattedEndpoint.put("description", endpoint.getDescription());
            
            // Add parameters
            List<Map<String, Object>> parameters = new ArrayList<>();
            for (EndpointDoc.ParameterDoc param : endpoint.getParameters()) {
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("name", param.getName());
                paramMap.put("type", param.getType());
                paramMap.put("required", param.isRequired());
                paramMap.put("validationMessages", param.getValidationMessages());
                parameters.add(paramMap);
            }
            formattedEndpoint.put("parameters", parameters);
            
            // Add request body type if present
            if (endpoint.getRequestBodyType() != null) {
                formattedEndpoint.put("requestBodyType", endpoint.getRequestBodyType().getSimpleName());
                formattedEndpoint.put("requestBodyExample", endpoint.getRequestBodyExample());
            }
            
            // Add response type
            formattedEndpoint.put("responseType", endpoint.getResponseType().getSimpleName());
            formattedEndpoint.put("responseExample", endpoint.getResponseExample());
            
            formattedEndpoints.add(formattedEndpoint);
        }
        
        response.put("endpoints", formattedEndpoints);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/ui", method = RequestMapping.Method.GET)
    public ResponseEntity<byte[]> getUI(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("Loading index.html...");
        byte[] content = readResource("docs/index.html");
        if (content != null) {
            System.out.println("Found index.html, size: " + content.length);
            ResponseEntity<byte[]> responseEntity = ResponseEntity.ok(content);
            responseEntity.contentType("text/html");
            return responseEntity;
        } else {
            System.err.println("index.html not found!");
            return ResponseEntity.notFound();
        }
    }

    @RequestMapping(value = "/css/*", method = RequestMapping.Method.GET)
    public ResponseEntity<byte[]> getCSS(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) {
            return ResponseEntity.notFound();
        }
        path = path.substring(1);
        System.out.println("Loading CSS: " + path);
        byte[] content = readResource(path);
        if (content != null) {
            System.out.println("Found CSS file, size: " + content.length);
            ResponseEntity<byte[]> responseEntity = ResponseEntity.ok(content);
            responseEntity.contentType("text/css");
            return responseEntity;
        } else {
            System.err.println("CSS file not found: " + path);
            return ResponseEntity.notFound();
        }
    }

    @RequestMapping(value = "/js/*", method = RequestMapping.Method.GET)
    public ResponseEntity<byte[]> getJavaScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) {
            return ResponseEntity.notFound();
        }
        path = path.substring(1);
        System.out.println("Loading JS: " + path);
        byte[] content = readResource(path);
        if (content != null) {
            System.out.println("Found JS file, size: " + content.length);
            ResponseEntity<byte[]> responseEntity = ResponseEntity.ok(content);
            responseEntity.contentType("application/javascript");
            return responseEntity;
        } else {
            System.err.println("JS file not found: " + path);
            return ResponseEntity.notFound();
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