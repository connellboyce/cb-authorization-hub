package com.connellboyce.authhub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WellKnownControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void testHumansDotText() {
		try {
			mockMvc.perform(get("/.well-known/humans.txt"))
					.andExpect(status().is(200));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testRobotsDotText() {
		try {
			mockMvc.perform(get("/.well-known/robots.txt"))
					.andExpect(status().is(200));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testDiscoveryEndpoint() {
		try {
			mockMvc.perform(get("/.well-known/oauth-authorization-server"))
					.andExpect(status().is(200))
					.andExpect(jsonPath("$.authorization_endpoint").exists())
					.andExpect(jsonPath("$.token_endpoint").exists())
					.andExpect(jsonPath("$.token_endpoint_auth_methods_supported").exists())
					.andExpect(jsonPath("$.jwks_uri").exists())
					.andExpect(jsonPath("$.response_types_supported").exists())
					.andExpect(jsonPath("$.grant_types_supported").isArray())
					.andExpect(jsonPath("$.introspection_endpoint").exists())
					.andExpect(jsonPath("$.code_challenge_methods_supported").isArray())
					.andExpect(jsonPath("$.introspection_endpoint_auth_methods_supported").isArray())
					.andExpect(jsonPath("$.issuer").exists());
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testOidcConfigurationEndpoint() {
		try {
			mockMvc.perform(get("/.well-known/openid-configuration"))
					.andExpect(status().is(200))
					.andExpect(jsonPath("$.authorization_endpoint").exists())
					.andExpect(jsonPath("$.token_endpoint").exists())
					.andExpect(jsonPath("$.token_endpoint_auth_methods_supported").exists())
					.andExpect(jsonPath("$.jwks_uri").exists())
					.andExpect(jsonPath("$.response_types_supported").exists())
					.andExpect(jsonPath("$.grant_types_supported").isArray())
					.andExpect(jsonPath("$.introspection_endpoint").exists())
					.andExpect(jsonPath("$.code_challenge_methods_supported").isArray())
					.andExpect(jsonPath("$.introspection_endpoint_auth_methods_supported").isArray())
					.andExpect(jsonPath("$.issuer").exists());
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}
}
