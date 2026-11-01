package com.connellboyce.authhub.service;

import com.connellboyce.authhub.exception.ResourceConflictException;
import com.connellboyce.authhub.exception.ResourceNotFoundException;
import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.repository.MongoRegisteredClientRepository;
import com.connellboyce.authhub.repository.ScopeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class ScopeServiceImpl implements ScopeService {
	@Autowired
	private ScopeRepository scopeRepository;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private MongoRegisteredClientRepository clientRepository;

	@Override
	public Scope createScope(String name, String applicationId) {
		if (name == null || name.isEmpty() || applicationId == null || applicationId.isEmpty()) {
			throw new IllegalArgumentException("Scope name and application ID must be provided.");
		}
		if (scopeRepository.findByName(name).isPresent()) {
			throw new IllegalArgumentException("Name already exists");
		}
		Application parentApplication = applicationService.getApplicationById(applicationId);
		if (parentApplication == null) {
			throw new IllegalArgumentException("Provided application ID does not exist.");
		}
		return scopeRepository.save(new Scope(String.valueOf(UUID.randomUUID()), name, applicationId));
	}

	@Override
	public List<Scope> getScopesByApplicationId(String applicationId) {
		if (applicationId == null || applicationId.isEmpty()) {
			throw new IllegalArgumentException("Application ID must be provided.");
		}
		return scopeRepository.findByApplicationId(applicationId).orElse(null);
	}

	@Override
	public List<Scope> getAllScopes() {
		return scopeRepository.findAll();
	}

	@Override
	public List<Scope> getScopesByNames(Collection<String> names) {
		return scopeRepository.findByNameIn(names);
	}

	@Override
	public List<Scope> getOwnedApplicationScopes(String applicationId, String ownerId) {
		Application application = applicationService.getOwnedApplication(applicationId, ownerId);
		return scopeRepository.findByApplicationId(application.getId()).orElse(List.of());
	}

	@Override
	public Scope createOwnedScope(String applicationId, String name, String ownerId) {
		Application application = applicationService.getOwnedApplication(applicationId, ownerId);
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Scope name is required");
		}
		if (scopeRepository.findByName(name).isPresent()) {
			throw new ResourceConflictException("Scope name '" + name + "' already exists");
		}
		return scopeRepository.save(new Scope(String.valueOf(UUID.randomUUID()), name, application.getId()));
	}

	@Override
	public void deleteOwnedScope(String applicationId, String scopeId, String ownerId) {
		Application application = applicationService.getOwnedApplication(applicationId, ownerId);
		Scope scope = scopeRepository.findByIdAndApplicationId(scopeId, application.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Scope not found"));
		long assignedClients = clientRepository.countByScopesContaining(scope.getName());
		if (assignedClients > 0) {
			throw new ResourceConflictException("Scope is assigned to " + assignedClients + " client(s) and cannot be deleted");
		}
		scopeRepository.deleteById(scope.getId());
	}
}
