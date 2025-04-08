package com.connellboyce.authhub.repository;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MongoRegisteredClientRepository extends MongoRepository<MongoRegisteredClient, String> {
	Optional<MongoRegisteredClient> findByClientId(String clientId);
	Optional<List<MongoRegisteredClient>> findByOwnerId(String ownerId);
}
