package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.CBUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthUtilServiceImplTest {
	@InjectMocks
	private AuthUtilServiceImpl authUtilService;

	@Mock
	private UserService userService;

	@Mock
	private Authentication authentication;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testGetUserIdFromAuthentication_jwtAuth_userLevel_success() {
		Jwt jwt = mock(Jwt.class);
		when(authentication.getPrincipal()).thenReturn(jwt);
		when(jwt.getClaimAsString("username")).thenReturn("username");
		when(jwt.getClaimAsString("sub")).thenReturn("user123");

		Optional<String> result = authUtilService.getUserIdFromAuthentication(authentication);

		assertTrue(result.isPresent());
		assertEquals("user123", result.get());
	}

	@Test
	void testGetUserIdFromAuthentication_jwtAuth_serviceLevel_success() {
		Jwt jwt = mock(Jwt.class);
		when(authentication.getPrincipal()).thenReturn(jwt);
		when(jwt.getClaimAsString("username")).thenReturn(null);
		when(jwt.getClaimAsString("sub")).thenReturn("clientId");

		Optional<String> result = authUtilService.getUserIdFromAuthentication(authentication);

		assertFalse(result.isPresent());
	}

	@Test
	void testGetUserIdFromAuthentication_userAuth_success() {
		UserDetails userDetails = mock(UserDetails.class);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userDetails.getUsername()).thenReturn("username");

		CBUser cbUser = new CBUser("abc123", "username", "password", Set.of("USER"), "test@email.com", "Test", "User");
		when(userService.getCBUserByUsername("username")).thenReturn(cbUser);

		Optional<String> result = authUtilService.getUserIdFromAuthentication(authentication);

		assertTrue(result.isPresent());
		assertEquals("abc123", result.get());
		verify(userService).getCBUserByUsername("username");
	}

	@Test
	void testGetUserIdFromAuthentication_unknownPrincipal_returnsEmpty() {
		when(authentication.getPrincipal()).thenReturn("some-random-object");

		Optional<String> result = authUtilService.getUserIdFromAuthentication(authentication);

		assertTrue(result.isEmpty());
	}
}