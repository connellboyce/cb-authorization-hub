package com.connellboyce.authhub.service;

import com.connellboyce.authhub.dao.Application;

public interface ApplicationService {
	Application createApplication(String name, String description);
	Application getApplicationById(String id);
}
