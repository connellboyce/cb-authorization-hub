package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.payload.request.CreateClientRequest;
import com.connellboyce.authhub.service.AuthUtilService;
import com.connellboyce.authhub.service.ClientService;
import com.connellboyce.authhub.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/client")
public class ClientsController {
	private final Logger LOGGER = LoggerFactory.getLogger(ClientsController.class);

	@Autowired
	ClientService clientService;

	@Autowired
	AuthUtilService authUtilService;

	@PostMapping
	@PreAuthorize("hasRole('ROLE_DEVELOPER') or hasAuthority('SCOPE_urn:connellboyce:scope:auth-hub#createClient')")
	public ResponseEntity<?> createClient(@RequestBody CreateClientRequest createClientRequest, Authentication authentication) {
		Optional<String> userId = authUtilService.getUserIdFromAuthentication(authentication);
		if (userId.isEmpty()) {
			return ResponseEntity.status(401).body("User level authentication required");
		}

		RegisteredClient createdClient = createClientRequest.toRegisteredClient();

		LOGGER.debug("Creating client with ID: {}", createdClient.getClientId());
		clientService.createClient(createdClient, userId.get());
		return ResponseEntity.ok().body(createdClient);
	}
}
