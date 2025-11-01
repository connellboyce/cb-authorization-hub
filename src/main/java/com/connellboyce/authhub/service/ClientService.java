package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.List;
import java.util.Set;

public interface ClientService {
	MongoRegisteredClient createClient(RegisteredClient registeredClient, String ownerId);
	List<MongoRegisteredClient> getClientsByOwner(String ownerId);
	MongoRegisteredClient getClientByClientId(String clientId);
	void deleteByClientId(String clientId);
	MongoRegisteredClient updateClient(String clientId, List<String> grantTypes, List<String> redirectUris, List<String> scopes) throws Exception;
	boolean validateClientOwnership(Authentication authentication, String clientId);
}
