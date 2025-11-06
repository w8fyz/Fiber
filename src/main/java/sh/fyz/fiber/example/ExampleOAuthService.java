package sh.fyz.fiber.example;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2AuthenticationService;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2Provider;
import sh.fyz.fiber.example.repo.ExampleUserRepository;
import sh.fyz.fiber.example.repo.entities.ExampleUser;
import sh.fyz.fiber.util.ResponseContext;

import java.util.Map;

public class ExampleOAuthService extends OAuth2AuthenticationService<ExampleUser> {
    public ExampleOAuthService(AuthenticationService<ExampleUser> authenticationService, GenericRepository<ExampleUser> userRepository) {
        super(authenticationService, userRepository);
    }

    @Override
    protected ResponseContext<ExampleUser> findOrCreateUser(Map<String, Object> userInfo, OAuth2Provider<ExampleUser> provider) {
        String providerId = provider.getProviderId();
        String externalId = (String) userInfo.get("id");
        
return null;
    }
}
