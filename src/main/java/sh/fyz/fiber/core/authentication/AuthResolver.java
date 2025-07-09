package sh.fyz.fiber.core.authentication;

import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AuthResolver {
    private final List<Authenticator> authenticators;

    public AuthResolver() {
        this.authenticators = new ArrayList<>();
    }

    public void registerAuthenticator(Authenticator authenticator) {
        authenticators.add(authenticator);
    }

    public UserAuth resolveUser(HttpServletRequest request, Set<AuthScheme> accepted) {
        if(authenticators.isEmpty()) {
            //System.out.println("WARNING: No authenticators registered. Please register at least one authenticator.");
        }
        if (accepted == null || accepted.isEmpty()) {
            return null;
        }

        for (Authenticator auth : authenticators) {
            if (accepted.contains(auth.scheme())) {
                UserAuth user = auth.authenticate(request);
                if (user != null) {
                    return user;
                }
            }
        }
        return null;
    }
} 