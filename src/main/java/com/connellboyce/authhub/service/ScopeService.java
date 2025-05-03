package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.Scope;

import java.util.List;

public interface ScopeService {
	Scope createScope(String name, String applicationId);
	List<Scope> getScopesByApplicationId(String applicationId);
}
