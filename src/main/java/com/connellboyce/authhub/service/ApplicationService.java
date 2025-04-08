package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.Application;
import java.util.List;

public interface ApplicationService {
	Application createApplication(String name, String description, String ownerId);
	Application getApplicationById(String id);
	List<Application> getApplicationsByOwnerId(String ownerId);

}
