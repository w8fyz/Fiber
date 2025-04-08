package sh.fyz.fiber.example;

import sh.fyz.fiber.core.authentication.oauth2.impl.DiscordOAuth2Provider;

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
}
