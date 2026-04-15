package sh.fyz.fiber.core.authentication.impl;

import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.entities.OAuth2Client;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationAuthenticator;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class BasicAuthenticator implements OAuth2ApplicationAuthenticator {

    @Override
    public OAuth2ApplicationInfo authenticate(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            String credentials;
            try {
                credentials = new String(Base64.getDecoder().decode(base64Credentials));
            } catch (Exception e) {
                return null;
            }
            String[] values = credentials.split(":", 2);
            
            if (values.length == 2) {
                String clientId = values[0];
                String clientSecret = values[1];
                OAuth2Client appInfo = FiberServer.get().getOauthClientService().getClient(clientId);
                if (appInfo != null && MessageDigest.isEqual(
                        appInfo.getClientSecret().getBytes(StandardCharsets.UTF_8),
                        clientSecret.getBytes(StandardCharsets.UTF_8))) {
                    return new OAuth2ApplicationInfo(appInfo.getClientId(), appInfo.getClientSecret());
                }
            }
        }
        return null;
    }
} 