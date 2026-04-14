package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;
import sh.fyz.fiber.core.security.processors.PermissionProcessor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

public class SecurityPipeline {
    private final Method method;
    private final boolean noCsrf;
    private final boolean needsBasicAuth;
    private final Set<AuthScheme> acceptedAuthSchemes;

    public SecurityPipeline(Method method, boolean noCsrf, boolean needsBasicAuth, Set<AuthScheme> acceptedAuthSchemes) {
        this.method = method;
        this.noCsrf = noCsrf;
        this.needsBasicAuth = needsBasicAuth;
        this.acceptedAuthSchemes = acceptedAuthSchemes;
    }

    public SecurityResult execute(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!noCsrf && FiberServer.get().getCsrfMiddleware() != null) {
            if (!FiberServer.get().getCsrfMiddleware().handle(req, resp)) {
                return SecurityResult.denied();
            }
        }

        OAuth2ApplicationInfo authenticatedApp = null;
        if (needsBasicAuth) {
            authenticatedApp = FiberServer.get().getBasicAuthenticator().authenticate(req);
            if (authenticatedApp == null) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "Invalid client credentials");
                return SecurityResult.denied();
            }
        }

        UserAuth authenticatedUser = null;
        if (!acceptedAuthSchemes.isEmpty()) {
            authenticatedUser = FiberServer.get().getAuthResolver().resolveUser(req, acceptedAuthSchemes);
            if (authenticatedUser == null) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return SecurityResult.denied();
            }
            req.setAttribute("userId", authenticatedUser.getId());
        }

        Object permissionResult = PermissionProcessor.process(method, authenticatedUser);
        if (permissionResult != null) {
            ResponseEntity<?> permResponse = (ResponseEntity<?>) permissionResult;
            permResponse.write(req, resp);
            return SecurityResult.denied();
        }

        return SecurityResult.ok(authenticatedUser, authenticatedApp);
    }
}
