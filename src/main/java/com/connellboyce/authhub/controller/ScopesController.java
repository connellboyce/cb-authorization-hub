package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.service.ScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/portal/operation/scope")
public class ScopesController {
	@Autowired
	private ScopeService scopeService;

	@PostMapping
	public String createScope(@RequestParam("name") String name, @RequestParam("applicationId") String applicationId, Authentication authentication, RedirectAttributes redirectAttributes) {
		Scope result = scopeService.createScope(
				name,
				applicationId
		);

		if (result != null) {
			redirectAttributes.addFlashAttribute("success", "Scope created successfully!");
		} else {
			redirectAttributes.addFlashAttribute("error", "Scope creation failed");
		}

		return "redirect:/portal/applications/" + applicationId;
	}
}
