package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.dao.Application;
import com.connellboyce.authhub.dao.Scope;
import com.connellboyce.authhub.payload.request.CreateApplicationRequest;
import com.connellboyce.authhub.payload.request.CreateScopeRequest;
import com.connellboyce.authhub.service.ApplicationService;
import com.connellboyce.authhub.service.ScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scope")
public class ScopesController {
	@Autowired
	private ScopeService scopeService;

	@PostMapping
	public ResponseEntity<?> createScope(@RequestBody CreateScopeRequest createScopeRequest) {
		Scope result = scopeService.createScope(
				createScopeRequest.getName(),
				createScopeRequest.getApplicationId()
		);

		return result == null ? ResponseEntity.badRequest().build() : ResponseEntity.ok(result);
	}
}
