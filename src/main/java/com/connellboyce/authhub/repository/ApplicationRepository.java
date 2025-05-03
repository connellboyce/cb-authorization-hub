package com.connellboyce.authhub.repository;

import com.connellboyce.authhub.model.dao.Application;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends MongoRepository<Application, String> {
	Optional<Application> findByName(String name);
	Optional<List<Application>> findByOwnerId(String ownerId);
	void deleteApplicationById(String id);
}
