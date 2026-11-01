package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.repository.MongoRegisteredClientRepository;
import com.connellboyce.authhub.repository.ScopeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.connellboyce.authhub.support.TestTokens.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ClientRegistrationControllerTest {
	private static final String OWNER = "owner-1";
	private static final String INTRUDER = "intruder-1";
	private static final String REGISTRATION = """
			{
			  "client_name": "My App",
			  "redirect_uris": ["https://app.example.com/callback"],
			  "grant_types": ["authorization_code", "refresh_token"],
			  "response_types": ["code"],
			  "token_endpoint_auth_method": "client_secret_basic",
			  "scope": "orders#read"
			}
			""";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	MongoRegisteredClientRepository clientRepository;

	@MockBean
	ScopeRepository scopeRepository;

	@BeforeEach
	void setup() {
		when(scopeRepository.findByNameIn(any())).thenReturn(List.of());
		when(scopeRepository.findByNameIn(Set.of("orders#read"))).thenReturn(List.of(new Scope("s1", "orders#read", "app-1")));
		when(clientRepository.existsByClientId(any())).thenReturn(false);
		when(clientRepository.save(any(MongoRegisteredClient.class))).thenAnswer(i -> i.getArgument(0));
		when(clientRepository.findByClientIdAndOwnerId(any(), any())).thenReturn(Optional.empty());
	}

	@Test
	void register_returns201WithGeneratedCredentials() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/clients").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content(REGISTRATION))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.client_id", matchesPattern("[A-Za-z0-9_-]{22}")))
				.andExpect(jsonPath("$.client_secret", matchesPattern("[A-Za-z0-9_-]{43}")))
				.andExpect(jsonPath("$.client_id_issued_at").isNumber())
				.andExpect(jsonPath("$.client_secret_expires_at").value(0))
				.andExpect(jsonPath("$.registration_access_token").doesNotExist())
				.andExpect(jsonPath("$.client_name").value("My App"))
				.andExpect(jsonPath("$.redirect_uris", hasSize(1)))
				.andExpect(jsonPath("$.redirect_uris[0]").value("https://app.example.com/callback"))
				.andExpect(jsonPath("$.grant_types", containsInAnyOrder("authorization_code", "refresh_token")))
				.andExpect(jsonPath("$.response_types", hasSize(1)))
				.andExpect(jsonPath("$.response_types[0]").value("code"))
				.andExpect(jsonPath("$.token_endpoint_auth_method").value("client_secret_basic"))
				.andExpect(jsonPath("$.scope").value("orders#read"))
				.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		String clientId = body.get("client_id").asText();
		String configurationUri = "https://auth.connellboyce.com/api/v1/clients/" + clientId;
		assertEquals(configurationUri, body.get("registration_client_uri").asText());
		assertEquals(configurationUri, result.getResponse().getHeader("Location"));

		ArgumentCaptor<MongoRegisteredClient> saved = ArgumentCaptor.forClass(MongoRegisteredClient.class);
		verify(clientRepository).save(saved.capture());
		assertEquals(OWNER, saved.getValue().getOwnerId());
		assertTrue(passwordEncoder.matches(body.get("client_secret").asText(), saved.getValue().getClientSecret()));
	}

	@Test
	void register_invalidMetadata_returnsRfc7591Error() throws Exception {
		mockMvc.perform(post("/api/v1/clients").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"redirect_uris\": [\"http://app.example.com/callback\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.error").value("invalid_redirect_uri"))
				.andExpect(jsonPath("$.error_description").isString());

		verify(clientRepository, never()).save(any());
	}

	@Test
	void register_suppliedClientId_isUsedAndSecretIsServerGenerated() throws Exception {
		mockMvc.perform(post("/api/v1/clients").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"client_id\": \"my-app\", \"client_secret\": \"chosen-secret\", \"redirect_uris\": [\"https://app.example.com/callback\"]}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "https://auth.connellboyce.com/api/v1/clients/my-app"))
				.andExpect(jsonPath("$.client_id").value("my-app"))
				.andExpect(jsonPath("$.client_secret", matchesPattern("[A-Za-z0-9_-]{43}")));

		verify(clientRepository).save(argThat(client -> "my-app".equals(client.getClientId())
				&& !passwordEncoder.matches("chosen-secret", client.getClientSecret())));
	}

	@Test
	void register_clientIdAlreadyRegistered_returns400() throws Exception {
		when(clientRepository.existsByClientId("taken")).thenReturn(true);

		mockMvc.perform(post("/api/v1/clients").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"client_id\": \"taken\", \"redirect_uris\": [\"https://app.example.com/callback\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_client_metadata"))
				.andExpect(jsonPath("$.error_description").value("client_id is not available"));

		verify(clientRepository, never()).save(any());
	}

	@Test
	void register_invalidClientIdFormat_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/clients").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"client_id\": \"my app\", \"redirect_uris\": [\"https://app.example.com/callback\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_client_metadata"));
	}

	@Test
	void register_malformedBody_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/clients").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_client_metadata"));
	}

	@Test
	void read_owner_returnsClientWithoutSecret() throws Exception {
		when(clientRepository.findByClientIdAndOwnerId("client-1", OWNER)).thenReturn(Optional.of(storedClient()));

		mockMvc.perform(get("/api/v1/clients/client-1").with(developer(OWNER, CLIENTS_READ)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.client_id").value("client-1"))
				.andExpect(jsonPath("$.client_secret").doesNotExist())
				.andExpect(jsonPath("$.registration_access_token").doesNotExist())
				.andExpect(jsonPath("$.registration_client_uri").value("https://auth.connellboyce.com/api/v1/clients/client-1"))
				.andExpect(content().string(not(containsString("stored-hash"))));
	}

	@Test
	void list_returnsOnlyOwnedClientsWithoutSecrets() throws Exception {
		when(clientRepository.findByOwnerId(OWNER)).thenReturn(Optional.of(List.of(storedClient())));

		mockMvc.perform(get("/api/v1/clients").with(developer(OWNER, CLIENTS_READ)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clients", hasSize(1)))
				.andExpect(jsonPath("$.clients[0].client_id").value("client-1"))
				.andExpect(jsonPath("$.clients[0].client_secret").doesNotExist());

		verify(clientRepository).findByOwnerId(OWNER);
		verify(clientRepository, never()).findAll();
	}

	@Test
	void list_noneOwned_returnsEmptyArray() throws Exception {
		when(clientRepository.findByOwnerId(OWNER)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/clients").with(developer(OWNER, CLIENTS_READ)))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"clients\": []}", true));
	}

	@Test
	void update_fullReplacement_resetsOmittedFields() throws Exception {
		when(clientRepository.findByClientIdAndOwnerId("client-1", OWNER)).thenReturn(Optional.of(storedClient()));

		mockMvc.perform(put("/api/v1/clients/client-1").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"client_id\": \"client-1\", \"redirect_uris\": [\"https://new.example.com/cb\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.client_name").doesNotExist())
				.andExpect(jsonPath("$.grant_types", hasSize(1)))
				.andExpect(jsonPath("$.grant_types[0]").value("authorization_code"))
				.andExpect(jsonPath("$.redirect_uris", hasSize(1)))
				.andExpect(jsonPath("$.redirect_uris[0]").value("https://new.example.com/cb"))
				.andExpect(jsonPath("$.token_endpoint_auth_method").value("client_secret_basic"))
				.andExpect(jsonPath("$.scope").doesNotExist())
				.andExpect(jsonPath("$.client_secret").doesNotExist());

		ArgumentCaptor<MongoRegisteredClient> saved = ArgumentCaptor.forClass(MongoRegisteredClient.class);
		verify(clientRepository).save(saved.capture());
		assertEquals("stored-hash", saved.getValue().getClientSecret());
		assertEquals(OWNER, saved.getValue().getOwnerId());
	}

	@Test
	void update_mismatchedClientId_returns400() throws Exception {
		when(clientRepository.findByClientIdAndOwnerId("client-1", OWNER)).thenReturn(Optional.of(storedClient()));

		mockMvc.perform(put("/api/v1/clients/client-1").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"client_id\": \"client-2\", \"redirect_uris\": [\"https://app.example.com/cb\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_client_metadata"));

		verify(clientRepository, never()).save(any());
	}

	@Test
	void update_withMatchingClientSecret_returns200() throws Exception {
		MongoRegisteredClient client = storedClient();
		client.setClientSecret(passwordEncoder.encode("current-secret"));
		when(clientRepository.findByClientIdAndOwnerId("client-1", OWNER)).thenReturn(Optional.of(client));

		mockMvc.perform(put("/api/v1/clients/client-1").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"client_id\": \"client-1\", \"client_secret\": \"current-secret\", \"redirect_uris\": [\"https://app.example.com/cb\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.client_secret").doesNotExist());
	}

	@Test
	void update_withMismatchedClientSecret_returns400() throws Exception {
		MongoRegisteredClient client = storedClient();
		client.setClientSecret(passwordEncoder.encode("current-secret"));
		when(clientRepository.findByClientIdAndOwnerId("client-1", OWNER)).thenReturn(Optional.of(client));

		mockMvc.perform(put("/api/v1/clients/client-1").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"client_id\": \"client-1\", \"client_secret\": \"guess\", \"redirect_uris\": [\"https://app.example.com/cb\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_client_metadata"));

		verify(clientRepository, never()).save(any());
	}

	@Test
	void nonOwner_receives404IdenticalToMissing() throws Exception {
		String body = "{\"client_id\": \"client-1\", \"redirect_uris\": [\"https://app.example.com/cb\"]}";

		MvcResult notOwnedRead = mockMvc.perform(get("/api/v1/clients/client-1").with(developer(INTRUDER, CLIENTS_READ)))
				.andExpect(status().isNotFound()).andReturn();
		MvcResult missingRead = mockMvc.perform(get("/api/v1/clients/missing").with(developer(INTRUDER, CLIENTS_READ)))
				.andExpect(status().isNotFound()).andReturn();
		mockMvc.perform(put("/api/v1/clients/client-1").with(developer(INTRUDER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/v1/clients/client-1").with(developer(INTRUDER, CLIENTS_WRITE)))
				.andExpect(status().isNotFound());

		assertEquals(missingRead.getResponse().getContentAsString(), notOwnedRead.getResponse().getContentAsString());
		verify(clientRepository, never()).save(any());
		verify(clientRepository, never()).deleteByClientId(any());
	}

	@Test
	void delete_owner_returns204() throws Exception {
		when(clientRepository.findByClientIdAndOwnerId("client-1", OWNER)).thenReturn(Optional.of(storedClient()));

		mockMvc.perform(delete("/api/v1/clients/client-1").with(developer(OWNER, CLIENTS_WRITE)))
				.andExpect(status().isNoContent());

		verify(clientRepository).deleteByClientId("client-1");
	}

	@Test
	void legacyClient_reportsBasicAndCollapsesMethodsOnUpdate() throws Exception {
		MongoRegisteredClient legacy = storedClient();
		legacy.setClientIdIssuedAt(null);
		legacy.setClientAuthenticationMethods(Set.of("client_secret_basic", "client_secret_post"));
		when(clientRepository.findByClientIdAndOwnerId("client-1", OWNER)).thenReturn(Optional.of(legacy));

		mockMvc.perform(get("/api/v1/clients/client-1").with(developer(OWNER, CLIENTS_READ)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token_endpoint_auth_method").value("client_secret_basic"))
				.andExpect(jsonPath("$.client_id_issued_at").doesNotExist());

		mockMvc.perform(put("/api/v1/clients/client-1").with(developer(OWNER, CLIENTS_WRITE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"client_id\": \"client-1\", \"redirect_uris\": [\"https://app.example.com/cb\"], \"token_endpoint_auth_method\": \"client_secret_post\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token_endpoint_auth_method").value("client_secret_post"));

		ArgumentCaptor<MongoRegisteredClient> saved = ArgumentCaptor.forClass(MongoRegisteredClient.class);
		verify(clientRepository).save(saved.capture());
		assertEquals(Set.of("client_secret_post"), saved.getValue().getClientAuthenticationMethods());
	}

	private MongoRegisteredClient storedClient() {
		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setId("mongo-id");
		client.setClientId("client-1");
		client.setClientSecret("stored-hash");
		client.setClientName("My App");
		client.setClientIdIssuedAt(Instant.parse("2026-11-01T00:00:00Z"));
		client.setOwnerId(OWNER);
		client.setRedirectUris(Set.of("https://app.example.com/callback"));
		client.setAuthorizationGrantTypes(Set.of("authorization_code", "refresh_token"));
		client.setClientAuthenticationMethods(Set.of("client_secret_basic"));
		client.setScopes(Set.of("orders#read"));
		return client;
	}
}
