package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service("applicationService")
public class ApplicationServiceImpl implements ApplicationService {

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private UserService userService;

	@Override
	public Application createApplication(String name, String description, String ownerId) {
		if (name == null || name.isEmpty() || description == null || description.isEmpty()) {
			return null;
		}
		if (applicationRepository.findByName(name).isPresent()) {
			//TODO throw exceptions instead
			return null;
		}
		if (ownerId != null && !ownerId.isEmpty() && applicationRepository.findByOwnerId(ownerId).isEmpty()) {
			return null;
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
	public boolean validateApplicationOwnership(Authentication authentication, String applicationId) {
		if (authentication == null || applicationId == null || applicationId.isEmpty()) {
			return false;
		}
		CBUser user = userService.getCBUserByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
		Application application = getApplicationById(applicationId);
		if (application == null || user == null) {
			return false;
		}
		if (application.getOwnerId() == null || application.getOwnerId().isEmpty()) {
			return false;
		}
		if (user.getId() == null || user.getId().isEmpty()) {
			return false;
		}

		return application.getOwnerId().equals(user.getId());
	}
}
