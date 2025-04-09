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
        String providerId = provider.getProviderId();
        String externalId = (String) userInfo.get("id");
        
        // Try to find existing user by provider ID and external ID
        User existingUser = ((UserRepository)userRepository).findByProviderIdAndExternalId(providerId, externalId);
        if (existingUser != null) {
            return existingUser;
        }
        
        // Create new user if not found
        User newUser = new User();
        newUser.setUsername((String) userInfo.get("username"));
        newUser.setEmail((String) userInfo.get("email"));
        newUser.setProviderId(providerId);
        newUser.setExternalId(externalId);
        
        // Set additional fields based on provider
        if (providerId.equals("discord")) {
            newUser.setAvatar((String) userInfo.get("avatar"));
            newUser.setDiscriminator((String) userInfo.get("discriminator"));
        }
        
        // Save the new user
        userRepository.save(newUser);
        return newUser;
    }
}
