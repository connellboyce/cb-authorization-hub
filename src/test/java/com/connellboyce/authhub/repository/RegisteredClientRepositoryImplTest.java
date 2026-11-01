package com.connellboyce.authhub.repository;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegisteredClientRepositoryImplTest {
	@InjectMocks
	private RegisteredClientRepositoryImpl registeredClientRepository;

	@Mock
	private MongoRegisteredClientRepository mongoRegisteredClientRepository;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testSaveDefaultRegisteredClient_throwsException() {
		assertThrows(UnsupportedOperationException.class, ()-> registeredClientRepository.save(mock(RegisteredClient.class)));
	}

	@Test
	void testFindById_notFound() {
		var result = registeredClientRepository.findById("nonexistent-id");
		assertNull(result);
	}

	@Test
	void testFindByClientId_notFound() {
		var result = registeredClientRepository.findByClientId("nonexistent-client-id");
		assertNull(result);
	}

	@Test
	void testFindById_found() {
		MongoRegisteredClient mongoClient = new MongoRegisteredClient();
		mongoClient.setClientId("client-id-123");
		mongoClient.setClientSecret("secret-xyz");
		mongoClient.setId("existing-id");
		mongoClient.setOwnerId("owner-456");
		mongoClient.setScopes(Set.of("scope1", "scope2"));
		mongoClient.setRedirectUris(Set.of("https://example.com/callback"));
		mongoClient.setClientAuthenticationMethods(Set.of("client_secret_basic"));
		mongoClient.setAuthorizationGrantTypes(Set.of("authorization_code"));
		mongoClient.setRequireAuthorizationConsent(false);
		mongoClient.setUseCustomTheme(false);

		when(mongoRegisteredClientRepository.findById("existing-id"))
				.thenReturn(Optional.of(mongoClient));

		RegisteredClient result = registeredClientRepository.findById("existing-id");
		assertNotNull(result);
		assertEquals("existing-id", result.getId());
		assertEquals("client-id-123", result.getClientId());
		assertEquals("secret-xyz", result.getClientSecret());
		assertEquals(2, result.getScopes().size());
		assertEquals(1, result.getRedirectUris().size());
		assertEquals(1, result.getClientAuthenticationMethods().size());
		assertEquals(1, result.getAuthorizationGrantTypes().size());
		assertFalse(result.getClientSettings().isRequireAuthorizationConsent());
	}

	@Test
	void testFindByClientId_found() {
		MongoRegisteredClient mongoClient = new MongoRegisteredClient();
		mongoClient.setClientId("client-id-123");
		mongoClient.setClientSecret("secret-xyz");
		mongoClient.setId("existing-id");
		mongoClient.setOwnerId("owner-456");
		mongoClient.setScopes(Set.of("scope1", "scope2"));
		mongoClient.setRedirectUris(Set.of("https://example.com/callback"));
		mongoClient.setClientAuthenticationMethods(Set.of("client_secret_basic"));
		mongoClient.setAuthorizationGrantTypes(Set.of("authorization_code"));
		mongoClient.setRequireAuthorizationConsent(false);
		mongoClient.setUseCustomTheme(false);

		when(mongoRegisteredClientRepository.findByClientId("client-id-123"))
				.thenReturn(Optional.of(mongoClient));

		RegisteredClient result = registeredClientRepository.findByClientId("client-id-123");
		assertNotNull(result);
		assertEquals("existing-id", result.getId());
		assertEquals("client-id-123", result.getClientId());
		assertEquals("secret-xyz", result.getClientSecret());
		assertEquals(2, result.getScopes().size());
		assertEquals(1, result.getRedirectUris().size());
		assertEquals(1, result.getClientAuthenticationMethods().size());
		assertEquals(1, result.getAuthorizationGrantTypes().size());
		assertFalse(result.getClientSettings().isRequireAuthorizationConsent());
	}

	@Test
	void testFindByClientId_mapsRegistrationMetadata() {
		Instant issuedAt = Instant.parse("2026-11-01T00:00:00Z");
		MongoRegisteredClient mongoClient = new MongoRegisteredClient();
		mongoClient.setId("existing-id");
		mongoClient.setClientId("client-id-123");
		mongoClient.setClientName("My App");
		mongoClient.setClientIdIssuedAt(issuedAt);
		mongoClient.setClientSecret("secret-xyz");
		mongoClient.setScopes(Set.of("scope1"));
		mongoClient.setRedirectUris(Set.of());
		mongoClient.setClientAuthenticationMethods(Set.of("client_secret_basic"));
		mongoClient.setAuthorizationGrantTypes(Set.of("client_credentials"));

		when(mongoRegisteredClientRepository.findByClientId("client-id-123"))
				.thenReturn(Optional.of(mongoClient));

		RegisteredClient result = registeredClientRepository.findByClientId("client-id-123");
		assertEquals("My App", result.getClientName());
		assertEquals(issuedAt, result.getClientIdIssuedAt());
		assertTrue(result.getRedirectUris().isEmpty());
	}

	@Test
	void testFindByClientId_legacyDocumentWithoutRegistrationMetadata() {
		MongoRegisteredClient mongoClient = new MongoRegisteredClient();
		mongoClient.setId("existing-id");
		mongoClient.setClientId("client-id-123");
		mongoClient.setClientSecret("secret-xyz");
		mongoClient.setScopes(Set.of("scope1"));
		mongoClient.setRedirectUris(Set.of("https://example.com/callback"));
		mongoClient.setClientAuthenticationMethods(Set.of("client_secret_basic", "client_secret_post"));
		mongoClient.setAuthorizationGrantTypes(Set.of("authorization_code"));

		when(mongoRegisteredClientRepository.findByClientId("client-id-123"))
				.thenReturn(Optional.of(mongoClient));

		RegisteredClient result = registeredClientRepository.findByClientId("client-id-123");
		assertEquals("existing-id", result.getClientName());
		assertNull(result.getClientIdIssuedAt());
		assertEquals(2, result.getClientAuthenticationMethods().size());
	}
}