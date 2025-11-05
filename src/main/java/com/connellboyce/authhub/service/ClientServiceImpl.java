package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.connellboyce.authhub.repository.MongoRegisteredClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service("clientService")
public class ClientServiceImpl implements ClientService {

	@Autowired
	private MongoRegisteredClientRepository repository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserService userService;

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

	@Override
	public boolean validateClientOwnership(Authentication authentication, String clientId) {
		if (authentication == null || clientId == null || clientId.isEmpty()) {
			return false;
		}
		CBUser user = userService.getCBUserByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
		MongoRegisteredClient client = getClientByClientId(clientId);
		if (client == null || user == null) {
			return false;
		}
		if (client.getOwnerId() == null || client.getOwnerId().isEmpty()) {
			return false;
		}
		if (user.getId() == null || user.getId().isEmpty()) {
			return false;
		}

		return client.getOwnerId().equals(user.getId());
	}
}
