package com.connellboyce.authhub.grant;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.connellboyce.authhub.service.ClientService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.authentication.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.token.*;

import java.util.Map;
import java.util.Optional;

public class TokenExchangeAuthenticationProvider implements AuthenticationProvider {
	private final JwtDecoder jwtDecoder;
	private final OAuth2AuthorizationService authorizationService;
	private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
	private final RegisteredClientRepository registeredClientRepository;

	public TokenExchangeAuthenticationProvider(
			JwtDecoder jwtDecoder,
			OAuth2AuthorizationService authorizationService,
			OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
			RegisteredClientRepository registeredClientRepository) {
		this.jwtDecoder = jwtDecoder;
		this.authorizationService = authorizationService;
		this.tokenGenerator = tokenGenerator;
		this.registeredClientRepository = registeredClientRepository;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		OAuth2TokenExchangeAuthenticationToken tokenExchangeAuth =
				(OAuth2TokenExchangeAuthenticationToken) authentication;

		String subjectToken = tokenExchangeAuth.getSubjectToken();
		Optional<TokenType> subjectTokenType = TokenType.from(tokenExchangeAuth.getSubjectTokenType());
		String actorToken = tokenExchangeAuth.getActorToken();
		Optional<TokenType> actorTokenType = TokenType.from(tokenExchangeAuth.getActorTokenType());
		Optional<TokenType> requestedTokenType = TokenType.from(tokenExchangeAuth.getRequestedTokenType());

		if (subjectToken.isEmpty()) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "subject_token is required", "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2")
			);
		}

		if (subjectTokenType.isEmpty()) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "subject_token_type is required", "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2")
			);
		}

		if (subjectTokenType.get() != TokenType.ACCESS_TOKEN) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error(OAuth2ErrorCodes.UNSUPPORTED_TOKEN_TYPE, "subject_token_type is not supported", "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2")
			);
		}

		if (requestedTokenType.isPresent() && requestedTokenType.get() != TokenType.ACCESS_TOKEN) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error(OAuth2ErrorCodes.UNSUPPORTED_TOKEN_TYPE, "requested_token_type is not supported", "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2")
			);
		}

		Jwt jwt;
		try {
			jwt = jwtDecoder.decode(subjectToken);
		} catch (JwtException ex) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Invalid subject_token", null), ex
			);
		}

		Authentication clientPrincipal = (Authentication) tokenExchangeAuth.getPrincipal();
		RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientPrincipal.getName());
		if (registeredClient == null) {
			throw new OAuth2AuthenticationException("Unknown client");
		}
		if (!(clientPrincipal instanceof OAuth2ClientAuthenticationToken clientAuth) || !clientAuth.isAuthenticated()) {
			throw new OAuth2AuthenticationException("Invalid client authentication");
		}

//		OAuth2TokenContext tokenContext =
		DefaultOAuth2TokenContext.Builder builder = DefaultOAuth2TokenContext.builder()
				.authorizationGrantType(new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:token-exchange"))
				.principal(authentication)
				.registeredClient(registeredClient)
				.tokenType(OAuth2TokenType.ACCESS_TOKEN)
				.authorizedScopes(tokenExchangeAuth.getScopes())
				.authorizationGrant(tokenExchangeAuth)
				.authorizationServerContext(AuthorizationServerContextHolder.getContext());
		DefaultOAuth2TokenContext tokenContext = delegateUserLevelClaims(builder, jwt).build();


		OAuth2AccessToken accessToken = toAccessToken(this.tokenGenerator.generate(tokenContext), tokenContext);
		if (accessToken == null) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
		}

		OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
				.principalName(jwt.getSubject())
				.authorizationGrantType(new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:token-exchange"))
				.token(accessToken)
				.build();

		authorizationService.save(authorization);

		return new OAuth2AccessTokenAuthenticationToken(
				registeredClient,
				tokenExchangeAuth,
				accessToken
		);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return OAuth2TokenExchangeAuthenticationToken.class.isAssignableFrom(authentication);
	}

	private OAuth2AccessToken toAccessToken(OAuth2Token token, OAuth2TokenContext tokenContext) {
		if (token instanceof OAuth2AccessToken accessToken) {
			return accessToken;
		}

		if (token instanceof Jwt jwt) {
			return new OAuth2AccessToken(
					OAuth2AccessToken.TokenType.BEARER,
					jwt.getTokenValue(),
					jwt.getIssuedAt(),
					jwt.getExpiresAt(),
					tokenContext.getAuthorizedScopes()
			);
		}

		throw new IllegalArgumentException("Unsupported token type: " + token.getClass().getName());
	}

	public DefaultOAuth2TokenContext.Builder delegateUserLevelClaims(DefaultOAuth2TokenContext.Builder builder, OAuth2Token subjectToken) {
		if(!(subjectToken instanceof Jwt jwt)) {
			return builder;
		}
		Map<String, Object> claims = ((Jwt) subjectToken).getClaims();
		claims.remove("nbf");
		claims.remove("iss");
		claims.remove("exp");
		claims.remove("iat");
		claims.remove("jti");

		for (Map.Entry<String, Object> entry : claims.entrySet()) {
			builder.put(entry.getKey(), entry.getValue());
		}

		return builder;
	}
}
