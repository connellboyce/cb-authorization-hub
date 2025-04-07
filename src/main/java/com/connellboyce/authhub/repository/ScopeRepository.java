package com.connellboyce.authhub.repository;

import com.connellboyce.authhub.dao.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ScopeRepository extends MongoRepository<Scope, String> {
	Optional<Scope> findByName(String name);
	Optional<List<Scope>> findByApplicationId(String applicationId);
}
