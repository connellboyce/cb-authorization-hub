package com.connellboyce.authhub.model.payload.response;

import com.connellboyce.authhub.model.dao.Scope;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ScopeResponse(String id, String name, @JsonProperty("application_id") String applicationId) {
	public static ScopeResponse from(Scope scope) {
		return new ScopeResponse(scope.getId(), scope.getName(), scope.getApplicationId());
	}
}
