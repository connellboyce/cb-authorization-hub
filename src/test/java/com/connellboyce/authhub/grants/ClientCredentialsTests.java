package com.connellboyce.authhub.grants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ClientCredentialsTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RegisteredClientRepository registeredClientRepository;

	@MockBean
	private PasswordEncoder passwordEncoder;

	private final String TOKEN_ENDPOINT = "/oauth2/token";
	private final String TEST_CLIENT_ID = "client";
	private final String TEST_CLIENT_SECRET = "secret";
	private final String TEST_SCOPE = "test";
	private final String OPENID_SCOPES = "openid profile offline_access";

	@BeforeEach
	void setup() {
		Mockito.when(passwordEncoder.matches(Mockito.matches(TEST_CLIENT_SECRET), Mockito.any())).thenReturn(true);

		RegisteredClient mockClient = RegisteredClient.withId(TEST_CLIENT_ID)
				.clientId(TEST_CLIENT_ID)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope(TEST_SCOPE)
				.scope("openid")
				.scope("profile")
				.scope("offline_access")
				.build();

		Mockito.when(registeredClientRepository.findByClientId(TEST_CLIENT_ID))
				.thenReturn(mockClient);
	}

	@Test
	void testClientCredentials_post_success() {
		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("client_id", TEST_CLIENT_ID)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("scope", TEST_SCOPE))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.token_type").value("Bearer"))
					.andExpect(jsonPath("$.expires_in").isNumber());
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_basic_success() {
		String basicAuth = Base64.getEncoder()
				.encodeToString((TEST_CLIENT_ID + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
							}})
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("scope", TEST_SCOPE))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.token_type").value("Bearer"))
					.andExpect(jsonPath("$.expires_in").isNumber());
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_withOpenId_post_success() {
		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("client_id", TEST_CLIENT_ID)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("scope", TEST_SCOPE + " " + OPENID_SCOPES))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.token_type").value("Bearer"))
					.andExpect(jsonPath("$.expires_in").isNumber());

		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_withOpenId_basic_success() {
		String basicAuth = Base64.getEncoder()
				.encodeToString((TEST_CLIENT_ID + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
							}})
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("scope", TEST_SCOPE + " " + OPENID_SCOPES))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.token_type").value("Bearer"))
					.andExpect(jsonPath("$.expires_in").isNumber());
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_basic_invalidClientId() {
		String basicAuth = Base64.getEncoder()
				.encodeToString(("wrong" + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
							}})
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("scope", TEST_SCOPE))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.expires_in").doesNotExist())
					.andExpect(jsonPath("$.token_type").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_post_invalidClientId() {
		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("client_id", "wrong")
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("scope", TEST_SCOPE))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.expires_in").doesNotExist())
					.andExpect(jsonPath("$.token_type").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_basic_invalidClientSecret() {
		String basicAuth = Base64.getEncoder()
				.encodeToString((TEST_CLIENT_ID + ":" + "wrong").getBytes(StandardCharsets.UTF_8));

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
							}})
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("scope", TEST_SCOPE))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.expires_in").doesNotExist())
					.andExpect(jsonPath("$.token_type").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_post_invalidClientSecret() {
		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("client_id", TEST_CLIENT_ID)
							.param("client_secret", "wrong")
							.param("scope", TEST_SCOPE))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.expires_in").doesNotExist())
					.andExpect(jsonPath("$.token_type").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_basic_invalidClientIdAndSecret() {
		String basicAuth = Base64.getEncoder()
				.encodeToString(("wrong" + ":" + "wrong").getBytes(StandardCharsets.UTF_8));

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
							}})
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("scope", TEST_SCOPE))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.expires_in").doesNotExist())
					.andExpect(jsonPath("$.token_type").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_post_invalidClientIdAndSecret() {
		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("client_id", "wrong")
							.param("client_secret", "wrong")
							.param("scope", TEST_SCOPE))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.expires_in").doesNotExist())
					.andExpect(jsonPath("$.token_type").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_basic_clientMissingGrant() {
		String invalidGrantClientId = "invalid-grant";
		RegisteredClient mockClient = RegisteredClient.withId(invalidGrantClientId)
				.clientId(invalidGrantClientId)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.scope(TEST_SCOPE)
				.scope("openid")
				.scope("profile")
				.scope("offline_access")
				.build();

		Mockito.when(registeredClientRepository.findByClientId(invalidGrantClientId))
				.thenReturn(mockClient);

		String basicAuth = Base64.getEncoder()
				.encodeToString((invalidGrantClientId + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
							}})
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("client_id", invalidGrantClientId)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("scope", TEST_SCOPE))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.expires_in").doesNotExist())
					.andExpect(jsonPath("$.token_type").doesNotExist())
					.andExpect(jsonPath("$.error").value("unauthorized_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_post_clientMissingGrant() {
		String invalidGrantClientId = "invalid-grant";
		RegisteredClient mockClient = RegisteredClient.withId(invalidGrantClientId)
				.clientId(invalidGrantClientId)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.scope(TEST_SCOPE)
				.scope("openid")
				.scope("profile")
				.scope("offline_access")
				.build();

		Mockito.when(registeredClientRepository.findByClientId(invalidGrantClientId))
				.thenReturn(mockClient);

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("client_id", invalidGrantClientId)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("scope", TEST_SCOPE))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.expires_in").doesNotExist())
					.andExpect(jsonPath("$.token_type").doesNotExist())
					.andExpect(jsonPath("$.error").value("unauthorized_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}
}
