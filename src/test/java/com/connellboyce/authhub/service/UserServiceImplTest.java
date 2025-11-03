package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplTest {
	@InjectMocks
	private UserServiceImpl userService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testCreateUser_success() {
		when(passwordEncoder.encode("password123"))
				.thenReturn("encodedPassword123");

		UserDetails user = userService.createUser("username", "password123", "test@email.com", "First", "Last");
		assertNotNull(user);
		assertEquals("username", user.getUsername());
		assertEquals("encodedPassword123", user.getPassword());
		assertTrue(user.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("USER")));
	}

	@Test
	void testCreateUser_uuidDuplicate() {
		when(userRepository.findById(anyString()))
				.thenReturn(Optional.of(new CBUser("1", "username", "password", Set.of("USER"), "test@email.com", "First", "Last")));

		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", "password123", "test@email.com", "First", "Last"));
	}

	@Test
	void testCreateUser_usernameDuplicate() {
		when(userRepository.findByUsername(anyString()))
				.thenReturn(Optional.of(new CBUser("1", "username", "password", Set.of("USER"), "test@email.com", "First", "Last")));

		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", "password123", "test@email.com", "First", "Last"));
	}

	@Test
	void testCreateUser_emailDuplicate() {
		when(userRepository.findByEmail(anyString()))
				.thenReturn(Optional.of(new CBUser("1", "username", "password", Set.of("USER"), "test@email.com", "First", "Last")));

		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", "password123", "test@email.com", "First", "Last"));
	}

	@Test
	void testCreateUser_emptyOrNullInputs() {
		assertThrows(IllegalArgumentException.class, () -> userService.createUser("", "password123", "test@email.com", "First", "Last"));
		assertThrows(IllegalArgumentException.class, () -> userService.createUser(null, "password123", "test@email.com", "First", "Last"));
		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", "", "test@email.com", "First", "Last"));
		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", null, "test@email.com", "First", "Last"));
		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", "password123", "", "First", "Last"));
		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", "password123", null, "First", "Last"));
		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", "password123", "test@email.com", "", "Last"));
		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", "password123", "test@email.com", null, "Last"));
		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", "password123", "test@email.com", "First", ""));
		assertThrows(IllegalArgumentException.class, () -> userService.createUser("username", "password123", "test@email.com", "First", null));
	}

	@Test
	void testGetCBUserByUsername_found() {
		when(userRepository.findByUsername("username"))
				.thenReturn(Optional.of(new CBUser("1", "username", "password", Set.of("USER"), "test@email.com", "First", "Last")));

		CBUser user = userService.getCBUserByUsername("username");
		assertNotNull(user);
		assertEquals("username", user.getUsername());
		assertEquals("1", user.getId());
		assertEquals("password", user.getPassword());
		assertTrue(user.getRoles().contains("USER"));
		assertEquals("test@email.com", user.getEmail());
		assertEquals("First", user.getFirstName());
		assertEquals("Last", user.getLastName());
	}

	@Test
	void testGetCBUserByUsername_notFound() {
		when(userRepository.findByUsername("nonexistent"))
				.thenReturn(Optional.empty());

		CBUser user = userService.getCBUserByUsername("nonexistent");
		assertNull(user);
	}
}