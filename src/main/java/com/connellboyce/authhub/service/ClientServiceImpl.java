package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.connellboyce.authhub.repository.MongoRegisteredClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClientServiceImpl implements ClientService {
	@Autowired
	private MongoRegisteredClientRepository repository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Override
	public MongoRegisteredClient createClient(RegisteredClient registeredClient, String ownerId) {
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
		mongoClient.setOwnerId(ownerId);

		return repository.save(mongoClient);
	}

	@Override
	public List<MongoRegisteredClient> getClientsByOwner(String ownerId) {
		return repository.findByOwnerId(ownerId).orElse(List.of());
	}

	@Override
	public MongoRegisteredClient getClientByClientId(String clientId) {
		return repository.findByClientId(clientId).orElse(null);
	}

	@Override
	public void deleteByClientId(String clientId) {
		repository.deleteByClientId(clientId);
	}

	@Override
	public MongoRegisteredClient updateClient(String clientId, List<String> grantTypes, List<String> redirectUris, List<String> scopes) throws Exception {
		MongoRegisteredClient client = repository.findByClientId(clientId).orElse(null);
		if (client == null) {
			throw new Exception("Client not found");
		}
		client.setAuthorizationGrantTypes(new HashSet<>(grantTypes));
		client.setRedirectUris(new HashSet<>(redirectUris));
		client.setScopes(new HashSet<>(scopes));
		return repository.save(client);
	}
}
