package com.connellboyce.authhub.config;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OidcUserInfoConfigTest {

	private final OidcUserInfoConfig config = new OidcUserInfoConfig();

	private RegisteredClient testClient() {
		return RegisteredClient.withId("client-1")
				.clientId("client-1")
				.clientSecret("secret")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("http://localhost/callback")
				.scope("openid")
				.build();
	}

	private OidcUserInfoAuthenticationContext contextFor(Authentication authenticationPrincipal) {
		OidcUserInfoAuthenticationToken oidcToken = new OidcUserInfoAuthenticationToken(authenticationPrincipal);
		OAuth2AccessToken accessToken = new OAuth2AccessToken(
				OAuth2AccessToken.TokenType.BEARER, "token-value", Instant.now(), Instant.now().plusSeconds(300));
		OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(testClient())
				.principalName("alice")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.token(accessToken)
				.build();

		return OidcUserInfoAuthenticationContext.with(oidcToken)
				.accessToken(accessToken)
				.authorization(authorization)
				.build();
	}

	// NOTE: OidcUserInfoConfig's intent for these two branches is to gracefully return
	// an empty OidcUserInfo (e.g. for a client_credentials-issued token with no
	// "username" claim). In reality, `new OidcUserInfo(Map.of())` throws
	// IllegalArgumentException ("claims cannot be empty") -- Spring's OidcUserInfo
	// rejects an empty claims map. Verified end-to-end: calling /userinfo with a
	// client_credentials token currently surfaces as a 401 invalid_token (the
	// exception gets mapped generically by the resource-server layer) rather than the
	// intended empty-body response. Documented as current (broken) behavior.
	@Test
	void oidcUserInfoMapper_nonJwtPrincipal_throwsInsteadOfReturningEmptyUserInfo() {
		UserService userService = mock(UserService.class);
		Function<OidcUserInfoAuthenticationContext, OidcUserInfo> mapper = config.oidcUserInfoMapper(userService);

		Authentication notAJwtAuth = new UsernamePasswordAuthenticationToken("alice", "pass");
		OidcUserInfoAuthenticationContext context = contextFor(notAJwtAuth);

		assertThrows(IllegalArgumentException.class, () -> mapper.apply(context));
	}

	@Test
	void oidcUserInfoMapper_jwtWithoutUsernameClaim_throwsInsteadOfReturningEmptyUserInfo() {
		UserService userService = mock(UserService.class);
		Function<OidcUserInfoAuthenticationContext, OidcUserInfo> mapper = config.oidcUserInfoMapper(userService);

		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.subject("alice")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.claim("sub", "alice")
				.build();
		JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt);
		OidcUserInfoAuthenticationContext context = contextFor(jwtAuth);

		assertThrows(IllegalArgumentException.class, () -> mapper.apply(context));
	}

	@Test
	void oidcUserInfoMapper_jwtWithUsernameClaim_returnsFullUserInfo() {
		UserService userService = mock(UserService.class);
		when(userService.getCBUserByUsername("alice"))
				.thenReturn(new CBUser("user-1", "alice", "pass", Set.of("USER"), "alice@example.com", "Alice", "Smith"));

		Function<OidcUserInfoAuthenticationContext, OidcUserInfo> mapper = config.oidcUserInfoMapper(userService);

		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.subject("alice")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.claim("sub", "alice")
				.claim("username", "alice")
				.build();
		JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt);
		OidcUserInfoAuthenticationContext context = contextFor(jwtAuth);

		OidcUserInfo result = mapper.apply(context);

		assertEquals("user-1", result.getSubject());
		assertEquals("alice", result.getPreferredUsername());
		assertEquals("Alice Smith", result.getFullName());
		assertEquals("alice@example.com", result.getEmail());
		assertEquals("Alice", result.getGivenName());
		assertEquals("Smith", result.getFamilyName());
	}
}
