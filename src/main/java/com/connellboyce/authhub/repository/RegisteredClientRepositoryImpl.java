package com.connellboyce.authhub.repository;

import com.connellboyce.authhub.dao.MongoRegisteredClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;


import java.util.stream.Collectors;

public class RegisteredClientRepositoryImpl implements RegisteredClientRepository {

	private final MongoRegisteredClientRepository repository;
	private final PasswordEncoder passwordEncoder;

	public RegisteredClientRepositoryImpl(MongoRegisteredClientRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void save(RegisteredClient registeredClient) {
		MongoRegisteredClient mongoClient = new MongoRegisteredClient();
		mongoClient.setId(registeredClient.getId());
		mongoClient.setClientId(registeredClient.getClientId());
		mongoClient.setClientSecret(passwordEncoder.encode(registeredClient.getClientSecret()));
		mongoClient.setClientAuthenticationMethods(
				registeredClient.getClientAuthenticationMethods().stream().map(ClientAuthenticationMethod::getValue).collect(Collectors.toSet())
		);
		mongoClient.setAuthorizationGrantTypes(
				registeredClient.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::getValue).collect(Collectors.toSet())
		);
		mongoClient.setRedirectUris(registeredClient.getRedirectUris());
		mongoClient.setScopes(registeredClient.getScopes());
		mongoClient.setRequireAuthorizationConsent(registeredClient.getClientSettings().isRequireAuthorizationConsent());

		repository.save(mongoClient);
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
				.build();
	}
}
