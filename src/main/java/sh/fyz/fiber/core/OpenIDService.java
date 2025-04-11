package sh.fyz.fiber.core;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import sh.fyz.fiber.config.OpenIDConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenIDService {
    private final OpenIDConfig config;
    private final Nonce nonce;
    private final State state;

    public OpenIDService(OpenIDConfig config) {
        this.config = config;
        this.nonce = new Nonce();
        this.state = new State();
    }

    public String createAuthorizationURL() throws URISyntaxException {
        ClientID clientID = new ClientID(config.getClientId());
        URI redirectURI = new URI(config.getRedirectUri());
        Scope scope = Scope.parse(config.getScope());

        AuthenticationRequest authRequest = new AuthenticationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE),
                scope,
                clientID,
                redirectURI)
                .state(state)
                .nonce(nonce)
                .build();

        return authRequest.toURI().toString();
    }

    public UserInfo processCallback(String code) throws IOException, ParseException, URISyntaxException {
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

        return userInfoResponse.toSuccessResponse().getUserInfo();
    }

    public State getState() {
        return state;
    }
} 