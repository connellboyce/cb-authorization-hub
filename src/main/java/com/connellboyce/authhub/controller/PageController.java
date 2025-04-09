package com.connellboyce.authhub.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
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
}
