package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.service.ApplicationService;
import com.connellboyce.authhub.service.AuthUtilService;
import com.connellboyce.authhub.service.ScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/portal/operation/application")
public class ApplicationsController {
	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private AuthUtilService authUtilService;

	@PostMapping
	public String createApplication(@RequestParam("applicationName") String name, @RequestParam("description") String description, Authentication authentication, RedirectAttributes redirectAttributes) {
		Optional<String> userId = authUtilService.getUserIdFromAuthentication(authentication);
		if (userId.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "User not authenticated");
			return "redirect:/portal/applications";
		}

		Application result = applicationService.createApplication(name, description, userId.get());
		if (result != null) {
			redirectAttributes.addFlashAttribute("success", "Application created successfully!");
		} else {
			redirectAttributes.addFlashAttribute("error", "Application creation failed");
		}
		return "redirect:/portal/applications";
	}

	@PutMapping
	public String updateApplication(@RequestParam("id") String id, @RequestParam("applicationName") String name, @RequestParam("description") String description, Authentication authentication, RedirectAttributes redirectAttributes) {
		Optional<String> userId = authUtilService.getUserIdFromAuthentication(authentication);
		if (userId.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "User not authenticated");
			return "redirect:/portal/applications/" + id;
		}

		Application result = applicationService.updateApplication(id, name, description, userId.get());
		if (result != null) {
			redirectAttributes.addFlashAttribute("success", "Application updated successfully!");
		} else {
			redirectAttributes.addFlashAttribute("error", "Application update failed");
		}
		return "redirect:/portal/applications/" + id;
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@applicationService.validateApplicationOwnership(authentication, #id)")
	public String deleteApplication(@PathVariable("id") String id, Authentication authentication, RedirectAttributes redirectAttributes) {
		applicationService.deleteApplicationById(id);
		redirectAttributes.addFlashAttribute("success", "Application deleted successfully!");
		return "redirect:/portal/applications";
	}
}
