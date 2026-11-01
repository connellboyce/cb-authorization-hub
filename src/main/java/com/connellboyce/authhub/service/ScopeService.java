package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.Scope;

import java.util.Collection;
import java.util.List;

public interface ScopeService {
	Scope createScope(String name, String applicationId);
	List<Scope> getScopesByApplicationId(String applicationId);
	List<Scope> getAllScopes();
	List<Scope> getScopesByNames(Collection<String> names);
	List<Scope> getOwnedApplicationScopes(String applicationId, String ownerId);
	Scope createOwnedScope(String applicationId, String name, String ownerId);
	void deleteOwnedScope(String applicationId, String scopeId, String ownerId);
}
