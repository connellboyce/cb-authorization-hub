package com.connellboyce.authhub.grants;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AuthorizationCodeTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RegisteredClientRepository registeredClientRepository;

	@MockBean
	private PasswordEncoder passwordEncoder;

	private final String AUTHORIZE_ENDPOINT = "/oauth2/authorize";
	private final String TOKEN_ENDPOINT = "/oauth2/token";
	private final String TEST_CLIENT_ID = "client";
	private final String TEST_CLIENT_SECRET = "secret";
	private final String TEST_SCOPE = "test";
	private final String TEST_STATE = "state";
	private final String TEST_REDIRECT_URI = "http://localhost:8080/callback";
	private final String OPENID_SCOPES = "openid profile";
	private final String REFRESH_SCOPE = "offline_access";

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
	}

	@Test
	void testAuthorizationCode_redirectToLogin_unauthenticated_success() {
		try {
			mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"scope=" + TEST_SCOPE)
					)
					.andExpect(status().is(302))
					.andExpect(redirectedUrlPattern("**/login"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_redirectToLogin_withState_unauthenticated_success() {
		try {
			mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"state=" + TEST_STATE + "&" +
							"scope=" + TEST_SCOPE)
					)
					.andExpect(status().is(302))
					.andExpect(redirectedUrlPattern("**/login"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_redirectToLogin_unauthenticated_mismatchedRedirectUri() {
		try {
			mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + "wrong" + "&" +
							"scope=" + TEST_SCOPE)
					)
					.andExpect(status().isBadRequest());
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_redirectToLogin_unauthenticated_invalidScope() {
		try {
			mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + "wrong" + "&" +
							"scope=" + "hollow-purple")
					)
					.andExpect(status().isBadRequest());
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_redirectToLogin_unauthenticated_invalidClient() {
		try {
			mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + "wrong" + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"scope=" + TEST_SCOPE)
					)
					.andExpect(status().isBadRequest());
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_redirectToLogin_authenticated_success() {
		try {
			mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"scope=" + TEST_SCOPE)
						.with(user("alice").password("password").roles("USER")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=**"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_redirectToLogin_withState_authenticated_success() {
		try {
			mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"state=" + TEST_STATE + "&" +
							"scope=" + TEST_SCOPE)
						.with(user("alice").password("password").roles("USER")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=**"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_roundTrip_success() {
		try {
			MvcResult result = mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"scope=" + TEST_SCOPE)
							.with(user("alice").password("password").roles("USER")))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=**"))
					.andReturn();

			String redirectUrl = result.getResponse().getRedirectedUrl();
			assertNotNull(redirectUrl, "Resulting redirect URL should not be null");

			String code = UriComponentsBuilder.fromUriString(redirectUrl)
					.build()
					.getQueryParams()
					.getFirst("code");

			assertNotNull(code, "Authorization code should not be null");

			mockMvc.perform(post(TOKEN_ENDPOINT)
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("client_id", TEST_CLIENT_ID)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("code", code)
					)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists())
					.andExpect(jsonPath("$.id_token").doesNotExist())
//					TODO: Update refresh token logic
//					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.scope").value(TEST_SCOPE))
					.andExpect(jsonPath("$.token_type").value("Bearer"))
					.andExpect(jsonPath("$.expires_in").isNumber());

		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_roundTrip_withState_success() {
		try {
			MvcResult result = mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"state=" + TEST_STATE + "&" +
							"scope=" + TEST_SCOPE)
							.with(user("alice").password("password").roles("USER")))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=**"))
					.andReturn();

			String redirectUrl = result.getResponse().getRedirectedUrl();
			assertNotNull(redirectUrl, "Resulting redirect URL should not be null");

			MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(redirectUrl)
					.build()
					.getQueryParams();

			String code = queryParams.getFirst("code");
			String state = queryParams.getFirst("state");

			assertNotNull(code, "Authorization code should not be null");
			assertEquals(state, TEST_STATE);

			mockMvc.perform(post(TOKEN_ENDPOINT)
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("client_id", TEST_CLIENT_ID)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("code", code)
					)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists())
					.andExpect(jsonPath("$.id_token").doesNotExist())
//					TODO: Update refresh token logic
//					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.scope").value(TEST_SCOPE))
					.andExpect(jsonPath("$.token_type").value("Bearer"))
					.andExpect(jsonPath("$.expires_in").isNumber());

		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_roundTrip_openid_success() {
		try {
			MvcResult result = mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"scope=" + TEST_SCOPE + " " + OPENID_SCOPES)
							.with(user("alice").password("password").roles("USER")))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=**"))
					.andReturn();

			String redirectUrl = result.getResponse().getRedirectedUrl();
			assertNotNull(redirectUrl, "Resulting redirect URL should not be null");

			String code = UriComponentsBuilder.fromUriString(redirectUrl)
					.build()
					.getQueryParams()
					.getFirst("code");

			assertNotNull(code, "Authorization code should not be null");

			mockMvc.perform(post(TOKEN_ENDPOINT)
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("client_id", TEST_CLIENT_ID)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("code", code)
					)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists())
					.andExpect(jsonPath("$.id_token").exists())
//					TODO: Update refresh token logic
//					.andExpect(jsonPath("$.refresh_token").doesNotExist())
					.andExpect(jsonPath("$.scope").value(TEST_SCOPE + " " + OPENID_SCOPES))
					.andExpect(jsonPath("$.token_type").value("Bearer"))
					.andExpect(jsonPath("$.expires_in").isNumber());

		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_roundTrip_refresh_success() {
		try {
			MvcResult result = mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + TEST_CLIENT_ID + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"scope=" + TEST_SCOPE + " " + REFRESH_SCOPE)
							.with(user("alice").password("password").roles("USER")))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=**"))
					.andReturn();

			String redirectUrl = result.getResponse().getRedirectedUrl();
			assertNotNull(redirectUrl, "Resulting redirect URL should not be null");

			String code = UriComponentsBuilder.fromUriString(redirectUrl)
					.build()
					.getQueryParams()
					.getFirst("code");

			assertNotNull(code, "Authorization code should not be null");

			mockMvc.perform(post(TOKEN_ENDPOINT)
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("client_id", TEST_CLIENT_ID)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("code", code)
					)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists())
					.andExpect(jsonPath("$.id_token").doesNotExist())
					.andExpect(jsonPath("$.refresh_token").exists())
					.andExpect(jsonPath("$.scope").value(TEST_SCOPE + " " + REFRESH_SCOPE))
					.andExpect(jsonPath("$.token_type").value("Bearer"))
					.andExpect(jsonPath("$.expires_in").isNumber());

		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}
}
