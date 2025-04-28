package sh.fyz.fiber.example.oauth2.providers;

import sh.fyz.fiber.core.authentication.oauth2.impl.DiscordOAuth2Provider;
import sh.fyz.fiber.example.repo.entities.ExampleUser;

import java.util.Map;

public class ExampleDiscordProvider extends DiscordOAuth2Provider<ExampleUser> {
    /**
     * Creates a new Discord OAuth2 provider.
     *
     * @param clientId     The Discord application client ID
     * @param clientSecret The Discord application client secret
     */
    public ExampleDiscordProvider(String clientId, String clientSecret) {
        super(clientId, clientSecret);
    }
    
    @Override
    public void mapUserData(Map<String, Object> userInfo, ExampleUser exampleUser) {
        exampleUser.setUsername((String) userInfo.get("username"));
        exampleUser.setEmail((String) userInfo.get("email"));
        exampleUser.setAvatar((String) userInfo.get("avatar"));
        exampleUser.setDiscriminator((String) userInfo.get("discriminator"));
    }
}
