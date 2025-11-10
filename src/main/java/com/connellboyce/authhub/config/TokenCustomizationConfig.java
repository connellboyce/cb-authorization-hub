package com.connellboyce.authhub.config;

import com.connellboyce.authhub.grant.TokenType;
import com.connellboyce.authhub.model.Actor;
import com.connellboyce.authhub.model.ActorType;
import com.connellboyce.authhub.service.UserService;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenExchangeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration.jwtDecoder;

@Configuration
public class TokenCustomizationConfig {
	@Bean
	OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(UserService userService, JWKSource<SecurityContext> jwkSource) {
		JwtDecoder jwtDecoder = jwtDecoder(jwkSource);

		return context -> {
			Authentication principal = context.getPrincipal();
			if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				Set<String> authorities = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority)
						.collect(Collectors.toSet());
				context.getClaims().claim("role", authorities);
				context.getClaims().claim("scope", context.getAuthorizedScopes());
				if (principal.getPrincipal() instanceof User user) {
					context.getClaims().subject(userService.getCBUserByUsername(principal.getName()).getId());
					context.getClaims().claim("username", user.getUsername());
					context.getClaims().claim("amr", Set.of("pwd"));
				}
				context.getClaims().claim("azp", context.getRegisteredClient().getClientId());
			}
			if (Objects.equals(AuthorizationGrantType.TOKEN_EXCHANGE.getValue(), context.getAuthorizationGrantType().getValue())) {
				OAuth2TokenExchangeAuthenticationToken tokenExchangeGrant = context.getAuthorizationGrant();
				String subjectToken = tokenExchangeGrant.getSubjectToken();
				String subjectTokenType = tokenExchangeGrant.getSubjectTokenType();

				if (TokenType.ACCESS_TOKEN != TokenType.from(subjectTokenType).orElse(null)) {
					throw new OAuth2AuthenticationException(
							new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "subject_token_type not supported", "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2")
					);
				}

				Jwt jwt;
				try {
					jwt = jwtDecoder.decode(subjectToken);
				} catch (JwtException ex) {
					throw new OAuth2AuthenticationException(
							new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Invalid subject_token", "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2"), ex
					);
				}
				Map<String, Object> subjectTokenClaims = jwt.getClaims();

				delegateClaimIfNotNull(subjectTokenClaims, context, "sub");
				delegateClaimIfNotNull(subjectTokenClaims, context, "amr");
				delegateClaimIfNotNull(subjectTokenClaims, context, "username");
				delegateClaimIfNotNull(subjectTokenClaims, context, "role");

				Object existingActorsClaim = subjectTokenClaims.get("act");
				Actor existingActor = null;
				if (existingActorsClaim instanceof String) {
					existingActor = new Actor((String) existingActorsClaim);
				}

				context.getClaims().claim("act",
						new Actor(context.getRegisteredClient().getClientId(),
								ActorType.SERVICE,
								existingActor));
			}
		};
	}

	private void delegateClaimIfNotNull(Map<String, Object> subjectTokenClaims, JwtEncodingContext context, String claimName) {
		Object claimValue = subjectTokenClaims.get(claimName);
		if (claimValue != null) {
			context.getClaims().claim(claimName, claimValue);
		}
	}
}
