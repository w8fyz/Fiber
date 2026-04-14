package sh.fyz.fiber.handler;

import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;

public class SecurityResult {
    private final UserAuth authenticatedUser;
    private final OAuth2ApplicationInfo authenticatedApp;
    private final boolean proceed;

    private SecurityResult(UserAuth authenticatedUser, OAuth2ApplicationInfo authenticatedApp, boolean proceed) {
        this.authenticatedUser = authenticatedUser;
        this.authenticatedApp = authenticatedApp;
        this.proceed = proceed;
    }

    public static SecurityResult ok(UserAuth user, OAuth2ApplicationInfo app) {
        return new SecurityResult(user, app, true);
    }

    public static SecurityResult denied() {
        return new SecurityResult(null, null, false);
    }

    public UserAuth getAuthenticatedUser() {
        return authenticatedUser;
    }

    public OAuth2ApplicationInfo getAuthenticatedApp() {
        return authenticatedApp;
    }

    public boolean shouldProceed() {
        return proceed;
    }
}
