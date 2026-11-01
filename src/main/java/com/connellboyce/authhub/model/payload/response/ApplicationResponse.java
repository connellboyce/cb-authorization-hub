package com.connellboyce.authhub.model.payload.response;

import com.connellboyce.authhub.model.dao.Application;

public record ApplicationResponse(String id, String name, String description) {
	public static ApplicationResponse from(Application application) {
		return new ApplicationResponse(application.getId(), application.getName(), application.getDescription());
	}
}
