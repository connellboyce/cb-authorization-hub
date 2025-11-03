package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.Scope;
import com.connellboyce.authhub.service.ApplicationService;
import com.connellboyce.authhub.service.ScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ScopesControllerTest {
	@Autowired
	MockMvc mockMvc;

	@MockBean
	ScopeService scopeService;

	@MockBean
	ApplicationService applicationService;

	@MockBean
	Authentication authentication;

	@Test
	void testCreateScope_success() {
		Scope scope = new Scope("0", "urn:cb:scope:test", "1");
		when(scopeService.createScope("urn:cb:scope:test", "1"))
				.thenReturn(scope);

		when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
				.thenReturn(true);

		try {
			mockMvc.perform(post("/portal/operation/scope")
							.with(csrf())
							.with(user("user123").roles("DEVELOPER"))
							.param("name", "urn:cb:scope:test")
							.param("applicationId", "1"))
					.andExpect(status().is(302))
					.andExpect(redirectedUrl("/portal/applications/1"))
					.andExpect(flash().attribute("success", "Scope created successfully!"));
		} catch (Exception e) {
			fail("Encountered exception when creating an application: " + e.getMessage());
		}
	}

	@Test
	void testCreateScope_missingCsrfToken() {
		Scope scope = new Scope("0", "urn:cb:scope:test", "1");
		when(scopeService.createScope("urn:cb:scope:test", "1"))
				.thenReturn(scope);

		try {
			mockMvc.perform(post("/portal/operation/scope")
							.with(user("user123").roles("DEVELOPER"))
							.param("name", "urn:cb:scope:test")
							.param("applicationId", "1"))
					.andExpect(status().is(403));
		} catch (Exception e) {
			fail("Encountered exception when creating an application: " + e.getMessage());
		}
	}

	@Test
	void testCreateScope_insufficientRole() {
		Scope scope = new Scope("0", "urn:cb:scope:test", "1");
		when(scopeService.createScope("urn:cb:scope:test", "1"))
				.thenReturn(scope);

		try {
			mockMvc.perform(post("/portal/operation/scope")
							.with(csrf())
							.with(user("user123").roles("USER"))
							.param("name", "urn:cb:scope:test")
							.param("applicationId", "1"))
					.andExpect(status().is(403));
		} catch (Exception e) {
			fail("Encountered exception when creating an application: " + e.getMessage());
		}
	}

	@Test
	void testCreateScope_unauthenticated() {
		Scope scope = new Scope("0", "urn:cb:scope:test", "1");
		when(scopeService.createScope("urn:cb:scope:test", "1"))
				.thenReturn(scope);

		try {
			mockMvc.perform(post("/portal/operation/scope")
							.with(csrf())
							.param("name", "urn:cb:scope:test")
							.param("applicationId", "1"))
					.andExpect(status().is(401));
		} catch (Exception e) {
			fail("Encountered exception when creating an application: " + e.getMessage());
		}
	}

	@Test
	void testCreateScope_identityDoesNotOwnResource() {
		Scope scope = new Scope("0", "urn:cb:scope:test", "1");
		when(scopeService.createScope("urn:cb:scope:test", "1"))
				.thenReturn(scope);

		when(applicationService.validateApplicationOwnership(any(Authentication.class), eq("1")))
				.thenReturn(false);

		try {
			mockMvc.perform(post("/portal/operation/scope")
							.with(csrf())
							.with(user("user123").roles("DEVELOPER"))
							.param("name", "urn:cb:scope:test")
							.param("applicationId", "2"))
					.andExpect(status().is(403));
		} catch (Exception e) {
			fail("Encountered exception when creating an application: " + e.getMessage());
		}
	}
}