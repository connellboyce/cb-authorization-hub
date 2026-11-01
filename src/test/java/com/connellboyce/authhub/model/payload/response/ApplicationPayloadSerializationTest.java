package com.connellboyce.authhub.model.payload.response;

import com.connellboyce.authhub.model.dao.Application;
import com.connellboyce.authhub.model.dao.Scope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationPayloadSerializationTest {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void applicationResponse_hasNoOwnerField() {
		JsonNode json = objectMapper.valueToTree(ApplicationResponse.from(new Application("app-1", "App", "Desc", "owner-1")));

		assertEquals("app-1", json.get("id").asText());
		assertEquals("App", json.get("name").asText());
		assertEquals("Desc", json.get("description").asText());
		assertEquals(3, json.size());
	}

	@Test
	void scopeResponse_usesSnakeCaseApplicationId() {
		JsonNode json = objectMapper.valueToTree(ScopeResponse.from(new Scope("scope-1", "read", "app-1")));

		assertEquals("app-1", json.get("application_id").asText());
		assertFalse(json.has("applicationId"));
	}

	@Test
	void scopeCatalogResponse_containsOnlyNameAndApplicationSummary() {
		ScopeCatalogResponse catalog = new ScopeCatalogResponse(List.of(
				new ScopeCatalogResponse.Entry("read", new ScopeCatalogResponse.ApplicationSummary("app-1", "App"))));

		JsonNode entry = objectMapper.valueToTree(catalog).get("scopes").get(0);

		assertEquals("read", entry.get("name").asText());
		assertEquals("app-1", entry.get("application").get("id").asText());
		assertEquals("App", entry.get("application").get("name").asText());
		assertEquals(2, entry.size());
		assertEquals(2, entry.get("application").size());
	}
}
