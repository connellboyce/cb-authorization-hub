package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.model.payload.request.ScopeRequest;
import com.connellboyce.authhub.model.payload.response.ScopeCatalogResponse;
import com.connellboyce.authhub.model.payload.response.ScopeListResponse;
import com.connellboyce.authhub.model.payload.response.ScopeResponse;
import com.connellboyce.authhub.service.ApplicationService;
import com.connellboyce.authhub.service.ScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class ScopeApiController {
	@Autowired
	private ScopeService scopeService;

	@Autowired
	private ApplicationService applicationService;

	@GetMapping("/applications/{id}/scopes")
	public ScopeListResponse listApplicationScopes(@PathVariable("id") String applicationId, Authentication authentication) {
		return new ScopeListResponse(scopeService.getOwnedApplicationScopes(applicationId, authentication.getName()).stream()
				.map(ScopeResponse::from)
				.toList());
	}

	@PostMapping("/applications/{id}/scopes")
	public ResponseEntity<ScopeResponse> createScope(@PathVariable("id") String applicationId, @RequestBody ScopeRequest request, Authentication authentication) {
		Scope created = scopeService.createOwnedScope(applicationId, request.name(), authentication.getName());
		return ResponseEntity
				.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{scopeId}").buildAndExpand(created.getId()).toUri())
				.body(ScopeResponse.from(created));
	}

	@DeleteMapping("/applications/{id}/scopes/{scopeId}")
	public ResponseEntity<Void> deleteScope(@PathVariable("id") String applicationId, @PathVariable("scopeId") String scopeId, Authentication authentication) {
		scopeService.deleteOwnedScope(applicationId, scopeId, authentication.getName());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/scopes")
	public ScopeCatalogResponse getScopeCatalog() {
		List<Scope> scopes = scopeService.getAllScopes();
		Map<String, Application> applicationsById = applicationService.getApplicationsByIds(scopes.stream().map(Scope::getApplicationId).collect(Collectors.toSet()))
				.stream()
				.collect(Collectors.toMap(Application::getId, Function.identity()));

		return new ScopeCatalogResponse(scopes.stream()
				.filter(scope -> applicationsById.containsKey(scope.getApplicationId()))
				.map(scope -> {
					Application application = applicationsById.get(scope.getApplicationId());
					return new ScopeCatalogResponse.Entry(scope.getName(), new ScopeCatalogResponse.ApplicationSummary(application.getId(), application.getName()));
				})
				.toList());
	}
}
