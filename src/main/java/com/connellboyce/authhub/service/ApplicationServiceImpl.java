package com.connellboyce.authhub.service;

import com.connellboyce.authhub.exception.ResourceConflictException;
import com.connellboyce.authhub.exception.ResourceNotFoundException;
import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.repository.ApplicationRepository;
import com.connellboyce.authhub.repository.ScopeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service("applicationService")
public class ApplicationServiceImpl implements ApplicationService {

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private ScopeRepository scopeRepository;

	@Autowired
	private AuthUtilService authUtilService;

	@Override
	public Application createApplication(String name, String description, String ownerId) throws IllegalArgumentException {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Application name is required");
		}
		if (description == null || description.isEmpty()) {
			throw new IllegalArgumentException("Application name is required");
		}
		if (applicationRepository.findByName(name).isPresent()) {
			throw new IllegalArgumentException("Application name " + name + " already exists");
		}
		if (ownerId == null || ownerId.isEmpty()) {
			throw new IllegalArgumentException("Owner ID is required");
		}
		return applicationRepository.save(new Application(String.valueOf(UUID.randomUUID()), name, description, ownerId));
	}

	@Override
	public Application getApplicationById(String id) {
		return applicationRepository.findById(id).orElse(null);
	}

	@Override
	public List<Application> getApplicationsByOwnerId(String ownerId) {
		return applicationRepository.findByOwnerId(ownerId).orElse(null);
	}

	@Override
	public Application updateApplication(String id, String name, String description, String ownerId) {
		//TODO validate inputs
		//TODO ensure no duplicate names
		return applicationRepository.save(new Application(id, name, description, ownerId));
	}

	@Override
	public void deleteApplicationById(String id) {
		applicationRepository.deleteApplicationById(id);
	}

	@Override
	public List<Application> getOwnedApplications(String ownerId) {
		return applicationRepository.findByOwnerId(ownerId).orElse(List.of());
	}

	@Override
	public List<Application> getApplicationsByIds(Collection<String> ids) {
		return applicationRepository.findAllById(ids);
	}

	@Override
	public Application getOwnedApplication(String id, String ownerId) {
		return applicationRepository.findByIdAndOwnerId(id, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Application not found"));
	}

	@Override
	public Application createOwnedApplication(String name, String description, String ownerId) {
		validateApplicationFields(name, description);
		if (applicationRepository.findByName(name).isPresent()) {
			throw new ResourceConflictException("Application name '" + name + "' already exists");
		}
		return applicationRepository.save(new Application(String.valueOf(UUID.randomUUID()), name, description, ownerId));
	}

	@Override
	public Application updateOwnedApplication(String id, String name, String description, String ownerId) {
		Application application = getOwnedApplication(id, ownerId);
		validateApplicationFields(name, description);
		applicationRepository.findByName(name)
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new ResourceConflictException("Application name '" + name + "' already exists");
				});
		application.setName(name);
		application.setDescription(description);
		return applicationRepository.save(application);
	}

	@Override
	public void deleteOwnedApplication(String id, String ownerId) {
		Application application = getOwnedApplication(id, ownerId);
		if (scopeRepository.existsByApplicationId(application.getId())) {
			throw new ResourceConflictException("Application has scopes and cannot be deleted");
		}
		applicationRepository.deleteById(application.getId());
	}

	private void validateApplicationFields(String name, String description) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Application name is required");
		}
		if (description == null || description.isBlank()) {
			throw new IllegalArgumentException("Application description is required");
		}
	}

	@Override
	public boolean validateApplicationOwnership(Authentication authentication, String applicationId) {
		if (authentication == null || applicationId == null || applicationId.isEmpty()) {
			return false;
		}
		Optional<String> userId = authUtilService.getUserIdFromAuthentication(authentication);
		Application application = getApplicationById(applicationId);
		if (application == null || userId.isEmpty()) {
			return false;
		}
		if (application.getOwnerId() == null || application.getOwnerId().isEmpty()) {
			return false;
		}

		return application.getOwnerId().equals(userId.get());
	}
}
