package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.exception.ResourceNotFoundException;
import com.connellboyce.authhub.model.ClientRegistrationResult;
import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.connellboyce.authhub.model.payload.request.ClientMetadataRequest;
import com.connellboyce.authhub.model.payload.response.ClientInformationResponse;
import com.connellboyce.authhub.model.payload.response.ClientListResponse;
import com.connellboyce.authhub.service.ClientMetadataValidator;
import com.connellboyce.authhub.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientRegistrationController {
	@Autowired
	private ClientService clientService;

	@Autowired
	private ClientMetadataValidator clientMetadataValidator;

	@Autowired
	private AuthorizationServerSettings authorizationServerSettings;

	@PostMapping
	public ResponseEntity<ClientInformationResponse> registerClient(@RequestBody ClientMetadataRequest request, Authentication authentication) {
		ClientRegistrationResult result = clientService.registerClient(clientMetadataValidator.validateRegistration(request), request.clientId(), authentication.getName());
		URI configurationUri = configurationUri(result.client().getClientId());
		return ResponseEntity
				.created(configurationUri)
				.body(ClientInformationResponse.from(result.client(), result.clientSecret(), configurationUri.toString()));
	}

	@GetMapping
	public ClientListResponse listClients(Authentication authentication) {
		return new ClientListResponse(clientService.getClientsByOwner(authentication.getName()).stream()
				.map(this::toResponse)
				.toList());
	}

	@GetMapping("/{clientId}")
	public ClientInformationResponse getClient(@PathVariable("clientId") String clientId, Authentication authentication) {
		return toResponse(clientService.findOwnedClient(clientId, authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("Client not found")));
	}

	@PutMapping("/{clientId}")
	public ClientInformationResponse updateClient(@PathVariable("clientId") String clientId, @RequestBody ClientMetadataRequest request, Authentication authentication) {
		return toResponse(clientService.replaceOwnedClient(clientId, clientMetadataValidator.validateUpdate(clientId, request), request.clientSecret(), authentication.getName()));
	}

	@DeleteMapping("/{clientId}")
	public ResponseEntity<Void> deleteClient(@PathVariable("clientId") String clientId, Authentication authentication) {
		clientService.deleteOwnedClient(clientId, authentication.getName());
		return ResponseEntity.noContent().build();
	}

	private ClientInformationResponse toResponse(MongoRegisteredClient client) {
		return ClientInformationResponse.from(client, null, configurationUri(client.getClientId()).toString());
	}

	private URI configurationUri(String clientId) {
		return UriComponentsBuilder.fromUriString(authorizationServerSettings.getIssuer())
				.path("/api/v1/clients/{clientId}")
				.buildAndExpand(clientId)
				.toUri();
	}
}
