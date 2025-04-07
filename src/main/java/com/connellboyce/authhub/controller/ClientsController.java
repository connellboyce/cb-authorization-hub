package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.payload.request.CreateClientRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/client")
public class ClientsController {
	private final Logger LOGGER = LoggerFactory.getLogger(ClientsController.class);

	@Autowired
	RegisteredClientRepository registeredClientRepository;

	@PostMapping
	@PreAuthorize("hasRole('ROLE_DEVELOPER') or hasAuthority('SCOPE_urn:connellboyce:scope:auth-hub#createClient')")
	public ResponseEntity<?> createClient(@RequestBody CreateClientRequest createClientRequest) {
		RegisteredClient createdClient = createClientRequest.toRegisteredClient();

		LOGGER.debug("Creating client with ID: {}", createdClient.getClientId());
		registeredClientRepository.save(createdClient);
		return ResponseEntity.ok().body(createdClient);
	}
}
