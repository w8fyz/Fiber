package sh.fyz.fiber.example;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import sh.fyz.fiber.annotations.Controller;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.core.authentication.OpenIDAuthenticationService;
import sh.fyz.fiber.FiberServer;

@Controller("auth")
public class OpenIDExampleController {
    private final OpenIDAuthenticationService<?> openIDService;

    public OpenIDExampleController() {
        this.openIDService = (OpenIDAuthenticationService<?>) FiberServer.get().getAuthService();
    }

    @RequestMapping(value = "/login", method = RequestMapping.Method.GET)
    public void login(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(true);
        OpenIDAuthenticationService.AuthorizationResponse authResponse = 
            openIDService.createAuthorizationRequest(session.getId());
        
        // Stocker le state dans la session pour vérification ultérieure
        session.setAttribute("openid_state", authResponse.getState());
        
        // Rediriger vers l'URL d'autorisation
        response.sendRedirect(authResponse.getAuthorizationUrl());
    }

    @RequestMapping(value = "/callback", method = RequestMapping.Method.GET)
    public void callback(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No session found");
            return;
        }

        String savedState = (String) session.getAttribute("openid_state");
        if (savedState == null || !savedState.equals(state)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid state parameter");
            return;
        }

        try {
            // Traiter le callback et obtenir l'utilisateur authentifié
            var user = openIDService.processCallback(code, state, session.getId());
            
            // Vous pouvez maintenant créer une session pour l'utilisateur, générer un JWT, etc.
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"Authentication successful\"}");
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Authentication failed: " + e.getMessage());
        }
    }
} 