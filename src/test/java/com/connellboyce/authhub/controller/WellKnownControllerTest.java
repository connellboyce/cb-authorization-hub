package com.connellboyce.authhub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WellKnownControllerTest {

	@Autowired
	private MockMvc mockMvc;

	// Asserting against the real static resource (rather than a hardcoded literal
	// or just an HTTP 200) proves the controller actually served the file's real
	// content -- not an empty body, an error page, or stale copy-pasted text.
	private static String readStaticResource(String fileName) throws Exception {
		return new String(new ClassPathResource("static/" + fileName).getInputStream().readAllBytes(),
				StandardCharsets.UTF_8);
	}

	@Test
	void testHumansDotText() {
		try {
			String expectedContents = readStaticResource("humans.txt");

			mockMvc.perform(get("/.well-known/humans.txt"))
					.andExpect(status().is(200))
					.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
					.andExpect(content().string(expectedContents));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testRobotsDotText() {
		try {
			String expectedContents = readStaticResource("robots.txt");

			mockMvc.perform(get("/.well-known/robots.txt"))
					.andExpect(status().is(200))
					.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
					.andExpect(content().string(expectedContents));
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
