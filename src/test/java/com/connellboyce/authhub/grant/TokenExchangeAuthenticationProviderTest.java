package com.connellboyce.authhub.grant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenExchangeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenExchangeAuthenticationProviderTest {

	private JwtDecoder jwtDecoder;
	private OAuth2AuthorizationService authorizationService;
	private OAuth2TokenGenerator<org.springframework.security.oauth2.core.OAuth2Token> tokenGenerator;
	private RegisteredClientRepository registeredClientRepository;
	private TokenExchangeAuthenticationProvider provider;

	private static final String SUBJECT_TOKEN = "subject-jwt-token";
	private static final String ACCESS_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setup() {
		jwtDecoder = mock(JwtDecoder.class);
		authorizationService = mock(OAuth2AuthorizationService.class);
		tokenGenerator = mock(OAuth2TokenGenerator.class);
		registeredClientRepository = mock(RegisteredClientRepository.class);
		provider = new TokenExchangeAuthenticationProvider(jwtDecoder, authorizationService, tokenGenerator, registeredClientRepository);

		AuthorizationServerContext context = mock(AuthorizationServerContext.class);
		when(context.getIssuer()).thenReturn("http://localhost");
		when(context.getAuthorizationServerSettings()).thenReturn(AuthorizationServerSettings.builder().build());
		AuthorizationServerContextHolder.setContext(context);
	}

	@AfterEach
	void tearDown() {
		AuthorizationServerContextHolder.resetContext();
	}

	private RegisteredClient exchangeCapableClient() {
		return RegisteredClient.withId("client-1")
				.clientId("client-1")
				.clientSecret("secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
				.scope("openid")
				.build();
	}

	private OAuth2ClientAuthenticationToken authenticatedClientPrincipal(RegisteredClient client) {
		return new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "secret");
	}

	private OAuth2TokenExchangeAuthenticationToken tokenExchangeRequest(Authentication clientPrincipal, String subjectTokenType, String requestedTokenType) {
		return new OAuth2TokenExchangeAuthenticationToken(
				requestedTokenType,
				SUBJECT_TOKEN,
				subjectTokenType,
				clientPrincipal,
				null,
				null,
				null,
				null,
				Set.of("openid"),
				null
		);
	}

	private Jwt validJwt() {
		return Jwt.withTokenValue(SUBJECT_TOKEN)
				.header("alg", "RS256")
				.subject("user-1")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.claim("sub", "user-1")
				.build();
	}

	@Test
	void supports_onlyTokenExchangeToken() {
		assertTrue(provider.supports(OAuth2TokenExchangeAuthenticationToken.class));
		assertFalse(provider.supports(UsernamePasswordAuthenticationToken.class));
	}

	@Test
	void authenticate_unrecognizedSubjectTokenType_throwsInvalidRequest() {
		RegisteredClient client = exchangeCapableClient();
		Authentication clientPrincipal = authenticatedClientPrincipal(client);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(clientPrincipal, "not-a-real-token-type", ACCESS_TOKEN_TYPE);

		OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
				() -> provider.authenticate(request));
		assertEquals("invalid_request", ex.getError().getErrorCode());
	}

	@Test
	void authenticate_subjectTokenTypeNotAccessToken_throwsUnsupportedTokenType() {
		RegisteredClient client = exchangeCapableClient();
		Authentication clientPrincipal = authenticatedClientPrincipal(client);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(
				clientPrincipal, TokenType.REFRESH_TOKEN.getPotentialValues().get(0), ACCESS_TOKEN_TYPE);

		OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
				() -> provider.authenticate(request));
		assertEquals("unsupported_token_type", ex.getError().getErrorCode());
	}

	@Test
	void authenticate_requestedTokenTypeNotAccessToken_throwsUnsupportedTokenType() {
		RegisteredClient client = exchangeCapableClient();
		Authentication clientPrincipal = authenticatedClientPrincipal(client);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(
				clientPrincipal, ACCESS_TOKEN_TYPE, TokenType.ID_TOKEN.getPotentialValues().get(0));

		OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
				() -> provider.authenticate(request));
		assertEquals("unsupported_token_type", ex.getError().getErrorCode());
	}

	// NOTE: an unrecognized requested_token_type is silently ignored rather than
	// rejected -- only a *recognized but unsupported* type (e.g. id_token) is treated
	// as an error (see the test above). This documents that current behavior.
	@Test
	void authenticate_unrecognizedRequestedTokenType_isIgnoredNotRejected() {
		RegisteredClient client = exchangeCapableClient();
		Authentication clientPrincipal = authenticatedClientPrincipal(client);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(
				clientPrincipal, ACCESS_TOKEN_TYPE, "not-a-real-token-type");

		when(jwtDecoder.decode(SUBJECT_TOKEN)).thenReturn(validJwt());
		when(registeredClientRepository.findByClientId("client-1")).thenReturn(client);

		OAuth2AccessToken generatedToken = new OAuth2AccessToken(
				OAuth2AccessToken.TokenType.BEARER, "generated-token", Instant.now(), Instant.now().plusSeconds(300));
		when(tokenGenerator.generate(any())).thenReturn(generatedToken);

		Authentication result = provider.authenticate(request);

		assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, result);
	}

	@Test
	void authenticate_undecodableSubjectToken_throwsInvalidGrant() {
		RegisteredClient client = exchangeCapableClient();
		Authentication clientPrincipal = authenticatedClientPrincipal(client);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(clientPrincipal, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_TYPE);

		when(jwtDecoder.decode(SUBJECT_TOKEN)).thenThrow(new JwtException("bad token"));

		OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
				() -> provider.authenticate(request));
		assertEquals("invalid_grant", ex.getError().getErrorCode());
	}

	@Test
	void authenticate_unknownClient_throws() {
		Authentication clientPrincipal = new UsernamePasswordAuthenticationToken("unknown-client", "creds");
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(clientPrincipal, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_TYPE);

		when(jwtDecoder.decode(SUBJECT_TOKEN)).thenReturn(validJwt());
		when(registeredClientRepository.findByClientId("unknown-client")).thenReturn(null);

		assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(request));
	}

	@Test
	void authenticate_clientPrincipalNotOAuth2ClientAuthenticationToken_throws() {
		RegisteredClient client = exchangeCapableClient();
		Authentication clientPrincipal = new UsernamePasswordAuthenticationToken("client-1", "creds");
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(clientPrincipal, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_TYPE);

		when(jwtDecoder.decode(SUBJECT_TOKEN)).thenReturn(validJwt());
		when(registeredClientRepository.findByClientId("client-1")).thenReturn(client);

		assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(request));
	}

	@Test
	void authenticate_unauthenticatedClientPrincipal_throws() {
		RegisteredClient client = exchangeCapableClient();
		// This constructor produces an unauthenticated token (as opposed to the
		// RegisteredClient-based constructor, which marks itself authenticated).
		Authentication clientPrincipal = new OAuth2ClientAuthenticationToken(
				"client-1", ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "secret", null);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(clientPrincipal, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_TYPE);

		when(jwtDecoder.decode(SUBJECT_TOKEN)).thenReturn(validJwt());
		when(registeredClientRepository.findByClientId("client-1")).thenReturn(client);

		assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(request));
	}

	@Test
	void authenticate_clientWithoutTokenExchangeGrant_throwsUnauthorizedClient() {
		RegisteredClient client = RegisteredClient.withId("client-1")
				.clientId("client-1")
				.clientSecret("secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("http://localhost/callback")
				.scope("openid")
				.build();
		Authentication clientPrincipal = authenticatedClientPrincipal(client);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(clientPrincipal, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_TYPE);

		when(jwtDecoder.decode(SUBJECT_TOKEN)).thenReturn(validJwt());
		when(registeredClientRepository.findByClientId("client-1")).thenReturn(client);

		OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
				() -> provider.authenticate(request));
		assertEquals("unauthorized_client", ex.getError().getErrorCode());
	}

	@Test
	void authenticate_tokenGeneratorReturnsNull_throwsServerError() {
		RegisteredClient client = exchangeCapableClient();
		Authentication clientPrincipal = authenticatedClientPrincipal(client);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(clientPrincipal, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_TYPE);

		when(jwtDecoder.decode(SUBJECT_TOKEN)).thenReturn(validJwt());
		when(registeredClientRepository.findByClientId("client-1")).thenReturn(client);
		when(tokenGenerator.generate(any())).thenReturn(null);

		OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
				() -> provider.authenticate(request));
		assertEquals("server_error", ex.getError().getErrorCode());
	}

	@Test
	void authenticate_success_withOAuth2AccessTokenFromGenerator() {
		RegisteredClient client = exchangeCapableClient();
		Authentication clientPrincipal = authenticatedClientPrincipal(client);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(clientPrincipal, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_TYPE);

		when(jwtDecoder.decode(SUBJECT_TOKEN)).thenReturn(validJwt());
		when(registeredClientRepository.findByClientId("client-1")).thenReturn(client);

		OAuth2AccessToken generatedToken = new OAuth2AccessToken(
				OAuth2AccessToken.TokenType.BEARER, "generated-token", Instant.now(), Instant.now().plusSeconds(300));
		when(tokenGenerator.generate(any())).thenReturn(generatedToken);

		Authentication result = provider.authenticate(request);

		assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, result);
		OAuth2AccessTokenAuthenticationToken accessTokenAuth = (OAuth2AccessTokenAuthenticationToken) result;
		assertEquals("generated-token", accessTokenAuth.getAccessToken().getTokenValue());
		verify(authorizationService).save(any(OAuth2Authorization.class));
	}

	@Test
	void authenticate_tokenGeneratorReturnsUnsupportedTokenType_throwsIllegalArgument() {
		RegisteredClient client = exchangeCapableClient();
		Authentication clientPrincipal = authenticatedClientPrincipal(client);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(clientPrincipal, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_TYPE);

		when(jwtDecoder.decode(SUBJECT_TOKEN)).thenReturn(validJwt());
		when(registeredClientRepository.findByClientId("client-1")).thenReturn(client);

		org.springframework.security.oauth2.core.OAuth2Token unsupportedToken =
				mock(org.springframework.security.oauth2.core.OAuth2Token.class);
		when(tokenGenerator.generate(any())).thenReturn(unsupportedToken);

		assertThrows(IllegalArgumentException.class, () -> provider.authenticate(request));
	}

	@Test
	void authenticate_success_withJwtFromGenerator() {
		RegisteredClient client = exchangeCapableClient();
		Authentication clientPrincipal = authenticatedClientPrincipal(client);
		OAuth2TokenExchangeAuthenticationToken request = tokenExchangeRequest(clientPrincipal, ACCESS_TOKEN_TYPE, ACCESS_TOKEN_TYPE);

		when(jwtDecoder.decode(SUBJECT_TOKEN)).thenReturn(validJwt());
		when(registeredClientRepository.findByClientId("client-1")).thenReturn(client);

		Jwt generatedJwt = Jwt.withTokenValue("generated-jwt")
				.header("alg", "RS256")
				.subject("client-1")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.claim("sub", "client-1")
				.build();
		when(tokenGenerator.generate(any())).thenReturn(generatedJwt);

		Authentication result = provider.authenticate(request);

		assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, result);
		OAuth2AccessTokenAuthenticationToken accessTokenAuth = (OAuth2AccessTokenAuthenticationToken) result;
		assertEquals("generated-jwt", accessTokenAuth.getAccessToken().getTokenValue());
		assertEquals(Set.of("openid"), accessTokenAuth.getAccessToken().getScopes());
	}
}
