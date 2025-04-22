package sh.fyz.fiber.core.authentication.oauth2;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.entities.OAuth2Client;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.JwtUtil;
import sh.fyz.fiber.core.authentication.oauth2.client.controller.OAuth2ClientController;
import sh.fyz.fiber.util.RandomUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OAuth2ClientService {
    private final GenericRepository<OAuth2Client> clientRepository;
    private final Map<String, AuthorizationCode> authorizationCodes = new ConcurrentHashMap<>();
    private final Map<String, AuthorizationRequest> pendingRequests = new ConcurrentHashMap<>();

    public OAuth2ClientService(GenericRepository<OAuth2Client> clientRepository, OAuth2ClientController controller) {
        this.clientRepository = clientRepository;
        if (controller == null) {
            controller = new OAuth2ClientController(this);
        }
        FiberServer.get().registerController(controller);
    }

    public OAuth2ClientService(GenericRepository<OAuth2Client> clientRepository) {
        this(clientRepository, null);
    }

    public OAuth2Client registerClient(String name, String redirectUri) {
        OAuth2Client client = new OAuth2Client();
        client.setClientId(UUID.randomUUID().toString());
        client.setClientSecret(generateClientSecret());
        client.setName(name);
        client.setRedirectUri(redirectUri);
        client.setEnabled(true);
        return clientRepository.save(client);
    }

    public OAuth2Client getClient(String clientId) {
        return clientRepository.findById(clientId);
    }

    public OAuth2Client getClientByCredentials(String clientId, String clientSecret) {
        return clientRepository.whereList("clientId", clientId)
            .stream()
            .filter(client -> client.getClientSecret().equals(clientSecret))
            .findFirst()
            .orElse(null);
    }

    public boolean validateRedirectUri(String clientId, String redirectUri) {
        OAuth2Client client = getClient(clientId);
        return client != null && client.getRedirectUri().equals(redirectUri);
    }

    private String generateClientSecret() {
        return RandomUtil.randomAlphanumeric(32);
    }

    public List<OAuth2Client> getAllClients() {
        return clientRepository.all();
    }

    public void disableClient(String clientId) {
        OAuth2Client client = getClient(clientId);
        if (client != null) {
            client.setEnabled(false);
            clientRepository.save(client);
        }
    }
    
    /**
     * Generate an authorization code for a user and client
     * @param user The user authorizing the client
     * @param clientId The OAuth client ID
     * @return The generated authorization code
     */
    public String generateAuthorizationCode(UserAuth user, String clientId) {
        // Generate a random code
        String code = UUID.randomUUID().toString();
        
        // Create authorization code object
        AuthorizationCode authCode = new AuthorizationCode(code, user, clientId);
        
        // Store the authorization code
        authorizationCodes.put(code, authCode);
        
        // Schedule cleanup of expired codes
        scheduleCodeCleanup(code);
        
        return code;
    }
    
    /**
     * Validate and exchange an authorization code
     * @param code The authorization code
     * @param clientId The OAuth client ID
     * @return The authorization code object or null if invalid
     */
    public AuthorizationCode validateAuthorizationCode(String code, String clientId) {
        // Get the authorization code
        AuthorizationCode authCode = authorizationCodes.get(code);
        
        // Check if code exists and is valid
        if (authCode == null || authCode.isExpired() || !authCode.getClientId().equals(clientId)) {
            return null;
        }
        
        // Remove the code to prevent reuse (single-use)
        authorizationCodes.remove(code);
        
        return authCode;
    }
    
    /**
     * Generate an access token for a user and client
     * @param user The user
     * @param clientId The OAuth client ID
     * @return The generated access token
     */
    public String generateAccessToken(UserAuth user, String clientId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId());
        claims.put("client_id", clientId);
        claims.put("type", "access");
        return JwtUtil.createToken(claims, 3600000); // 1 hour
    }
    
    /**
     * Schedule cleanup of an authorization code
     * @param code The authorization code to clean up
     */
    private void scheduleCodeCleanup(String code) {
        // In a real implementation, use a proper scheduler like Quartz or a database with TTL
        // For this example, we'll use a simple thread that sleeps and then removes the code
        new Thread(() -> {
            try {
                // Sleep for 10 minutes (code expiration time)
                Thread.sleep(600000);
                // Remove the code if it still exists
                authorizationCodes.remove(code);
            } catch (InterruptedException e) {
                // Thread interrupted, ignore
            }
        }).start();
    }
    
    /**
     * Internal class for storing authorization requests
     */
    public static class AuthorizationRequest {
        private final String clientId;
        private final String redirectUri;
        private final String state;

        public AuthorizationRequest(String clientId, String redirectUri, String state) {
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.state = state;
        }
        
        public String getClientId() {
            return clientId;
        }
        
        public String getRedirectUri() {
            return redirectUri;
        }
        
        public String getState() {
            return state;
        }
    }
    
    /**
     * Internal class for storing authorization codes
     */
    public static class AuthorizationCode {
        private final String code;
        private final UserAuth user;
        private final String clientId;
        private final long expiresAt;

        public AuthorizationCode(String code, UserAuth user, String clientId) {
            this.code = code;
            this.user = user;
            this.clientId = clientId;
            this.expiresAt = System.currentTimeMillis() + 600000; // 10 minutes
        }

        public String getCode() {
            return code;
        }

        public UserAuth getUser() {
            return user;
        }

        public String getClientId() {
            return clientId;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * Update an OAuth2 client
     * @param client The client to update
     * @return The updated client
     */
    public OAuth2Client updateClient(OAuth2Client client) {
        return clientRepository.save(client);
    }
    
    /**
     * Delete an OAuth2 client
     * @param clientId The ID of the client to delete
     */
    public void deleteClient(String clientId) {
        OAuth2Client client = getClient(clientId);
        if (client != null) {
            clientRepository.delete(client);
        }
    }
} 