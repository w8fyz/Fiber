package sh.fyz.fiber.handler;

import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.OpenIDService;

import java.io.IOException;

public class OpenIDHandler {
    private final OpenIDService openIDService;

    public OpenIDHandler(OpenIDService openIDService) {
        this.openIDService = openIDService;
    }

    public void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String authUrl = openIDService.createAuthorizationURL();
            response.sendRedirect(authUrl);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error during authentication initialization: " + e.getMessage());
        }
    }

    public void handleCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String code = request.getParameter("code");
        String state = request.getParameter("state");

        if (code == null || state == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing required parameters");
            return;
        }

        // Vérifier que le state correspond
        if (!state.equals(openIDService.getState().getValue())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid state parameter");
            return;
        }

        try {
            UserInfo userInfo = openIDService.processCallback(code);
            
            // Ici, vous pouvez créer une session utilisateur, générer un JWT, etc.
            response.setContentType("application/json");
            response.getWriter().write(userInfo.toJSONObject().toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error during authentication: " + e.getMessage());
        }
    }
} 