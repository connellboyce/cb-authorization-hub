package com.connellboyce.authhub.repository;

import com.connellboyce.authhub.model.dao.CBUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<CBUser, String> {
	Optional<CBUser> findByUsername(String username);
	Optional<CBUser> findByEmail(String email);

}
