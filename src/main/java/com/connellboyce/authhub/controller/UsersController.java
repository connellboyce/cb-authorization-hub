package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.payload.request.CreateUserRequest;
import com.connellboyce.authhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UsersController {
	@Autowired
	private UserService userService;

	@PostMapping
	public ResponseEntity<?> createUser(@RequestBody CreateUserRequest createUserRequest) {
		UserDetails result = userService.createUser(
				createUserRequest.getUsername(),
				createUserRequest.getPassword(),
				createUserRequest.getEmail(),
				createUserRequest.getFirstName(),
				createUserRequest.getLastName()
		);
		return result == null ? ResponseEntity.badRequest().body("User creation failed") : ResponseEntity.ok(result);
	}
}
