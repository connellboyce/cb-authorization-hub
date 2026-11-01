package com.connellboyce.authhub.service;

import com.connellboyce.authhub.exception.InvalidClientMetadataException;
import com.connellboyce.authhub.exception.ResourceNotFoundException;
import com.connellboyce.authhub.model.ClientMetadata;
import com.connellboyce.authhub.model.ClientRegistrationResult;
import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.connellboyce.authhub.repository.MongoRegisteredClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.connellboyce.authhub.exception.InvalidClientMetadataException.INVALID_CLIENT_METADATA;
import static com.connellboyce.authhub.util.CredentialUtils.generateClientId;
import static com.connellboyce.authhub.util.CredentialUtils.generateClientSecret;

@Service("clientService")
public class ClientServiceImpl implements ClientService {

	@Autowired
	private MongoRegisteredClientRepository repository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuthUtilService authUtilService;

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
		Optional<String> userId = authUtilService.getUserIdFromAuthentication(authentication);
		MongoRegisteredClient client = getClientByClientId(clientId);
		if (client == null || userId.isEmpty() || userId.get().isEmpty()) {
			return false;
		}
		if (client.getOwnerId() == null || client.getOwnerId().isEmpty()) {
			return false;
		}

		return client.getOwnerId().equals(userId.get());
	}

	@Override
	public ClientRegistrationResult registerClient(ClientMetadata metadata, String requestedClientId, String ownerId) {
		String clientId = requestedClientId == null ? generateAvailableClientId() : requestedClientId;
		if (requestedClientId != null && repository.existsByClientId(clientId)) {
			throw new InvalidClientMetadataException(INVALID_CLIENT_METADATA, "client_id is not available");
		}
		String clientSecret = generateClientSecret();

		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setId(String.valueOf(UUID.randomUUID()));
		client.setClientId(clientId);
		client.setClientSecret(passwordEncoder.encode(clientSecret));
		client.setClientIdIssuedAt(Instant.now());
		client.setOwnerId(ownerId);
		applyMetadata(client, metadata);

		return new ClientRegistrationResult(repository.save(client), clientSecret);
	}

	@Override
	public Optional<MongoRegisteredClient> findOwnedClient(String clientId, String ownerId) {
		return repository.findByClientIdAndOwnerId(clientId, ownerId);
	}

	@Override
	public MongoRegisteredClient replaceOwnedClient(String clientId, ClientMetadata metadata, String suppliedClientSecret, String ownerId) {
		MongoRegisteredClient client = findOwnedClient(clientId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Client not found"));
		if (suppliedClientSecret != null && !passwordEncoder.matches(suppliedClientSecret, client.getClientSecret())) {
			throw new InvalidClientMetadataException(INVALID_CLIENT_METADATA, "client_secret does not match the current client secret");
		}
		applyMetadata(client, metadata);
		return repository.save(client);
	}

	@Override
	public void deleteOwnedClient(String clientId, String ownerId) {
		MongoRegisteredClient client = findOwnedClient(clientId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Client not found"));
		repository.deleteByClientId(client.getClientId());
	}

	private String generateAvailableClientId() {
		String clientId;
		do {
			clientId = generateClientId();
		} while (repository.existsByClientId(clientId));
		return clientId;
	}

	private void applyMetadata(MongoRegisteredClient client, ClientMetadata metadata) {
		client.setClientName(metadata.clientName());
		client.setRedirectUris(metadata.redirectUris());
		client.setAuthorizationGrantTypes(metadata.grantTypes());
		client.setClientAuthenticationMethods(Set.of(metadata.tokenEndpointAuthMethod()));
		client.setScopes(metadata.scopes());
	}
}
