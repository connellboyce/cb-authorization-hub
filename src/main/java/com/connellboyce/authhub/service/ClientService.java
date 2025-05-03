package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.List;

public interface ClientService {
	MongoRegisteredClient createClient(RegisteredClient registeredClient, String ownerId);
	List<MongoRegisteredClient> getClientsByOwner(String ownerId);
}
