package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.payload.request.CreateApplicationRequest;
import com.connellboyce.authhub.service.ApplicationService;
import com.connellboyce.authhub.service.AuthUtilService;
import com.connellboyce.authhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/application")
public class ApplicationsController {
	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private AuthUtilService authUtilService;

	@PostMapping
	public ResponseEntity<?> createApplication(@RequestBody CreateApplicationRequest createApplicationRequest, Authentication authentication) {
		Optional<String> userId = authUtilService.getUserIdFromAuthentication(authentication);
		if (userId.isEmpty()) {
			return ResponseEntity.status(401).body("User level authentication required");
		}

		Application result = applicationService.createApplication(
				createApplicationRequest.getName(),
				createApplicationRequest.getDescription(),
				userId.get()
		);
		return result == null ? ResponseEntity.badRequest().build() : ResponseEntity.ok(result);
	}
}
