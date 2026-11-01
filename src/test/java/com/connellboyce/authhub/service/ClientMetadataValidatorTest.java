package com.connellboyce.authhub.service;

import com.connellboyce.authhub.exception.InvalidClientMetadataException;
import com.connellboyce.authhub.model.ClientMetadata;
import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.model.payload.request.ClientMetadataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ClientMetadataValidatorTest {
	private static final String CALLBACK = "https://app.example.com/callback";

	@Mock
	private ScopeService scopeService;

	@InjectMocks
	private ClientMetadataValidator validator;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		when(scopeService.getScopesByNames(any())).thenReturn(List.of());
	}

	@Test
	void registration_validRequest_normalizes() {
		when(scopeService.getScopesByNames(Set.of("orders#read", "orders#write"))).thenReturn(List.of(
				new Scope("s1", "orders#read", "app-1"),
				new Scope("s2", "orders#write", "app-1")));

		ClientMetadata metadata = validator.validateRegistration(new ClientMetadataRequest(null, null, "My App", List.of(CALLBACK),
				List.of("authorization_code", "refresh_token"), List.of("code"), "client_secret_post", " orders#read  orders#write "));

		assertEquals("My App", metadata.clientName());
		assertEquals(Set.of(CALLBACK), metadata.redirectUris());
		assertEquals(Set.of("authorization_code", "refresh_token"), metadata.grantTypes());
		assertEquals("client_secret_post", metadata.tokenEndpointAuthMethod());
		assertEquals(Set.of("orders#read", "orders#write"), metadata.scopes());
	}

	@Test
	void registration_suppliedClientIdAndSecret_areAccepted() {
		ClientMetadata metadata = validator.validateRegistration(
				new ClientMetadataRequest("chosen-id", "chosen-secret", null, List.of(CALLBACK), null, null, null, null));

		assertEquals(Set.of(CALLBACK), metadata.redirectUris());
	}

	@Test
	void registration_clientIdWithUnsafeCharactersOrLength_rejected() {
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateRegistration(
				new ClientMetadataRequest("my app", null, null, List.of(CALLBACK), null, null, null, null)));
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateRegistration(
				new ClientMetadataRequest("../admin", null, null, List.of(CALLBACK), null, null, null, null)));
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateRegistration(
				new ClientMetadataRequest("", null, null, List.of(CALLBACK), null, null, null, null)));
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateRegistration(
				new ClientMetadataRequest("a".repeat(129), null, null, List.of(CALLBACK), null, null, null, null)));
	}

	@Test
	void grantTypes_omitted_defaultsToAuthorizationCode() {
		ClientMetadata metadata = validator.validateRegistration(new ClientMetadataRequest(null, null, null, List.of(CALLBACK), null, null, null, null));

		assertEquals(Set.of("authorization_code"), metadata.grantTypes());
	}

	@Test
	void grantTypes_unsupported_rejected() {
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of(CALLBACK), List.of("authorization_code", "password"), null, null, null)));
	}

	@Test
	void responseTypes_omittedWithoutAuthorizationCode_defaultsToEmpty() {
		ClientMetadata metadata = validator.validateRegistration(new ClientMetadataRequest(null, null, null, null, List.of("client_credentials"), null, null, null));

		assertEquals(Set.of("client_credentials"), metadata.grantTypes());
		assertEquals(Set.of(), metadata.redirectUris());
	}

	@Test
	void responseTypes_mismatchWithAuthorizationCode_rejected() {
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of(CALLBACK), List.of("authorization_code"), List.of("token"), null, null)));
	}

	@Test
	void responseTypes_codeWithoutAuthorizationCode_rejected() {
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, null, List.of("client_credentials"), List.of("code"), null, null)));
	}

	@Test
	void redirectUris_missingForAuthorizationCode_rejected() {
		assertError(InvalidClientMetadataException.INVALID_REDIRECT_URI, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, null, List.of("authorization_code"), null, null, null)));
		assertError(InvalidClientMetadataException.INVALID_REDIRECT_URI, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of(), List.of("authorization_code"), null, null, null)));
	}

	@Test
	void redirectUris_nonLoopbackHttp_rejected() {
		assertError(InvalidClientMetadataException.INVALID_REDIRECT_URI, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of("http://app.example.com/callback"), null, null, null, null)));
	}

	@Test
	void redirectUris_loopbackHttp_accepted() {
		ClientMetadata metadata = validator.validateRegistration(new ClientMetadataRequest(null, null, null,
				List.of("http://localhost:3000/callback", "http://127.0.0.1:2121/cb", "http://[::1]/cb"), null, null, null, null));

		assertEquals(3, metadata.redirectUris().size());
	}

	@Test
	void redirectUris_fragment_rejected() {
		assertError(InvalidClientMetadataException.INVALID_REDIRECT_URI, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of("https://app.example.com/callback#frag"), null, null, null, null)));
	}

	@Test
	void redirectUris_relativeOrMalformedOrCustomScheme_rejected() {
		assertError(InvalidClientMetadataException.INVALID_REDIRECT_URI, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of("/callback"), null, null, null, null)));
		assertError(InvalidClientMetadataException.INVALID_REDIRECT_URI, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of("https://app example.com/callback"), null, null, null, null)));
		assertError(InvalidClientMetadataException.INVALID_REDIRECT_URI, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of("myapp://callback"), null, null, null, null)));
	}

	@Test
	void authMethod_omitted_defaultsToClientSecretBasic() {
		ClientMetadata metadata = validator.validateRegistration(new ClientMetadataRequest(null, null, null, List.of(CALLBACK), null, null, null, null));

		assertEquals("client_secret_basic", metadata.tokenEndpointAuthMethod());
	}

	@Test
	void authMethod_unsupported_rejected() {
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of(CALLBACK), null, null, "none", null)));
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of(CALLBACK), null, null, "private_key_jwt", null)));
	}

	@Test
	void scope_unknown_rejected() {
		when(scopeService.getScopesByNames(Set.of("known", "unknown"))).thenReturn(List.of(new Scope("s1", "known", "app-1")));

		InvalidClientMetadataException ex = assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateRegistration(
				new ClientMetadataRequest(null, null, null, List.of(CALLBACK), null, null, null, "known unknown")));
		assertEquals("Unknown scope: unknown", ex.getMessage());
	}

	@Test
	void scope_ownedByAnotherDeveloper_accepted() {
		when(scopeService.getScopesByNames(Set.of("theirs#read"))).thenReturn(List.of(new Scope("s1", "theirs#read", "someone-elses-app")));

		ClientMetadata metadata = validator.validateRegistration(new ClientMetadataRequest(null, null, null, List.of(CALLBACK), null, null, null, "theirs#read"));

		assertEquals(Set.of("theirs#read"), metadata.scopes());
	}

	@Test
	void scope_omitted_isEmpty() {
		ClientMetadata metadata = validator.validateRegistration(new ClientMetadataRequest(null, null, " ", List.of(CALLBACK), null, null, null, null));

		assertEquals(Set.of(), metadata.scopes());
		assertNull(metadata.clientName());
	}

	@Test
	void update_matchingClientId_normalizes() {
		ClientMetadata metadata = validator.validateUpdate("client-1", new ClientMetadataRequest("client-1", null, null, List.of(CALLBACK), null, null, null, null));

		assertEquals(Set.of("authorization_code"), metadata.grantTypes());
	}

	@Test
	void update_mismatchedOrMissingClientId_rejected() {
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateUpdate("client-1",
				new ClientMetadataRequest("client-2", null, null, List.of(CALLBACK), null, null, null, null)));
		assertError(InvalidClientMetadataException.INVALID_CLIENT_METADATA, () -> validator.validateUpdate("client-1",
				new ClientMetadataRequest(null, null, null, List.of(CALLBACK), null, null, null, null)));
	}

	@Test
	void update_clientSecret_isLeftForTheServiceToVerify() {
		ClientMetadata metadata = validator.validateUpdate("client-1",
				new ClientMetadataRequest("client-1", "secret", null, List.of(CALLBACK), null, null, null, null));

		assertEquals(Set.of(CALLBACK), metadata.redirectUris());
	}

	private InvalidClientMetadataException assertError(String errorCode, Executable executable) {
		InvalidClientMetadataException ex = assertThrows(InvalidClientMetadataException.class, executable);
		assertEquals(errorCode, ex.getErrorCode());
		return ex;
	}
}
