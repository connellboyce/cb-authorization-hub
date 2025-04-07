package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.Scope;

public interface ScopeService {
	Scope createScope(String name, String applicationId);
}
