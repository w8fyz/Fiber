package sh.fyz.fiber.example;

import sh.fyz.fiber.core.authentication.oauth2.impl.DiscordOAuth2Provider;

import java.util.Map;

public class DiscordProvider extends DiscordOAuth2Provider<User> {
    /**
     * Creates a new Discord OAuth2 provider.
     *
     * @param clientId     The Discord application client ID
     * @param clientSecret The Discord application client secret
     */
    public DiscordProvider(String clientId, String clientSecret) {
        super(clientId, clientSecret);
    }
    
    @Override
    public void mapUserData(Map<String, Object> userInfo, User user) {
        user.setUsername((String) userInfo.get("username"));
        user.setEmail((String) userInfo.get("email"));
        user.setAvatar((String) userInfo.get("avatar"));
        user.setDiscriminator((String) userInfo.get("discriminator"));
    }
}
