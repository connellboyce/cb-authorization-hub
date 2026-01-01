package com.connellboyce.authhub.grant;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

public class BasicCredentialsAuthenticationProvider implements AuthenticationProvider {
	private final RegisteredClientRepository registeredClientRepository;
	private final PasswordEncoder passwordEncoder;

	public BasicCredentialsAuthenticationProvider(
			RegisteredClientRepository registeredClientRepository,
			PasswordEncoder passwordEncoder
	) {
		this.registeredClientRepository = registeredClientRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {

		OAuth2ClientAuthenticationToken clientAuth =
				(OAuth2ClientAuthenticationToken) authentication;

		String clientId = (String) authentication.getPrincipal();
		String clientSecret = (String) authentication.getCredentials();

		if (clientSecret == null) {
			return null;
		}

		RegisteredClient client =
				registeredClientRepository.findByClientId(clientId);

		if (client == null) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
		}

		if (!passwordEncoder.matches(
				clientSecret,
				client.getClientSecret()
		)) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
		}

		return new OAuth2ClientAuthenticationToken(
				client,
				clientAuth.getClientAuthenticationMethod(),
				clientSecret
		);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
