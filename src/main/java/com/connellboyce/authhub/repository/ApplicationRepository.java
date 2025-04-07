package com.connellboyce.authhub.repository;

import com.connellboyce.authhub.dao.Application;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ApplicationRepository extends MongoRepository<Application, String> {
	Optional<Application> findByName(String name);
	Optional<Application> findByOwnerId(String ownerId);
}
