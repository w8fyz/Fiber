package sh.fyz.fiber.unit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2AuthenticationService;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2CallbackResult;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2Provider;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2TokenResponse;
import sh.fyz.fiber.core.authentication.oauth2.UserOAuth2TokenService;
import sh.fyz.fiber.util.ResponseContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OAuth2AuthenticationServiceTest {

    @SuppressWarnings("unchecked")
    private AuthenticationService<UserAuth> authService;
    @SuppressWarnings("unchecked")
    private GenericRepository<UserAuth> userRepository;
    private UserOAuth2TokenService tokenService;
    @SuppressWarnings("unchecked")
    private OAuth2Provider<UserAuth> provider;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private TestOAuth2Service service;
    private UserAuth createdUser;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        authService = (AuthenticationService<UserAuth>) mock(AuthenticationService.class);
        userRepository = (GenericRepository<UserAuth>) mock(GenericRepository.class);
        tokenService = mock(UserOAuth2TokenService.class);
        provider = (OAuth2Provider<UserAuth>) mock(OAuth2Provider.class);

        when(provider.getProviderId()).thenReturn("discord");
        when(provider.getIdField()).thenReturn("id");

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        createdUser = mock(UserAuth.class);
        when(createdUser.getId()).thenReturn("user-42");

        service = new TestOAuth2Service(authService, userRepository, tokenService, createdUser);
        service.registerProvider(provider);
    }

    @Test
    void shortCircuitsWhenRequestIsAlreadyAuthenticated() {
        UserAuth existing = mock(UserAuth.class);
        when(authService.resolveFromRequest(request)).thenReturn(existing);

        ResponseContext<UserAuth> ctx = service.handleCallback("code-xyz", "state-xyz",
                "https://example.com/cb", request, response);

        assertSame(existing, ctx.getResult());
        verify(provider, never()).processCallback(anyString(), anyString());
        verify(tokenService, never()).saveOrUpdate(any(), anyString(), any());
        verify(authService, never()).setAuthCookies(any(), any(), any());
    }

    @Test
    void persistsTokensAfterSuccessfulFreshLogin() {
        when(authService.resolveFromRequest(request)).thenReturn(null);

        String state = service.getAuthorizationUrlState("discord", "https://example.com/cb");

        OAuth2TokenResponse tokens = new OAuth2TokenResponse(
                "access-123", "Bearer", 3600L, "refresh-xyz", "identify email");
        OAuth2CallbackResult result = new OAuth2CallbackResult(Map.of("id", "discord-uid-1"), tokens);
        when(provider.processCallback("code-ok", "https://example.com/cb")).thenReturn(result);

        ResponseContext<UserAuth> ctx = service.handleCallback("code-ok", state,
                "https://example.com/cb", request, response);

        assertSame(createdUser, ctx.getResult());
        verify(provider).processCallback("code-ok", "https://example.com/cb");
        verify(tokenService).saveOrUpdate("user-42", "discord", tokens);
        verify(authService).setAuthCookies(createdUser, request, response);
    }

    @Test
    void expiredStateRejected() {
        when(authService.resolveFromRequest(request)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                service.handleCallback("code", "bogus-state", "https://example.com/cb", request, response));
        verify(provider, never()).processCallback(anyString(), anyString());
    }

    @Test
    void getValidAccessTokenDelegatesToTokenService() {
        when(tokenService.getValidAccessToken("user-42", provider)).thenReturn("access-cached");
        assertEquals("access-cached", service.getValidAccessToken(createdUser, "discord"));
    }

    @Test
    void getValidAccessTokenReturnsNullForUnknownProvider() {
        assertNull(service.getValidAccessToken(createdUser, "unknown"));
        verifyNoInteractions(tokenService);
    }

    /**
     * Concrete subclass exposing a hook to mint state entries without
     * reaching into private state, and plugging in a deterministic user
     * factory for findOrCreateUser.
     */
    private static class TestOAuth2Service extends OAuth2AuthenticationService<UserAuth> {
        private final UserAuth user;

        TestOAuth2Service(AuthenticationService<UserAuth> authService,
                          GenericRepository<UserAuth> repo,
                          UserOAuth2TokenService tokenService,
                          UserAuth user) {
            super(authService, repo, tokenService);
            this.user = user;
        }

        String getAuthorizationUrlState(String providerId, String redirectUri) {
            OAuth2Provider<UserAuth> p = getProvider(providerId);
            when(p.getAuthorizationUrl(anyString(), eq(redirectUri)))
                    .thenAnswer(inv -> "https://auth/?state=" + inv.getArgument(0));
            String url = getAuthorizationUrl(providerId, redirectUri);
            return url.substring(url.indexOf("state=") + "state=".length());
        }

        @Override
        protected ResponseContext<UserAuth> findOrCreateUser(Map<String, Object> userInfo,
                                                             OAuth2Provider<UserAuth> provider) {
            return new ResponseContext<>(user, null, null);
        }
    }
}
