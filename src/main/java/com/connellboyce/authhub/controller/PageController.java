package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.service.ApplicationService;
import com.connellboyce.authhub.service.ClientService;
import com.connellboyce.authhub.service.ScopeService;
import com.connellboyce.authhub.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Controller
public class PageController {

	@Value("#{'${spring.security.login.entry-point.preserved-params}'.split(',')}")
	private Set<String> preservedParams;

	@Autowired
	private ClientService clientService;

	@Autowired
	private UserService userService;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private ScopeService scopeService;

	@GetMapping("/login")
	public String login(HttpSession session, Model model) {
		preservedParams.forEach(param -> {
			Object value = session.getAttribute("auth_param_" + param);
			if (value != null) {
				model.addAttribute(param, value);
			}
		});
		if (model.getAttribute("client_id") != null) {
			model.addAttribute("client", clientService.getClientByClientId(Objects.requireNonNull(model.getAttribute("client_id")).toString()));
		}

		return "login";
	}

	@GetMapping("/register")
	public String register() {
		return "register";
	}

	@GetMapping("/portal/index")
	public String portalHomePage(Model model, Authentication authentication) {
		model.addAttribute("name", authentication.getName());
		return "portal/index";
	}

	@GetMapping("/portal/clients")
	public String portalClientsPage(Model model, Authentication authentication) {
		model.addAttribute("clients", clientService.getClientsByOwner(userService.getCBUserByUsername(((UserDetails) authentication.getPrincipal()).getUsername()).getId()));

		return "portal/clients";
	}

	@GetMapping("/portal/applications")
	public String portalApplicationsPage(Model model, Authentication authentication) {
		model.addAttribute("applications", applicationService.getApplicationsByOwnerId(userService.getCBUserByUsername(((UserDetails) authentication.getPrincipal()).getUsername()).getId()));

		return "portal/applications";
	}

	@GetMapping("/portal/applications/create")
	public String createApplicationPage() {
		return "portal/create-application";
	}

	@GetMapping("/portal/clients/create")
	public String createClientPage(Model model) {
		model.addAttribute("generatedSecret", generateSecret());
		model.addAttribute("grantTypes", Map.ofEntries(
				Map.entry("authorization_code", "Authorization Code"),
				Map.entry("client_credentials", "Client Credentials"),
				Map.entry("refresh_token", "Refresh Token")
		));
		Map<String, List<Scope>> scopeMap = new HashMap<>();
		scopeService.getAllScopes().forEach(scope -> {
			String appName = applicationService.getApplicationById(scope.getApplicationId()).getName();
			if(scopeMap.containsKey(appName)) {
				List<Scope> scopeList = scopeMap.get(appName);
				scopeList.add(scope);
			} else {
				scopeMap.put(appName, new ArrayList<>(List.of(scope)));
			}
		});
		model.addAttribute("scopesByApplication", scopeMap);
		return "portal/create-client";
	}

	@GetMapping("/portal/applications/{id}")
	public String editApplicationPage(@PathVariable("id") String id, Model model, Authentication authentication) {
		//TODO: Check if the user is the owner of the application
		model.addAttribute("app", applicationService.getApplicationById(id));
		model.addAttribute("scopes", scopeService.getScopesByApplicationId(id));
		return "portal/edit-application";
	}

	@GetMapping("/portal/clients/{clientId}")
	public String editClientPage(@PathVariable("clientId") String clientId, Model model, Authentication authentication) {
		//TODO: Check if the user is the owner of the client
		model.addAttribute("client", clientService.getClientByClientId(clientId));
		model.addAttribute("grantTypes", Map.ofEntries(
				Map.entry("authorization_code", "Authorization Code"),
				Map.entry("client_credentials", "Client Credentials"),
				Map.entry("refresh_token", "Refresh Token")
		));
		Map<String, List<Scope>> scopeMap = new HashMap<>();
		scopeService.getAllScopes().forEach(scope -> {
			String appName = applicationService.getApplicationById(scope.getApplicationId()).getName();
			if(scopeMap.containsKey(appName)) {
				List<Scope> scopeList = scopeMap.get(appName);
				scopeList.add(scope);
			} else {
				scopeMap.put(appName, new ArrayList<>(List.of(scope)));
			}
		});
		model.addAttribute("scopesByApplication", scopeMap);

		return "portal/edit-client";
	}

	private String generateSecret() {
		try {
			SecureRandom random = SecureRandom.getInstanceStrong();
			byte[] values = new byte[32];
			random.nextBytes(values);
			return Base64.getEncoder().encodeToString(values);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Failed to generate secure random value:", e);
		}
	}
}
