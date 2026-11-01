package com.connellboyce.authhub.config;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManagementJwtAuthenticationConverterTest {

	private UserRepository userRepository;
	private ManagementJwtAuthenticationConverter converter;

	@BeforeEach
	void setup() {
		userRepository = mock(UserRepository.class);
		converter = new ManagementJwtAuthenticationConverter(userRepository);
	}

	@Test
	void convert_existingUser_authoritiesFromDatabaseRolesAndTokenScopes() {
		when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1", "alice", Set.of("USER", "DEVELOPER"))));

		AbstractAuthenticationToken authentication = converter.convert(jwt("user-1", "alice", "urn:cb:scope:authhub:clients#read"));

		assertEquals("user-1", authentication.getName());
		assertEquals(Set.of("ROLE_USER", "ROLE_DEVELOPER", "SCOPE_urn:cb:scope:authhub:clients#read"), authorities(authentication));
	}

	@Test
	void convert_missingUser_throwsInvalidBearerToken() {
		when(userRepository.findById("user-1")).thenReturn(Optional.empty());

		assertThrows(InvalidBearerTokenException.class, () -> converter.convert(jwt("user-1", "alice", "openid")));
	}

	@Test
	void convert_clientCredentialsToken_throwsInvalidBearerToken() {
		when(userRepository.findById("client-1")).thenReturn(Optional.of(user("client-1", "alice", Set.of("DEVELOPER"))));
		Jwt clientToken = Jwt.withTokenValue("token").header("alg", "none").subject("client-1").claim("scope", "openid").build();

		assertThrows(InvalidBearerTokenException.class, () -> converter.convert(clientToken));
	}

	@Test
	void convert_usernameMismatch_throwsInvalidBearerToken() {
		when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1", "bob", Set.of("DEVELOPER"))));

		assertThrows(InvalidBearerTokenException.class, () -> converter.convert(jwt("user-1", "alice", "openid")));
	}

	@Test
	void convert_roleRevoked_ignoresRoleClaimInToken() {
		when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1", "alice", Set.of("USER"))));
		Jwt token = Jwt.withTokenValue("token").header("alg", "none")
				.subject("user-1")
				.claim("username", "alice")
				.claim("role", List.of("ROLE_DEVELOPER"))
				.claim("scope", "openid")
				.build();

		AbstractAuthenticationToken authentication = converter.convert(token);

		assertFalse(authorities(authentication).contains("ROLE_DEVELOPER"));
		assertTrue(authorities(authentication).contains("ROLE_USER"));
	}

	private Jwt jwt(String subject, String username, String scope) {
		return Jwt.withTokenValue("token").header("alg", "none")
				.subject(subject)
				.claim("username", username)
				.claim("scope", scope)
				.build();
	}

	private CBUser user(String id, String username, Set<String> roles) {
		return new CBUser(id, username, "pass", roles, "email", "First", "Last");
	}

	private Set<String> authorities(AbstractAuthenticationToken authentication) {
		return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
	}
}
