package sh.fyz.fiber.docs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthResolver;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.EnumSet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller("/docs")
public class DocumentationController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentationController.class);

    private static final String DOCS_ADMIN_ROLE = "admin";

    private final List<EndpointDoc> endpoints;
    private final Map<String, String> mimeTypes;
    private final boolean filterInternal;

    public DocumentationController() {
        this(true);
    }

    public DocumentationController(boolean filterInternal) {
        this.endpoints = new ArrayList<>();
        this.mimeTypes = new HashMap<>();
        this.filterInternal = filterInternal;
        mimeTypes.put("html", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("json", "application/json");
    }

    public void registerController(Class<?> controllerClass) {
        String basePath = controllerClass.getAnnotation(Controller.class).value();
        if (filterInternal && basePath != null && basePath.startsWith("/internal")) {
            // Internal endpoints are hidden from the published docs.
            return;
        }
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                endpoints.add(EndpointDoc.fromMethod(method, basePath));
            }
        }
    }

    /**
     * In production, the doc endpoints require an authenticated user with the
     * {@code admin} role. In development, anonymous access is permitted.
     *
     * @return {@code true} if the request is authorised to view documentation.
     */
    private static boolean checkDocsAccess(HttpServletRequest request, HttpServletResponse response) {
        boolean dev;
        try {
            dev = FiberServer.get().isDev();
        } catch (Exception e) {
            dev = false;
        }
        if (dev) return true;

        AuthResolver authResolver;
        try {
            authResolver = FiberServer.get().getAuthResolver();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        UserAuth user = authResolver != null
                ? authResolver.resolveUser(request, EnumSet.allOf(AuthScheme.class))
                : null;
        if (user == null || user.getRole() == null
                || !DOCS_ADMIN_ROLE.equalsIgnoreCase(user.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    @RequestMapping(value = "/api", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, Object>> getApiDocs(HttpServletRequest request, HttpServletResponse response) {
        if (!checkDocsAccess(request, response)) {
            Map<String, Object> body = new HashMap<>();
            body.put("error", "Documentation requires admin role in production");
            return ResponseEntity.forbidden(body);
        }

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> formattedEndpoints = new ArrayList<>();
        for (EndpointDoc endpoint : endpoints) {
            Map<String, Object> formattedEndpoint = new HashMap<>();
            formattedEndpoint.put("path", endpoint.getPath());
            formattedEndpoint.put("method", endpoint.getMethod().toString());
            formattedEndpoint.put("description", endpoint.getDescription());

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

            List<Map<String, Object>> pathVariables = new ArrayList<>();
            for (EndpointDoc.PathVariableDoc pathVar : endpoint.getPathVariables()) {
                Map<String, Object> pathVarMap = new HashMap<>();
                pathVarMap.put("name", pathVar.getName());
                pathVarMap.put("type", pathVar.getType());
                pathVariables.add(pathVarMap);
            }
            formattedEndpoint.put("pathVariables", pathVariables);

            if (endpoint.getRequestBodyType() != null) {
                formattedEndpoint.put("requestBodyType", endpoint.getRequestBodyType().getSimpleName());
                formattedEndpoint.put("requestBodyExample", endpoint.getRequestBodyExample());
            }

            formattedEndpoint.put("responseType", endpoint.getResponseType().getSimpleName());
            formattedEndpoint.put("responseExample", endpoint.getResponseExample());

            formattedEndpoints.add(formattedEndpoint);
        }

        result.put("endpoints", formattedEndpoints);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/ui", method = RequestMapping.Method.GET)
    public ResponseEntity<byte[]> getUI(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!checkDocsAccess(request, response)) {
            return ResponseEntity.forbidden("Documentation requires admin role in production".getBytes());
        }

        byte[] content = readResource("docs/index.html");
        if (content != null) {
            ResponseEntity<byte[]> responseEntity = ResponseEntity.ok(content);
            responseEntity.contentType("text/html");
            return responseEntity;
        } else {
            logger.warn("docs/index.html resource not found");
            return ResponseEntity.notFound();
        }
    }

    @RequestMapping(value = "/css/*", method = RequestMapping.Method.GET)
    public ResponseEntity<byte[]> getCSS(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!checkDocsAccess(request, response)) {
            return ResponseEntity.forbidden("Documentation requires admin role in production".getBytes());
        }
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) {
            return ResponseEntity.notFound();
        }
        path = path.substring(1);
        byte[] content = readResource(path);
        if (content != null) {
            ResponseEntity<byte[]> responseEntity = ResponseEntity.ok(content);
            responseEntity.contentType("text/css");
            return responseEntity;
        } else {
            logger.warn("CSS file not found: {}", path);
            return ResponseEntity.notFound();
        }
    }

    @RequestMapping(value = "/js/*", method = RequestMapping.Method.GET)
    public ResponseEntity<byte[]> getJavaScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!checkDocsAccess(request, response)) {
            return ResponseEntity.forbidden("Documentation requires admin role in production".getBytes());
        }
        String path = request.getPathInfo();
        if (path == null || path.equals("/")) {
            return ResponseEntity.notFound();
        }
        path = path.substring(1);
        byte[] content = readResource(path);
        if (content != null) {
            ResponseEntity<byte[]> responseEntity = ResponseEntity.ok(content);
            responseEntity.contentType("application/javascript");
            return responseEntity;
        } else {
            logger.warn("JS file not found: {}", path);
            return ResponseEntity.notFound();
        }
    }

    private byte[] readResource(String resourcePath) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                logger.warn("Resource not found: {}", resourcePath);
                return null;
            }
            return inputStream.readAllBytes();
        } catch (IOException e) {
            logger.error("Error reading resource: {}", resourcePath, e);
            return null;
        }
    }
}
