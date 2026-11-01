package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.Application;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.List;

public interface ApplicationService {
	Application createApplication(String name, String description, String ownerId) throws IllegalArgumentException;
	Application getApplicationById(String id);
	List<Application> getApplicationsByOwnerId(String ownerId);
	Application updateApplication(String id, String name, String description, String ownerId);
	void deleteApplicationById(String id);
	boolean validateApplicationOwnership(Authentication authentication, String applicationId);
	List<Application> getOwnedApplications(String ownerId);
	List<Application> getApplicationsByIds(Collection<String> ids);
	Application getOwnedApplication(String id, String ownerId);
	Application createOwnedApplication(String name, String description, String ownerId);
	Application updateOwnedApplication(String id, String name, String description, String ownerId);
	void deleteOwnedApplication(String id, String ownerId);
}
