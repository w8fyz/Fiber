package sh.fyz.fiber.core.authentication;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import sh.fyz.fiber.config.OpenIDConfig;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;

public class OpenIDAuthenticationService<T extends UserAuth> extends AuthenticationService<T> {
    private final OpenIDConfig config;
    private final ConcurrentHashMap<String, State> stateStore;
    private final ConcurrentHashMap<String, Nonce> nonceStore;

    public OpenIDAuthenticationService(OpenIDConfig config, Class<T> userClass) {
        super(userClass);
        this.config = config;
        this.stateStore = new ConcurrentHashMap<>();
        this.nonceStore = new ConcurrentHashMap<>();
    }

    public AuthorizationResponse createAuthorizationRequest(String sessionId) throws URISyntaxException {
        ClientID clientID = new ClientID(config.getClientId());
        URI redirectURI = new URI(config.getRedirectUri());
        Scope scope = Scope.parse(config.getScope());

        State state = new State();
        Nonce nonce = new Nonce();
        
        // Store state and nonce for later verification
        stateStore.put(sessionId, state);
        nonceStore.put(sessionId, nonce);

        AuthenticationRequest authRequest = new AuthenticationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE),
                scope,
                clientID,
                redirectURI)
                .state(state)
                .nonce(nonce)
                .build();

        return new AuthorizationResponse(authRequest.toURI().toString(), state.getValue());
    }

    public T processCallback(String code, String state, String sessionId) throws IOException, ParseException, URISyntaxException {
        // Vérifier le state
        State savedState = stateStore.get(sessionId);
        if (savedState == null || !savedState.getValue().equals(state)) {
            throw new IOException("Invalid state parameter");
        }

        // Créer les identifiants du client
        ClientID clientID = new ClientID(config.getClientId());
        Secret clientSecret = new Secret(config.getClientSecret());
        URI redirectURI = new URI(config.getRedirectUri());
        URI tokenEndpoint = new URI(config.getIssuerUri() + "/protocol/openid-connect/token");

        // Créer la requête de token
        TokenRequest tokenRequest = new TokenRequest(
                tokenEndpoint,
                new ClientSecretBasic(clientID, clientSecret),
                new AuthorizationCodeGrant(new AuthorizationCode(code), redirectURI)
        );

        // Envoyer la requête et obtenir la réponse
        TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenRequest.toHTTPRequest().send());
        if (!tokenResponse.indicatesSuccess()) {
            TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
            throw new IOException("Token request failed: " + errorResponse.getErrorObject().getDescription());
        }

        AccessToken accessToken = tokenResponse.toSuccessResponse().getTokens().getAccessToken();

        // Obtenir les informations utilisateur
        URI userInfoEndpoint = new URI(config.getIssuerUri() + "/protocol/openid-connect/userinfo");
        UserInfoRequest userInfoRequest = new UserInfoRequest(userInfoEndpoint, accessToken);
        UserInfoResponse userInfoResponse = UserInfoResponse.parse(userInfoRequest.toHTTPRequest().send());

        if (!userInfoResponse.indicatesSuccess()) {
            throw new IOException("UserInfo request failed");
        }

        UserInfo userInfo = userInfoResponse.toSuccessResponse().getUserInfo();
        
        // Nettoyer les données de session
        stateStore.remove(sessionId);
        nonceStore.remove(sessionId);

        // Créer et retourner l'utilisateur authentifié
        return createAuthenticatedUser(userInfo);
    }

    protected T createAuthenticatedUser(UserInfo userInfo) {
        // Cette méthode doit être implémentée par les classes filles pour créer
        // un utilisateur authentifié à partir des informations OpenID
        throw new UnsupportedOperationException("createAuthenticatedUser must be implemented by subclasses");
    }

    public static class AuthorizationResponse {
        private final String authorizationUrl;
        private final String state;

        public AuthorizationResponse(String authorizationUrl, String state) {
            this.authorizationUrl = authorizationUrl;
            this.state = state;
        }

        public String getAuthorizationUrl() {
            return authorizationUrl;
        }

        public String getState() {
            return state;
        }
    }
} 