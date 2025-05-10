package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.payload.request.CreateClientRequest;
import com.connellboyce.authhub.service.AuthUtilService;
import com.connellboyce.authhub.service.ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

import static com.connellboyce.authhub.model.payload.request.CreateClientRequest.toRegisteredClient;

@Controller
@RequestMapping("/portal/operation/client")
public class ClientsController {
	private final Logger LOGGER = LoggerFactory.getLogger(ClientsController.class);

	@Autowired
	ClientService clientService;

	@Autowired
	AuthUtilService authUtilService;

	@PostMapping
	public String createClient(@RequestParam("clientId") String clientId, @RequestParam("clientSecret") String clientSecret, @RequestParam("grantTypes") List<String> grantTypes, @RequestParam("redirectUrls") List<String> redirectUrls, @RequestParam("scopes") List<String> scopes, Authentication authentication, RedirectAttributes redirectAttributes) {
		Optional<String> userId = authUtilService.getUserIdFromAuthentication(authentication);
		if (userId.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "User not authenticated");
			return "redirect:/portal/clients";
		}

		RegisteredClient createdClient = toRegisteredClient(clientId, clientSecret, redirectUrls, scopes, grantTypes);

		LOGGER.debug("Creating client with ID: {}", createdClient.getClientId());
		clientService.createClient(createdClient, userId.get());

		redirectAttributes.addFlashAttribute("success", "Client created successfully!");
		return "redirect:/portal/clients";
	}

	@DeleteMapping("/{clientId}")
	public String deleteClient(@PathVariable("clientId") String clientId, Authentication authentication, RedirectAttributes redirectAttributes) {
		clientService.deleteByClientId(clientId);
		redirectAttributes.addFlashAttribute("success", "Client deleted successfully!");
		return "redirect:/portal/clients";
	}
}
