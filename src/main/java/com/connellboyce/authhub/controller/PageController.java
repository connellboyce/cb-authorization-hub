package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.service.ApplicationService;
import com.connellboyce.authhub.service.ClientService;
import com.connellboyce.authhub.service.ScopeService;
import com.connellboyce.authhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

	@Autowired
	private ClientService clientService;

	@Autowired
	private UserService userService;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private ScopeService scopeService;

	@GetMapping("/login")
	public String login() {
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

	@GetMapping("/portal/applications/{id}")
	public String editApplicationPage(@PathVariable("id") String id, Model model, Authentication authentication) {
		model.addAttribute("app", applicationService.getApplicationById(id));
		model.addAttribute("scopes", scopeService.getScopesByApplicationId(id));
		return "portal/edit-application";
	}
}
