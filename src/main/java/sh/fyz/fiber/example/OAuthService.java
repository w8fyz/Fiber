package sh.fyz.fiber.example;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2AuthenticationService;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2Provider;

import java.util.Map;

public class OAuthService extends OAuth2AuthenticationService<User> {
    public OAuthService(AuthenticationService<User> authenticationService, GenericRepository<User> userRepository) {
        super(authenticationService, userRepository);
    }

    @Override
    protected User findOrCreateUser(Map<String, Object> userInfo, OAuth2Provider<User> provider) {
        return null;
    }
}
