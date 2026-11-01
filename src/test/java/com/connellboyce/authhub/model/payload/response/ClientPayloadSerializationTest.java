package com.connellboyce.authhub.model.payload.response;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.connellboyce.authhub.model.payload.request.ClientMetadataRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClientPayloadSerializationTest {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void clientInformationResponse_withoutSecret_omitsClientSecret() {
		JsonNode json = objectMapper.valueToTree(ClientInformationResponse.from(client(), null, "http://localhost/api/v1/clients/client-1"));

		assertFalse(json.has("client_secret"));
		assertFalse(json.has("registration_access_token"));
		assertEquals("client-1", json.get("client_id").asText());
		assertEquals(1793491200L, json.get("client_id_issued_at").asLong());
		assertEquals(0L, json.get("client_secret_expires_at").asLong());
		assertEquals("http://localhost/api/v1/clients/client-1", json.get("registration_client_uri").asText());
		assertEquals("My App", json.get("client_name").asText());
		assertEquals("code", json.get("response_types").get(0).asText());
		assertEquals("client_secret_post", json.get("token_endpoint_auth_method").asText());
		assertEquals("a b", json.get("scope").asText());
	}

	@Test
	void clientInformationResponse_withSecret_includesClientSecret() {
		JsonNode json = objectMapper.valueToTree(ClientInformationResponse.from(client(), "plain-secret", "uri"));

		assertEquals("plain-secret", json.get("client_secret").asText());
	}

	@Test
	void clientInformationResponse_legacyClient_omitsIssuedAtAndReportsBasic() {
		MongoRegisteredClient legacy = client();
		legacy.setClientIdIssuedAt(null);
		legacy.setClientName(null);
		legacy.setClientAuthenticationMethods(Set.of("client_secret_post", "client_secret_basic"));
		legacy.setAuthorizationGrantTypes(Set.of("client_credentials"));

		JsonNode json = objectMapper.valueToTree(ClientInformationResponse.from(legacy, null, "uri"));

		assertFalse(json.has("client_id_issued_at"));
		assertFalse(json.has("client_name"));
		assertEquals("client_secret_basic", json.get("token_endpoint_auth_method").asText());
		assertEquals(0, json.get("response_types").size());
	}

	@Test
	void clientInformationResponse_withoutScopes_omitsScope() {
		MongoRegisteredClient client = client();
		client.setScopes(Set.of());

		JsonNode json = objectMapper.valueToTree(ClientInformationResponse.from(client, null, "uri"));

		assertFalse(json.has("scope"));
	}

	@Test
	void clientMetadataRequest_dropsUnknownFieldsAndReadsSnakeCase() throws Exception {
		ClientMetadataRequest request = objectMapper.readValue("""
				{
				  "client_name": "My App",
				  "redirect_uris": ["https://app.example.com/cb"],
				  "grant_types": ["authorization_code"],
				  "response_types": ["code"],
				  "token_endpoint_auth_method": "client_secret_basic",
				  "scope": "a b",
				  "logo_uri": "https://evil.example.com/logo.png",
				  "owner_id": "someone-else"
				}
				""", ClientMetadataRequest.class);

		assertEquals("My App", request.clientName());
		assertEquals(List.of("https://app.example.com/cb"), request.redirectUris());
		assertEquals(List.of("authorization_code"), request.grantTypes());
		assertEquals(List.of("code"), request.responseTypes());
		assertEquals("client_secret_basic", request.tokenEndpointAuthMethod());
		assertEquals("a b", request.scope());
		assertNull(request.clientId());
		assertNull(request.clientSecret());
	}

	private MongoRegisteredClient client() {
		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setClientId("client-1");
		client.setClientSecret("stored-hash");
		client.setClientIdIssuedAt(Instant.ofEpochSecond(1793491200L));
		client.setClientName("My App");
		client.setRedirectUris(Set.of("https://app.example.com/cb"));
		client.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
		client.setClientAuthenticationMethods(Set.of("client_secret_post"));
		client.setScopes(Set.of("b", "a"));
		return client;
	}
}
