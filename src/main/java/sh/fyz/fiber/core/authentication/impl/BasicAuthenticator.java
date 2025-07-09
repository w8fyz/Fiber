package sh.fyz.fiber.core.authentication.impl;

import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.entities.OAuth2Client;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationAuthenticator;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicAuthenticator implements OAuth2ApplicationAuthenticator {

    @Override
    public OAuth2ApplicationInfo authenticate(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        //System.out.println("FOUND HEADER : "+authHeader);
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            //System.out.println("Base64 : "+base64Credentials);
            String credentials;
            try {
                credentials = new String(Base64.getDecoder().decode(base64Credentials));
                //System.out.println("Credentials : "+credentials);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            String[] values = credentials.split(":", 2);
            
            if (values.length == 2) {
                String clientId = values[0];
                String clientSecret = values[1];
                //System.out.println("Client ID : "+clientId);
                //System.out.println("Client Secret : "+clientSecret);
                OAuth2Client appInfo = FiberServer.get().getOauthClientService().getClient(clientId);
                //System.out.println("appInfo : "+(appInfo != null ? appInfo.getClientId() : "null"));
                if (appInfo != null && appInfo.getClientSecret().equals(clientSecret)) {
                    return new OAuth2ApplicationInfo(appInfo.getClientId(), appInfo.getClientSecret());
                }
            }
        }
        return null;
    }
} 