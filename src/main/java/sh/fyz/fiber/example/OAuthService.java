package sh.fyz.fiber.example;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2AuthenticationService;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2Provider;
import sh.fyz.fiber.example.repo.UserRepository;
import sh.fyz.fiber.example.repo.entities.User;

import java.util.Map;

public class OAuthService extends OAuth2AuthenticationService<User> {
    public OAuthService(AuthenticationService<User> authenticationService, GenericRepository<User> userRepository) {
        super(authenticationService, userRepository);
    }

    @Override
    protected User findOrCreateUser(Map<String, Object> userInfo, OAuth2Provider<User> provider) {
        String providerId = provider.getProviderId();
        String externalId = (String) userInfo.get("id");
        
        // Try to find existing user by provider ID and external ID
        User existingUser = ((UserRepository)userRepository).findByProviderIdAndExternalId(providerId, externalId);
        if (existingUser != null) {
            // Update existing user information
            provider.mapUserData(userInfo, existingUser);
            existingUser = userRepository.save(existingUser);
            return existingUser;
        }
        
        // Create new user if not found
        User newUser = new User();
        newUser.setProviderId(providerId);
        newUser.setExternalId(externalId);
        
        // Map user data from provider
        provider.mapUserData(userInfo, newUser);
        
        // Save the new user
        userRepository.save(newUser);
        return newUser;
    }
}
