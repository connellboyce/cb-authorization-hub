package com.connellboyce.authhub.service;

import com.connellboyce.authhub.dao.Application;
import com.connellboyce.authhub.dao.Scope;
import com.connellboyce.authhub.repository.ScopeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScopeServiceImpl implements ScopeService {
	@Autowired
	private ScopeRepository scopeRepository;

	@Autowired
	private ApplicationService applicationService;

	@Override
	public Scope createScope(String name, String applicationId) {
		if (name == null || name.isEmpty() || applicationId == null || applicationId.isEmpty()) {
			return null;
		}
		Application parentApplication = applicationService.getApplicationById(applicationId);
		if (parentApplication == null) {
			return null;
		}
		return scopeRepository.save(new Scope(String.valueOf(UUID.randomUUID()), name, applicationId));
	}
}
