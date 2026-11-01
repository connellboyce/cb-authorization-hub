package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.payload.request.ApplicationRequest;
import com.connellboyce.authhub.model.payload.response.ApplicationListResponse;
import com.connellboyce.authhub.model.payload.response.ApplicationResponse;
import com.connellboyce.authhub.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationApiController {
	@Autowired
	private ApplicationService applicationService;

	@GetMapping
	public ApplicationListResponse listApplications(Authentication authentication) {
		return new ApplicationListResponse(applicationService.getOwnedApplications(authentication.getName()).stream()
				.map(ApplicationResponse::from)
				.toList());
	}

	@PostMapping
	public ResponseEntity<ApplicationResponse> createApplication(@RequestBody ApplicationRequest request, Authentication authentication) {
		Application created = applicationService.createOwnedApplication(request.name(), request.description(), authentication.getName());
		return ResponseEntity
				.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri())
				.body(ApplicationResponse.from(created));
	}

	@GetMapping("/{id}")
	public ApplicationResponse getApplication(@PathVariable("id") String id, Authentication authentication) {
		return ApplicationResponse.from(applicationService.getOwnedApplication(id, authentication.getName()));
	}

	@PutMapping("/{id}")
	public ApplicationResponse updateApplication(@PathVariable("id") String id, @RequestBody ApplicationRequest request, Authentication authentication) {
		return ApplicationResponse.from(applicationService.updateOwnedApplication(id, request.name(), request.description(), authentication.getName()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteApplication(@PathVariable("id") String id, Authentication authentication) {
		applicationService.deleteOwnedApplication(id, authentication.getName());
		return ResponseEntity.noContent().build();
	}
}
