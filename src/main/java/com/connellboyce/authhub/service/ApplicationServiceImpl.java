package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ApplicationServiceImpl implements ApplicationService {

	@Autowired
	private ApplicationRepository applicationRepository;

	@Override
	public Application createApplication(String name, String description) {
		if (name == null || name.isEmpty() || description == null || description.isEmpty()) {
			return null;
		}
		if (applicationRepository.findByName(name).isPresent()) {
			return null;
		}
		return applicationRepository.save(new Application(String.valueOf(UUID.randomUUID()), name, description, null));
	}

	@Override
	public Application getApplicationById(String id) {
		return applicationRepository.findById(id).orElse(null);
	}
}
