package sh.fyz.fiber.example;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2AuthenticationService;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2Provider;
import sh.fyz.fiber.core.authentication.oauth2.ResponseUser;
import sh.fyz.fiber.example.repo.ExampleUserRepository;
import sh.fyz.fiber.example.repo.entities.ExampleUser;

import java.util.Map;

public class ExampleOAuthService extends OAuth2AuthenticationService<ExampleUser> {
    public ExampleOAuthService(AuthenticationService<ExampleUser> authenticationService, GenericRepository<ExampleUser> userRepository) {
        super(authenticationService, userRepository);
    }

    @Override
    protected ResponseUser<ExampleUser> findOrCreateUser(Map<String, Object> userInfo, OAuth2Provider<ExampleUser> provider) {
        String providerId = provider.getProviderId();
        String externalId = (String) userInfo.get("id");
        
        // Try to find existing user by provider ID and external ID
        ExampleUser existingExampleUser = ((ExampleUserRepository)userRepository).findByProviderIdAndExternalId(providerId, externalId);
        if (existingExampleUser != null) {
            // Update existing user information
            provider.mapUserData(userInfo, existingExampleUser);
            existingExampleUser = userRepository.save(existingExampleUser);
            return new ResponseUser<>(existingExampleUser, null, null);
        }
        
        // Create new user if not found
        ExampleUser newExampleUser = new ExampleUser();
        newExampleUser.setProviderId(providerId);
        newExampleUser.setExternalId(externalId);
        
        // Map user data from provider
        provider.mapUserData(userInfo, newExampleUser);
        
        // Save the new user
        userRepository.save(newExampleUser);
        return new  ResponseUser<>(newExampleUser, "CREATED", null);
    }
}
