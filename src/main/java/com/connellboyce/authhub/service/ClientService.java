package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.ClientMetadata;
import com.connellboyce.authhub.model.ClientRegistrationResult;
import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.List;
import java.util.Optional;

public interface ClientService {
	MongoRegisteredClient createClient(RegisteredClient registeredClient, String ownerId);
	List<MongoRegisteredClient> getClientsByOwner(String ownerId);
	MongoRegisteredClient getClientByClientId(String clientId);
	void deleteByClientId(String clientId);
	MongoRegisteredClient updateClient(String clientId, List<String> grantTypes, List<String> redirectUris, List<String> scopes) throws Exception;
	boolean validateClientOwnership(Authentication authentication, String clientId);
	ClientRegistrationResult registerClient(ClientMetadata metadata, String requestedClientId, String ownerId);
	Optional<MongoRegisteredClient> findOwnedClient(String clientId, String ownerId);
	MongoRegisteredClient replaceOwnedClient(String clientId, ClientMetadata metadata, String suppliedClientSecret, String ownerId);
	void deleteOwnedClient(String clientId, String ownerId);
}
