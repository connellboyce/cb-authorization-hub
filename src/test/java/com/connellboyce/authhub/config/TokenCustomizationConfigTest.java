package com.connellboyce.authhub.config;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.service.UserService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenExchangeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.connellboyce.authhub.util.RsaUtils.generateRsaKey;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenCustomizationConfigTest {

	private final TokenCustomizationConfig config = new TokenCustomizationConfig();

	private JWKSource<SecurityContext> buildJwkSource() {
		KeyPair keyPair = generateRsaKey();
		RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
				.privateKey((RSAPrivateKey) keyPair.getPrivate())
				.keyID(UUID.randomUUID().toString())
				.build();
		return new ImmutableJWKSet<>(new JWKSet(rsaKey));
	}

	private String mintJwt(JWKSource<SecurityContext> jwkSource, JwtClaimsSet claims) {
		JwtEncoder encoder = new NimbusJwtEncoder(jwkSource);
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private JwtEncodingContext contextFor(OAuth2TokenType tokenType, Authentication principal,
			AuthorizationGrantType grantType, Authentication authorizationGrant, RegisteredClient client) {
		JwtEncodingContext.Builder builder = JwtEncodingContext.with(
				JwsHeader.with(SignatureAlgorithm.RS256), JwtClaimsSet.builder());
		builder.tokenType(tokenType)
				.principal(principal)
				.authorizationGrantType(grantType)
				.registeredClient(client)
				.authorizedScopes(Set.of("openid"));
		if (authorizationGrant != null) {
			builder.authorizationGrant(authorizationGrant);
		}
		return builder.build();
	}

	private RegisteredClient testClient() {
		return RegisteredClient.withId("client-1")
				.clientId("client-1")
				.clientSecret("secret")
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("openid")
				.clientSettings(ClientSettings.builder().build())
				.build();
	}

	@Test
	void tokenCustomizer_accessToken_withUserPrincipal_addsUserClaims() {
		UserService userService = mock(UserService.class);
		when(userService.getCBUserByUsername("alice"))
				.thenReturn(new CBUser("user-1", "alice", "pass", Set.of("USER"), "a@b.com", "Alice", "Smith"));

		JWKSource<SecurityContext> jwkSource = buildJwkSource();
		OAuth2TokenCustomizer<JwtEncodingContext> customizer = config.tokenCustomizer(userService, jwkSource);

		User user = new User("alice", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
		Authentication principal = new UsernamePasswordAuthenticationToken(user, "pass", user.getAuthorities());

		JwtEncodingContext context = contextFor(
				OAuth2TokenType.ACCESS_TOKEN, principal, AuthorizationGrantType.CLIENT_CREDENTIALS, null, testClient());

		customizer.customize(context);
		JwtClaimsSet claims = context.getClaims().build();

		assertEquals("user-1", claims.getSubject());
		assertEquals("alice", claims.getClaim("username"));
		assertEquals(Set.of("pwd"), claims.getClaim("amr"));
		assertEquals("client-1", claims.getClaim("azp"));
		assertEquals(Set.of("ROLE_USER"), claims.getClaim("role"));
	}

	@Test
	void tokenCustomizer_accessToken_withNonUserPrincipal_omitsUserClaims() {
		UserService userService = mock(UserService.class);
		JWKSource<SecurityContext> jwkSource = buildJwkSource();
		OAuth2TokenCustomizer<JwtEncodingContext> customizer = config.tokenCustomizer(userService, jwkSource);

		Authentication clientPrincipal = new UsernamePasswordAuthenticationToken("client-1", "secret", List.of());

		JwtEncodingContext context = contextFor(
				OAuth2TokenType.ACCESS_TOKEN, clientPrincipal, AuthorizationGrantType.CLIENT_CREDENTIALS, null, testClient());

		customizer.customize(context);
		JwtClaimsSet claims = context.getClaims().build();

		assertNull(claims.getSubject());
		assertNull(claims.getClaim("username"));
		assertEquals("client-1", claims.getClaim("azp"));
	}

	@Test
	void tokenCustomizer_idToken_addsUserClaims() {
		UserService userService = mock(UserService.class);
		when(userService.getCBUserByUsername("alice"))
				.thenReturn(new CBUser("user-1", "alice", "pass", Set.of("USER"), "a@b.com", "Alice", "Smith"));

		JWKSource<SecurityContext> jwkSource = buildJwkSource();
		OAuth2TokenCustomizer<JwtEncodingContext> customizer = config.tokenCustomizer(userService, jwkSource);

		User user = new User("alice", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
		Authentication principal = new UsernamePasswordAuthenticationToken(user, "pass", user.getAuthorities());

		JwtEncodingContext context = contextFor(
				new OAuth2TokenType(OidcParameterNames.ID_TOKEN), principal, AuthorizationGrantType.AUTHORIZATION_CODE, null, testClient());

		customizer.customize(context);
		JwtClaimsSet claims = context.getClaims().build();

		assertEquals("user-1", claims.getSubject());
		assertEquals("alice", claims.getClaim("username"));
		assertEquals(Set.of("ROLE_USER"), claims.getClaim("role"));
	}

	@Test
	void tokenCustomizer_tokenExchange_delegatesSubjectClaimsAndAddsActorClaim() {
		UserService userService = mock(UserService.class);
		JWKSource<SecurityContext> jwkSource = buildJwkSource();

		JwtClaimsSet subjectClaims = JwtClaimsSet.builder()
				.subject("user-1")
				.issuer("http://localhost")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.claim("username", "alice")
				.claim("role", Set.of("ROLE_USER"))
				.claim("amr", Set.of("pwd"))
				.claim("azp", "original-client")
				.build();
		String subjectToken = mintJwt(jwkSource, subjectClaims);

		OAuth2TokenCustomizer<JwtEncodingContext> customizer = config.tokenCustomizer(userService, jwkSource);

		Authentication clientPrincipal = new UsernamePasswordAuthenticationToken("client-2", "secret", List.of());
		OAuth2TokenExchangeAuthenticationToken tokenExchangeGrant = new OAuth2TokenExchangeAuthenticationToken(
				"urn:ietf:params:oauth:token-type:access_token",
				subjectToken,
				"urn:ietf:params:oauth:token-type:access_token",
				clientPrincipal,
				null, null, null, null, Set.of("openid"), null);

		JwtEncodingContext context = contextFor(
				OAuth2TokenType.ACCESS_TOKEN, clientPrincipal, AuthorizationGrantType.TOKEN_EXCHANGE, tokenExchangeGrant, testClient());

		customizer.customize(context);
		JwtClaimsSet claims = context.getClaims().build();

		assertEquals("user-1", claims.getClaim("sub"));
		assertEquals("alice", claims.getClaim("username"));
		// Claims delegated from a decoded subject JWT come back as Lists (JSON has no
		// Set type), not the original Sets they were minted with.
		assertEquals(List.of("pwd"), claims.getClaim("amr"));
		assertEquals(List.of("ROLE_USER"), claims.getClaim("role"));

		@SuppressWarnings("unchecked")
		var act = (java.util.Map<String, Object>) claims.getClaim("act");
		assertNotNull(act);
		// NOTE: the "act.sub" claim is populated from the *subject token's own* "azp"
		// claim ("original-client" here), not from the currently-authenticated
		// exchanging client ("client-2"). Documented as current behavior -- this may be
		// worth revisiting, since it means the actor claim always echoes the original
		// issuing client rather than identifying whoever is actually performing this
		// exchange when the two differ (e.g. a genuine delegation chain).
		assertEquals("original-client", act.get("sub"));
		assertEquals("service", act.get("typ"));
	}

	@Test
	void tokenCustomizer_tokenExchange_unsupportedSubjectTokenType_throws() {
		UserService userService = mock(UserService.class);
		JWKSource<SecurityContext> jwkSource = buildJwkSource();
		OAuth2TokenCustomizer<JwtEncodingContext> customizer = config.tokenCustomizer(userService, jwkSource);

		Authentication clientPrincipal = new UsernamePasswordAuthenticationToken("client-2", "secret", List.of());
		OAuth2TokenExchangeAuthenticationToken tokenExchangeGrant = new OAuth2TokenExchangeAuthenticationToken(
				"urn:ietf:params:oauth:token-type:access_token",
				"irrelevant-subject-token",
				"unsupported-type",
				clientPrincipal,
				null, null, null, null, Set.of("openid"), null);

		JwtEncodingContext context = contextFor(
				OAuth2TokenType.ACCESS_TOKEN, clientPrincipal, AuthorizationGrantType.TOKEN_EXCHANGE, tokenExchangeGrant, testClient());

		assertThrows(OAuth2AuthenticationException.class, () -> customizer.customize(context));
	}

	@Test
	void tokenCustomizer_tokenExchange_undecodableSubjectToken_throwsInvalidGrant() {
		UserService userService = mock(UserService.class);
		JWKSource<SecurityContext> jwkSource = buildJwkSource();
		OAuth2TokenCustomizer<JwtEncodingContext> customizer = config.tokenCustomizer(userService, jwkSource);

		Authentication clientPrincipal = new UsernamePasswordAuthenticationToken("client-2", "secret", List.of());
		OAuth2TokenExchangeAuthenticationToken tokenExchangeGrant = new OAuth2TokenExchangeAuthenticationToken(
				"urn:ietf:params:oauth:token-type:access_token",
				"not-a-real-jwt",
				"urn:ietf:params:oauth:token-type:access_token",
				clientPrincipal,
				null, null, null, null, Set.of("openid"), null);

		JwtEncodingContext context = contextFor(
				OAuth2TokenType.ACCESS_TOKEN, clientPrincipal, AuthorizationGrantType.TOKEN_EXCHANGE, tokenExchangeGrant, testClient());

		OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
				() -> customizer.customize(context));
		assertEquals("invalid_grant", ex.getError().getErrorCode());
	}
}
