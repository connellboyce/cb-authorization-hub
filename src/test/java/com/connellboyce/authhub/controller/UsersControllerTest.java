package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UsersControllerTest {
	@Autowired
	MockMvc mockMvc;

	@MockBean
	UserService userService;

	ObjectMapper mapper = new ObjectMapper();

	@Test
	void testCreateScope_success() {
		UserDetails userDetails = new User("user123", "testpassword", List.of(new SimpleGrantedAuthority("ROLE_USER")));
		when(userService.createUser("user123", "testpassword", "test-cbauth@mailinator.com", "Goopy", "Scoopy"))
				.thenReturn(userDetails);

		try {
			mockMvc.perform(post("/api/v1/user")
							.contentType("application/json")
							.content(mapper.writeValueAsString(Map.of(
									"username", "user123",
									"password", "testpassword",
									"email","test-cbauth@mailinator.com",
									"firstName", "Goopy",
									"lastName", "Scoopy"
							))))
					.andExpect(status().is(200));
		} catch (Exception e) {
			fail("Encountered exception when creating a user: " + e.getMessage());
		}
	}

	@Test
	void testCreateScope_failure() {
		when(userService.createUser("user123", "testpassword", "test-cbauth@mailinator.com", "Goopy", "Scoopy"))
				.thenReturn(null);

		try {
			mockMvc.perform(post("/api/v1/user")
							.contentType("application/json")
							.content(mapper.writeValueAsString(Map.of(
									"username", "user123",
									"password", "testpassword",
									"email","test-cbauth@mailinator.com",
									"firstName", "Goopy",
									"lastName", "Scoopy"
							))))
					.andExpect(status().is(500));
		} catch (Exception e) {
			fail("Encountered exception when creating a user: " + e.getMessage());
		}
	}

	@Test
	void testCreateScope_usernameAlreadyExists() {
		when(userService.createUser("user123", "testpassword", "test-cbauth@mailinator.com", "Goopy", "Scoopy"))
				.thenThrow(new IllegalArgumentException("Username already exists"));

		try {
			mockMvc.perform(post("/api/v1/user")
							.contentType("application/json")
							.content(mapper.writeValueAsString(Map.of(
									"username", "user123",
									"password", "testpassword",
									"email","test-cbauth@mailinator.com",
									"firstName", "Goopy",
									"lastName", "Scoopy"
							))))
					.andExpect(status().is(400));
		} catch (Exception e) {
			fail("Encountered exception when creating a user: " + e.getMessage());
		}
	}

	@Test
	void testCreateScope_emailAlreadyExists() {
		when(userService.createUser("user123", "testpassword", "test-cbauth@mailinator.com", "Goopy", "Scoopy"))
				.thenThrow(new IllegalArgumentException("Email already exists"));

		try {
			mockMvc.perform(post("/api/v1/user")
							.contentType("application/json")
							.content(mapper.writeValueAsString(Map.of(
									"username", "user123",
									"password", "testpassword",
									"email","test-cbauth@mailinator.com",
									"firstName", "Goopy",
									"lastName", "Scoopy"
							))))
					.andExpect(status().is(400));
		} catch (Exception e) {
			fail("Encountered exception when creating a user: " + e.getMessage());
		}
	}

}