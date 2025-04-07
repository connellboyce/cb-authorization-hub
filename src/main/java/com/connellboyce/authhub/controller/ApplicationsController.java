package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.payload.request.CreateApplicationRequest;
import com.connellboyce.authhub.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/application")
public class ApplicationsController {
	@Autowired
	private ApplicationService applicationService;

	@PostMapping
	public ResponseEntity<?> createApplication(@RequestBody CreateApplicationRequest createApplicationRequest) {
		Application result = applicationService.createApplication(
				createApplicationRequest.getName(),
				createApplicationRequest.getDescription()
		);
		return result == null ? ResponseEntity.badRequest().build() : ResponseEntity.ok(result);
	}
}
