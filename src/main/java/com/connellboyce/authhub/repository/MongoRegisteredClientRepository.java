package com.connellboyce.authhub.repository;

import com.connellboyce.authhub.dao.MongoRegisteredClient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MongoRegisteredClientRepository extends MongoRepository<MongoRegisteredClient, String> {
	Optional<MongoRegisteredClient> findByClientId(String clientId);
}
