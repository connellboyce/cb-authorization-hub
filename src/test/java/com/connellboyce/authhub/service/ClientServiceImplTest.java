package com.connellboyce.authhub.service;

import com.connellboyce.authhub.exception.InvalidClientMetadataException;
import com.connellboyce.authhub.exception.ResourceNotFoundException;
import com.connellboyce.authhub.model.ClientMetadata;
import com.connellboyce.authhub.model.ClientRegistrationResult;
import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.connellboyce.authhub.repository.MongoRegisteredClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientServiceImplTest {

	@Mock
	private MongoRegisteredClientRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private UserService userService;

	@InjectMocks
	private ClientServiceImpl clientService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		AuthUtilServiceImpl authUtilService = new AuthUtilServiceImpl();
		ReflectionTestUtils.setField(authUtilService, "userService", userService);
		ReflectionTestUtils.setField(clientService, "authUtilService", authUtilService);
	}

	@Test
	void createClient_success_persistsMappedClientAndEncodesSecret() {
		RegisteredClient registeredClient = RegisteredClient.withId("mongo-id")
				.clientId("client-1")
				.clientSecret("raw-secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.redirectUri("http://localhost/callback")
				.scope("read")
				.clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
				.build();

		when(passwordEncoder.encode("raw-secret")).thenReturn("encoded-secret");
		when(repository.save(any(MongoRegisteredClient.class))).thenAnswer(i -> i.getArgument(0));

		MongoRegisteredClient result = clientService.createClient(registeredClient, "owner-1");

		assertNotNull(result);
		assertEquals("mongo-id", result.getId());
		assertEquals("client-1", result.getClientId());
		assertEquals("encoded-secret", result.getClientSecret());
		assertEquals(Set.of("client_secret_basic"), result.getClientAuthenticationMethods());
		assertEquals(Set.of("client_credentials"), result.getAuthorizationGrantTypes());
		assertEquals(Set.of("http://localhost/callback"), result.getRedirectUris());
		assertEquals(Set.of("read"), result.getScopes());
		assertTrue(result.isRequireAuthorizationConsent());
		assertEquals("owner-1", result.getOwnerId());
	}

	@Test
	void getClientsByOwner_found() {
		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setClientId("client-1");
		when(repository.findByOwnerId("owner-1")).thenReturn(Optional.of(List.of(client)));

		List<MongoRegisteredClient> result = clientService.getClientsByOwner("owner-1");

		assertEquals(1, result.size());
		assertEquals("client-1", result.get(0).getClientId());
	}

	@Test
	void getClientsByOwner_none_returnsEmptyList() {
		when(repository.findByOwnerId("owner-1")).thenReturn(Optional.empty());

		List<MongoRegisteredClient> result = clientService.getClientsByOwner("owner-1");

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void getClientByClientId_found() {
		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setClientId("client-1");
		when(repository.findByClientId("client-1")).thenReturn(Optional.of(client));

		assertEquals(client, clientService.getClientByClientId("client-1"));
	}

	@Test
	void getClientByClientId_notFound_returnsNull() {
		when(repository.findByClientId("missing")).thenReturn(Optional.empty());

		assertNull(clientService.getClientByClientId("missing"));
	}

	@Test
	void deleteByClientId_delegatesToRepository() {
		clientService.deleteByClientId("client-1");

		verify(repository).deleteByClientId("client-1");
	}

	@Test
	void updateClient_success_updatesAndPersists() throws Exception {
		MongoRegisteredClient existing = new MongoRegisteredClient();
		existing.setClientId("client-1");
		when(repository.findByClientId("client-1")).thenReturn(Optional.of(existing));
		when(repository.save(any(MongoRegisteredClient.class))).thenAnswer(i -> i.getArgument(0));

		MongoRegisteredClient result = clientService.updateClient(
				"client-1",
				List.of("refresh_token"),
				List.of("http://localhost/new-callback"),
				List.of("write")
		);

		assertEquals(Set.of("refresh_token"), result.getAuthorizationGrantTypes());
		assertEquals(Set.of("http://localhost/new-callback"), result.getRedirectUris());
		assertEquals(Set.of("write"), result.getScopes());
	}

	@Test
	void updateClient_notFound_throws() {
		when(repository.findByClientId("missing")).thenReturn(Optional.empty());

		Exception ex = assertThrows(Exception.class, () ->
				clientService.updateClient("missing", List.of(), List.of(), List.of()));
		assertEquals("Client not found", ex.getMessage());
	}

	@Test
	void validateClientOwnership_falseWhenAuthenticationNull() {
		assertFalse(clientService.validateClientOwnership(null, "client-1"));
	}

	@Test
	void validateClientOwnership_falseWhenClientIdNullOrEmpty() {
		Authentication auth = mock(Authentication.class);
		assertFalse(clientService.validateClientOwnership(auth, null));
		assertFalse(clientService.validateClientOwnership(auth, ""));
	}

	@Test
	void validateClientOwnership_falseWhenClientNotFound() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);
		when(userService.getCBUserByUsername("alice"))
				.thenReturn(new CBUser("user-1", "alice", "pass", Set.of(), "email", "Alice", "Smith"));
		when(repository.findByClientId("client-1")).thenReturn(Optional.empty());

		assertFalse(clientService.validateClientOwnership(auth, "client-1"));
	}

	@Test
	void validateClientOwnership_falseWhenUserNotFound() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);
		when(userService.getCBUserByUsername("alice")).thenReturn(null);

		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setOwnerId("user-1");
		when(repository.findByClientId("client-1")).thenReturn(Optional.of(client));

		assertFalse(clientService.validateClientOwnership(auth, "client-1"));
	}

	@Test
	void validateClientOwnership_falseWhenClientOwnerIdNullOrEmpty() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);
		when(userService.getCBUserByUsername("alice"))
				.thenReturn(new CBUser("user-1", "alice", "pass", Set.of(), "email", "Alice", "Smith"));

		MongoRegisteredClient clientWithNullOwner = new MongoRegisteredClient();
		clientWithNullOwner.setOwnerId(null);
		when(repository.findByClientId("client-1")).thenReturn(Optional.of(clientWithNullOwner));
		assertFalse(clientService.validateClientOwnership(auth, "client-1"));

		MongoRegisteredClient clientWithEmptyOwner = new MongoRegisteredClient();
		clientWithEmptyOwner.setOwnerId("");
		when(repository.findByClientId("client-2")).thenReturn(Optional.of(clientWithEmptyOwner));
		assertFalse(clientService.validateClientOwnership(auth, "client-2"));
	}

	@Test
	void validateClientOwnership_falseWhenUserIdNullOrEmpty() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);

		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setOwnerId("owner-1");
		when(repository.findByClientId("client-1")).thenReturn(Optional.of(client));

		when(userService.getCBUserByUsername("alice"))
				.thenReturn(new CBUser(null, "alice", "pass", Set.of(), "email", "Alice", "Smith"));
		assertFalse(clientService.validateClientOwnership(auth, "client-1"));

		when(userService.getCBUserByUsername("alice"))
				.thenReturn(new CBUser("", "alice", "pass", Set.of(), "email", "Alice", "Smith"));
		assertFalse(clientService.validateClientOwnership(auth, "client-1"));
	}

	@Test
	void validateClientOwnership_trueWhenOwnerMatches() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);
		when(userService.getCBUserByUsername("alice"))
				.thenReturn(new CBUser("owner-1", "alice", "pass", Set.of(), "email", "Alice", "Smith"));

		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setOwnerId("owner-1");
		when(repository.findByClientId("client-1")).thenReturn(Optional.of(client));

		assertTrue(clientService.validateClientOwnership(auth, "client-1"));
	}

	@Test
	void validateClientOwnership_falseWhenOwnerMismatch() {
		Authentication auth = mock(Authentication.class);
		UserDetails userDetails = new User("alice", "pass", Set.of());
		when(auth.getPrincipal()).thenReturn(userDetails);
		when(userService.getCBUserByUsername("alice"))
				.thenReturn(new CBUser("someone-else", "alice", "pass", Set.of(), "email", "Alice", "Smith"));

		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setOwnerId("owner-1");
		when(repository.findByClientId("client-1")).thenReturn(Optional.of(client));

		assertFalse(clientService.validateClientOwnership(auth, "client-1"));
	}

	@Test
	void validateClientOwnership_jwtPrincipal_trueWhenSubMatchesOwner() {
		Authentication auth = mock(Authentication.class);
		when(auth.getPrincipal()).thenReturn(userJwt("owner-1"));

		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setOwnerId("owner-1");
		when(repository.findByClientId("client-1")).thenReturn(Optional.of(client));

		assertTrue(clientService.validateClientOwnership(auth, "client-1"));
	}

	@Test
	void validateClientOwnership_jwtPrincipal_falseWhenSubDiffersFromOwner() {
		Authentication auth = mock(Authentication.class);
		when(auth.getPrincipal()).thenReturn(userJwt("someone-else"));

		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setOwnerId("owner-1");
		when(repository.findByClientId("client-1")).thenReturn(Optional.of(client));

		assertFalse(clientService.validateClientOwnership(auth, "client-1"));
	}

	@Test
	void validateClientOwnership_jwtPrincipal_falseForClientCredentialsToken() {
		Authentication auth = mock(Authentication.class);
		when(auth.getPrincipal()).thenReturn(Jwt.withTokenValue("token").header("alg", "none").subject("owner-1").build());

		MongoRegisteredClient client = new MongoRegisteredClient();
		client.setOwnerId("owner-1");
		when(repository.findByClientId("client-1")).thenReturn(Optional.of(client));

		assertFalse(clientService.validateClientOwnership(auth, "client-1"));
	}

	private Jwt userJwt(String subject) {
		return Jwt.withTokenValue("token").header("alg", "none").subject(subject).claim("username", "alice").build();
	}

	private static final ClientMetadata METADATA = new ClientMetadata(
			"My App",
			Set.of("https://app.example.com/callback"),
			Set.of("authorization_code", "refresh_token"),
			"client_secret_post",
			Set.of("openid")
	);

	@Test
	void registerClient_generatesCredentialsAndStoresBcryptHash() {
		ReflectionTestUtils.setField(clientService, "passwordEncoder", new BCryptPasswordEncoder());
		when(repository.existsByClientId(any())).thenReturn(false);
		when(repository.save(any(MongoRegisteredClient.class))).thenAnswer(i -> i.getArgument(0));

		ClientRegistrationResult result = clientService.registerClient(METADATA, null, "owner-1");
		MongoRegisteredClient client = result.client();

		assertEquals(22, client.getClientId().length());
		assertEquals(43, result.clientSecret().length());
		assertNotEquals(result.clientSecret(), client.getClientSecret());
		assertTrue(client.getClientSecret().startsWith("$2"));
		assertTrue(new BCryptPasswordEncoder().matches(result.clientSecret(), client.getClientSecret()));
		assertNotNull(client.getId());
		assertNotNull(client.getClientIdIssuedAt());
		assertEquals("owner-1", client.getOwnerId());
		assertEquals("My App", client.getClientName());
		assertEquals(Set.of("https://app.example.com/callback"), client.getRedirectUris());
		assertEquals(Set.of("authorization_code", "refresh_token"), client.getAuthorizationGrantTypes());
		assertEquals(Set.of("client_secret_post"), client.getClientAuthenticationMethods());
		assertEquals(Set.of("openid"), client.getScopes());
	}

	@Test
	void registerClient_regeneratesClientIdOnCollision() {
		when(passwordEncoder.encode(any())).thenReturn("encoded");
		when(repository.existsByClientId(any())).thenReturn(true, false);
		when(repository.save(any(MongoRegisteredClient.class))).thenAnswer(i -> i.getArgument(0));

		clientService.registerClient(METADATA, null, "owner-1");

		verify(repository, times(2)).existsByClientId(any());
	}

	@Test
	void registerClient_requestedClientId_isUsedWhenAvailable() {
		when(passwordEncoder.encode(any())).thenReturn("encoded");
		when(repository.existsByClientId("my-app")).thenReturn(false);
		when(repository.save(any(MongoRegisteredClient.class))).thenAnswer(i -> i.getArgument(0));

		ClientRegistrationResult result = clientService.registerClient(METADATA, "my-app", "owner-1");

		assertEquals("my-app", result.client().getClientId());
		assertEquals(43, result.clientSecret().length());
	}

	@Test
	void registerClient_requestedClientIdTakenByClient_throwsInvalidClientMetadata() {
		when(repository.existsByClientId("my-app")).thenReturn(true);

		InvalidClientMetadataException ex = assertThrows(InvalidClientMetadataException.class,
				() -> clientService.registerClient(METADATA, "my-app", "owner-1"));

		assertEquals("client_id is not available", ex.getMessage());
		verify(repository, never()).save(any());
	}

	@Test
	void findOwnedClient_nonOwner_returnsEmpty() {
		when(repository.findByClientIdAndOwnerId("client-1", "intruder")).thenReturn(Optional.empty());

		assertTrue(clientService.findOwnedClient("client-1", "intruder").isEmpty());
	}

	@Test
	void findOwnedClient_owner_returnsClient() {
		MongoRegisteredClient client = new MongoRegisteredClient();
		when(repository.findByClientIdAndOwnerId("client-1", "owner-1")).thenReturn(Optional.of(client));

		assertEquals(Optional.of(client), clientService.findOwnedClient("client-1", "owner-1"));
	}

	@Test
	void replaceOwnedClient_replacesMetadataAndKeepsCredentialsOwnerAndSettings() {
		MongoRegisteredClient existing = new MongoRegisteredClient();
		existing.setId("mongo-id");
		existing.setClientId("client-1");
		existing.setClientSecret("stored-hash");
		existing.setOwnerId("owner-1");
		existing.setRequireAuthorizationConsent(true);
		existing.setUseCustomTheme(true);
		existing.setClientName("Old");
		existing.setScopes(Set.of("legacy"));
		existing.setClientAuthenticationMethods(Set.of("client_secret_basic", "client_secret_post"));
		when(repository.findByClientIdAndOwnerId("client-1", "owner-1")).thenReturn(Optional.of(existing));
		when(repository.save(any(MongoRegisteredClient.class))).thenAnswer(i -> i.getArgument(0));

		MongoRegisteredClient replaced = clientService.replaceOwnedClient("client-1", METADATA, null, "owner-1");

		assertEquals("mongo-id", replaced.getId());
		assertEquals("client-1", replaced.getClientId());
		assertEquals("stored-hash", replaced.getClientSecret());
		assertEquals("owner-1", replaced.getOwnerId());
		assertTrue(replaced.isRequireAuthorizationConsent());
		assertTrue(replaced.isUseCustomTheme());
		assertEquals("My App", replaced.getClientName());
		assertEquals(Set.of("openid"), replaced.getScopes());
		assertEquals(Set.of("client_secret_post"), replaced.getClientAuthenticationMethods());
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void replaceOwnedClient_nonOwner_throwsNotFound() {
		when(repository.findByClientIdAndOwnerId("client-1", "intruder")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> clientService.replaceOwnedClient("client-1", METADATA, "guessed-secret", "intruder"));
		verify(passwordEncoder, never()).matches(any(), any());
		verify(repository, never()).save(any());
	}

	@Test
	void replaceOwnedClient_matchingSecret_isAccepted() {
		MongoRegisteredClient existing = new MongoRegisteredClient();
		existing.setClientId("client-1");
		existing.setClientSecret("stored-hash");
		existing.setOwnerId("owner-1");
		when(repository.findByClientIdAndOwnerId("client-1", "owner-1")).thenReturn(Optional.of(existing));
		when(passwordEncoder.matches("current-secret", "stored-hash")).thenReturn(true);
		when(repository.save(any(MongoRegisteredClient.class))).thenAnswer(i -> i.getArgument(0));

		MongoRegisteredClient replaced = clientService.replaceOwnedClient("client-1", METADATA, "current-secret", "owner-1");

		assertEquals("stored-hash", replaced.getClientSecret());
	}

	@Test
	void replaceOwnedClient_mismatchedSecret_throwsInvalidClientMetadata() {
		MongoRegisteredClient existing = new MongoRegisteredClient();
		existing.setClientId("client-1");
		existing.setClientSecret("stored-hash");
		existing.setOwnerId("owner-1");
		when(repository.findByClientIdAndOwnerId("client-1", "owner-1")).thenReturn(Optional.of(existing));
		when(passwordEncoder.matches("wrong-secret", "stored-hash")).thenReturn(false);

		InvalidClientMetadataException ex = assertThrows(InvalidClientMetadataException.class,
				() -> clientService.replaceOwnedClient("client-1", METADATA, "wrong-secret", "owner-1"));

		assertEquals(InvalidClientMetadataException.INVALID_CLIENT_METADATA, ex.getErrorCode());
		verify(repository, never()).save(any());
	}

	@Test
	void deleteOwnedClient_owner_deletes() {
		MongoRegisteredClient existing = new MongoRegisteredClient();
		existing.setClientId("client-1");
		when(repository.findByClientIdAndOwnerId("client-1", "owner-1")).thenReturn(Optional.of(existing));

		clientService.deleteOwnedClient("client-1", "owner-1");

		verify(repository).deleteByClientId("client-1");
	}

	@Test
	void deleteOwnedClient_nonOwner_throwsNotFound() {
		when(repository.findByClientIdAndOwnerId("client-1", "intruder")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> clientService.deleteOwnedClient("client-1", "intruder"));
		verify(repository, never()).deleteByClientId(any());
	}
}
