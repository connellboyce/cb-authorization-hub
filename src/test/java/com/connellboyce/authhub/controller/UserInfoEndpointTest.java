package com.connellboyce.authhub.controller;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserInfoEndpointTest {

	private final String AUTHORIZE_ENDPOINT = "/oauth2/authorize";
	private final String TOKEN_ENDPOINT = "/oauth2/token";
	private final String TEST_CLIENT_ID = "client";
	private final String TEST_CLIENT_SECRET = "secret";
	private final String TEST_SCOPE = "test";
	private final String TEST_STATE = "state";
	private final String TEST_REDIRECT_URI = "http://localhost:8080/callback";
	private final String OPENID_SCOPES = "openid profile";

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RegisteredClientRepository registeredClientRepository;

	@MockBean
	private PasswordEncoder passwordEncoder;

	@MockBean
	UserService userService;

	@BeforeEach
	void setup() {
		Mockito.when(passwordEncoder.matches(Mockito.matches(TEST_CLIENT_SECRET), Mockito.any())).thenReturn(true);

		RegisteredClient mockClient = RegisteredClient.withId(TEST_CLIENT_ID)
				.clientId(TEST_CLIENT_ID)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.redirectUri(TEST_REDIRECT_URI)
				.scope(TEST_SCOPE)
				.scope("openid")
				.scope("profile")
				.scope("offline_access")
				.build();

		Mockito.when(registeredClientRepository.findByClientId(TEST_CLIENT_ID))
				.thenReturn(mockClient);

		CBUser mockUser = new CBUser("123", "alice", "testpassword", Set.of("USER"), "alice@example.com", "Alice", "Smith");

		Mockito.when(userService.getCBUserByUsername(Mockito.anyString()))
				.thenReturn(mockUser);
	}

	@Test
	void testUserInfoEndpoint_returnsUserInfo() throws Exception {
		String userLevelToken = generateUserLevelToken();

		mockMvc.perform(get("/userinfo")
						.header("Authorization", "Bearer " + userLevelToken)
						.header("Accept", "application/json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sub").exists())
				.andExpect(jsonPath("$.name").exists())
				.andExpect(jsonPath("$.preferred_username").exists())
				.andExpect(jsonPath("$.given_name").exists())
				.andExpect(jsonPath("$.family_name").exists())
				.andExpect(jsonPath("$.email").exists());
	}

	@Test
	void testUserInfoEndpoint_invalid() throws Exception {
		mockMvc.perform(get("/userinfo")
						.header("Authorization", "Bearer eyJ.InvalidToken")
						.header("Accept", "application/json"))
				.andExpect(status().isUnauthorized());
	}

	private String generateUserLevelToken() {
		try {
			when(userService.getCBUserByUsername("alice"))
					.thenReturn(new CBUser("1", "alice", "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

			MvcResult codeResult = mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"scope=" + TEST_SCOPE + " " + OPENID_SCOPES)
							.with(user("alice").password("password").roles("USER")))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=**"))
					.andReturn();

			String redirectUrl = codeResult.getResponse().getRedirectedUrl();
			assertNotNull(redirectUrl, "Resulting redirect URL should not be null");

			String code = UriComponentsBuilder.fromUriString(redirectUrl)
					.build()
					.getQueryParams()
					.getFirst("code");

			assertNotNull(code, "Authorization code should not be null");

			MvcResult tokenResult = mockMvc.perform(post(TOKEN_ENDPOINT)
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("client_id", TEST_CLIENT_ID)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("code", code)
					)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists())
					.andExpect(jsonPath("$.token_type").value("Bearer"))
					.andExpect(jsonPath("$.expires_in").isNumber())
					.andReturn();

			String json = tokenResult.getResponse().getContentAsString();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json);

			String accessToken = node.get("access_token").asText();
			assertNotNull(accessToken, "Access token should not be null");

			return accessToken;
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
		return null;
	}
}
