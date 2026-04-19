package sh.fyz.fiber.core.authentication.oauth2;

import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.JwtUtil;
import sh.fyz.fiber.core.authentication.entities.OAuth2Client;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.oauth2.client.controller.OAuth2ClientController;
import sh.fyz.fiber.core.security.BCryptUtil;
import sh.fyz.fiber.util.RandomUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2 client (RP) registry & authorization-code issuer.
 *
 * <p>Client secrets are stored as BCrypt hashes; the plaintext is only ever returned by
 * {@link #registerClient(String, String)} via {@link OAuth2ClientCredentials}.
 * Authorization codes are bound to {@code client_id} <b>and</b> {@code redirect_uri},
 * and accept optional PKCE (RFC 7636) parameters with {@code S256} or {@code plain}.</p>
 */
public class OAuth2ClientService {

    private final GenericRepository<OAuth2Client> clientRepository;
    private final Map<String, AuthorizationCode> authorizationCodes = new ConcurrentHashMap<>();
    private final Map<String, AuthorizationRequest> pendingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService codeCleanupExecutor;

    public OAuth2ClientService(GenericRepository<OAuth2Client> clientRepository, OAuth2ClientController controller) {
        this.clientRepository = clientRepository;
        this.codeCleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "oauth2-code-cleanup");
            t.setDaemon(true);
            return t;
        });
        if (controller == null) {
            controller = new OAuth2ClientController(this);
        }
        FiberServer.get().registerController(controller);
    }

    public OAuth2ClientService(GenericRepository<OAuth2Client> clientRepository) {
        this(clientRepository, null);
    }

    /**
     * Register a new OAuth2 client. The returned credentials object exposes the plaintext
     * secret <b>once</b>; subsequent calls to {@link #getClient(String)} only return the
     * BCrypt hash (in {@code clientSecret}).
     */
    public OAuth2ClientCredentials registerClient(String name, String redirectUri) {
        String plaintextSecret = generateClientSecret();
        OAuth2Client client = new OAuth2Client();
        client.setClientId(UUID.randomUUID().toString());
        client.setClientSecret(BCryptUtil.hashPassword(plaintextSecret));
        client.setName(name);
        client.setRedirectUri(redirectUri);
        client.setEnabled(true);
        OAuth2Client saved = clientRepository.save(client);
        return new OAuth2ClientCredentials(saved, plaintextSecret);
    }

    public OAuth2Client getClient(String clientId) {
        return clientRepository.findById(clientId);
    }

    /**
     * Verify the supplied {@code clientId}/{@code clientSecret} pair using BCrypt.
     * @return the matching client, or {@code null} on mismatch / unknown id / disabled.
     */
    public OAuth2Client getClientByCredentials(String clientId, String clientSecret) {
        if (clientId == null || clientSecret == null) return null;
        OAuth2Client client = clientRepository.findById(clientId);
        if (client == null || !client.isEnabled()) return null;
        if (!BCryptUtil.checkPassword(clientSecret, client.getClientSecret())) return null;
        return client;
    }

    public boolean validateRedirectUri(String clientId, String redirectUri) {
        OAuth2Client client = getClient(clientId);
        return client != null && redirectUri != null && redirectUri.equals(client.getRedirectUri());
    }

    private String generateClientSecret() {
        return RandomUtil.randomAlphanumeric(48);
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

    public String generateAuthorizationCode(UserAuth user, String clientId, String redirectUri) {
        return generateAuthorizationCode(user, clientId, redirectUri, null, null);
    }

    /**
     * Generate an authorization code bound to {@code clientId + redirectUri}, optionally
     * carrying a PKCE challenge. Codes are single-use and expire after 10 minutes.
     */
    public String generateAuthorizationCode(UserAuth user, String clientId, String redirectUri,
                                            String codeChallenge, String codeChallengeMethod) {
        if (codeChallenge != null) {
            if (codeChallengeMethod == null) {
                codeChallengeMethod = PkceUtil.METHOD_PLAIN;
            }
            if (!PkceUtil.isSupportedMethod(codeChallengeMethod)) {
                throw new IllegalArgumentException("Unsupported code_challenge_method: " + codeChallengeMethod);
            }
        }
        String code = UUID.randomUUID().toString();
        AuthorizationCode authCode = new AuthorizationCode(code, user, clientId, redirectUri,
                codeChallenge, codeChallengeMethod);
        authorizationCodes.put(code, authCode);
        scheduleCodeCleanup(code);
        return code;
    }

    public AuthorizationCode validateAuthorizationCode(String code, String clientId,
                                                       String redirectUri, String codeVerifier) {
        if (code == null || clientId == null) return null;
        AuthorizationCode authCode = authorizationCodes.remove(code);
        if (authCode == null) return null;
        if (authCode.isExpired()) return null;
        if (!clientId.equals(authCode.getClientId())) return null;
        if (!java.util.Objects.equals(redirectUri, authCode.getRedirectUri())) {
            return null;
        }
        if (authCode.getCodeChallenge() != null) {
            if (!PkceUtil.verify(codeVerifier, authCode.getCodeChallenge(),
                    authCode.getCodeChallengeMethod())) {
                return null;
            }
        }
        return authCode;
    }

    /** Backwards-compatible overload — equivalent to no PKCE and no redirect_uri binding. */
    @Deprecated
    public AuthorizationCode validateAuthorizationCode(String code, String clientId) {
        AuthorizationCode authCode = authorizationCodes.remove(code);
        if (authCode == null || authCode.isExpired() || !authCode.getClientId().equals(clientId)) {
            return null;
        }
        if (authCode.getCodeChallenge() != null) {
            // PKCE was requested but no verifier provided — refuse.
            return null;
        }
        return authCode;
    }

    public String generateAccessToken(UserAuth user, String clientId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId());
        claims.put("client_id", clientId);
        claims.put("type", "access");
        return JwtUtil.createToken(claims, 3600000); // 1 hour
    }

    private void scheduleCodeCleanup(String code) {
        codeCleanupExecutor.schedule(() -> authorizationCodes.remove(code), 10, TimeUnit.MINUTES);
    }

    public void shutdown() {
        codeCleanupExecutor.shutdown();
        try {
            if (!codeCleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                codeCleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            codeCleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class AuthorizationRequest {
        private final String clientId;
        private final String redirectUri;
        private final String state;

        public AuthorizationRequest(String clientId, String redirectUri, String state) {
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.state = state;
        }

        public String getClientId() { return clientId; }
        public String getRedirectUri() { return redirectUri; }
        public String getState() { return state; }
    }

    public static class AuthorizationCode {
        private final String code;
        private final UserAuth user;
        private final String clientId;
        private final String redirectUri;
        private final String codeChallenge;
        private final String codeChallengeMethod;
        private final long expiresAt;

        public AuthorizationCode(String code, UserAuth user, String clientId,
                                 String redirectUri, String codeChallenge,
                                 String codeChallengeMethod) {
            this.code = code;
            this.user = user;
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.codeChallenge = codeChallenge;
            this.codeChallengeMethod = codeChallengeMethod;
            this.expiresAt = System.currentTimeMillis() + 600_000L;
        }

        public AuthorizationCode(String code, UserAuth user, String clientId) {
            this(code, user, clientId, null, null, null);
        }

        public String getCode() { return code; }
        public UserAuth getUser() { return user; }
        public String getClientId() { return clientId; }
        public String getRedirectUri() { return redirectUri; }
        public String getCodeChallenge() { return codeChallenge; }
        public String getCodeChallengeMethod() { return codeChallengeMethod; }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    public OAuth2Client updateClient(OAuth2Client client) {
        return clientRepository.save(client);
    }

    public void deleteClient(String clientId) {
        OAuth2Client client = getClient(clientId);
        if (client != null) {
            clientRepository.delete(client);
        }
    }
}
