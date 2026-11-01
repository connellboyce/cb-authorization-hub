package com.connellboyce.authhub.model.payload.response;

import java.util.List;

public record ScopeCatalogResponse(List<Entry> scopes) {
	public record Entry(String name, ApplicationSummary application) {
	}

	public record ApplicationSummary(String id, String name) {
	}
}
