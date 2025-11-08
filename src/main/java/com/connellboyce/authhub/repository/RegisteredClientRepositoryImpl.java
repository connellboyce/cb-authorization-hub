package com.connellboyce.authhub.repository;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;

public class RegisteredClientRepositoryImpl implements RegisteredClientRepository {

	private final MongoRegisteredClientRepository repository;

	public RegisteredClientRepositoryImpl(MongoRegisteredClientRepository repository) {
		this.repository = repository;
	}

	@Override
	public void save(RegisteredClient registeredClient) {
		throw new UnsupportedOperationException("This save method is not supported, as it does not allow for owner ID to be passed into the method. Please use ClientService.createClient() instead.");
	}

	@Override
	public RegisteredClient findById(String id) {
		return repository.findById(id).map(this::toRegisteredClient).orElse(null);
	}

	@Override
	public RegisteredClient findByClientId(String clientId) {
		return repository.findByClientId(clientId).map(this::toRegisteredClient).orElse(null);
	}

	private RegisteredClient toRegisteredClient(MongoRegisteredClient mongoClient) {
		return RegisteredClient.withId(mongoClient.getId())
				.clientId(mongoClient.getClientId())
				.clientSecret(mongoClient.getClientSecret())
				.clientAuthenticationMethods(authMethods ->
						mongoClient.getClientAuthenticationMethods().forEach(authMethod ->
								authMethods.add(new ClientAuthenticationMethod(authMethod))))
				.authorizationGrantTypes(grantTypes ->
						mongoClient.getAuthorizationGrantTypes().forEach(grantType ->
								grantTypes.add(new AuthorizationGrantType(grantType))))
				.redirectUris(uris -> uris.addAll(mongoClient.getRedirectUris()))
				.scopes(scopes -> scopes.addAll(mongoClient.getScopes()))
				.clientSettings(ClientSettings.builder().requireAuthorizationConsent(mongoClient.isRequireAuthorizationConsent()).build())
				.tokenSettings(TokenSettings.builder()
						.accessTokenTimeToLive(Duration.ofHours(1))
						.refreshTokenTimeToLive(Duration.ofDays(30))
						.reuseRefreshTokens(false)
						.build())
				.build();
	}
}
