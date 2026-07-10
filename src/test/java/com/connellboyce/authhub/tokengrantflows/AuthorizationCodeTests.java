package com.connellboyce.authhub.tokengrantflows;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

	@MockBean
	private UserService userService;

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
		when(passwordEncoder.matches(Mockito.matches(TEST_CLIENT_SECRET), Mockito.any())).thenReturn(true);

		RegisteredClient mockClient = RegisteredClient.withId(TEST_CLIENT_ID)
				.clientId(TEST_CLIENT_ID)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.redirectUri(TEST_REDIRECT_URI)
				.scope(TEST_SCOPE)
				.scope("openid")
				.scope("profile")
				.scope("offline_access")
				.build();

		when(registeredClientRepository.findByClientId(TEST_CLIENT_ID))
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
			when(userService.getCBUserByUsername("alice"))
					.thenReturn(new CBUser("1", "alice", "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

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
			when(userService.getCBUserByUsername("alice"))
					.thenReturn(new CBUser("1", "alice", "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

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
			when(userService.getCBUserByUsername("alice"))
					.thenReturn(new CBUser("1", "alice", "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

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
			when(userService.getCBUserByUsername("alice"))
					.thenReturn(new CBUser("1", "alice", "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

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

	@Test
	void testAuthorizationCode_basic_roundTrip_success() {
		try {
			when(userService.getCBUserByUsername("alice"))
					.thenReturn(new CBUser("1", "alice", "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

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

			String basicAuth = Base64.getEncoder()
					.encodeToString((TEST_CLIENT_ID + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
							}})
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("code", code)
					)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists())
					.andExpect(jsonPath("$.scope").value(TEST_SCOPE))
					.andExpect(jsonPath("$.token_type").value("Bearer"))
					.andExpect(jsonPath("$.expires_in").isNumber());

		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_basic_whenClientOnlyAllowsPost_shouldFail() {
		String postOnlyClientId = "post-only-authcode-client";
		RegisteredClient postOnlyClient = RegisteredClient.withId(postOnlyClientId)
				.clientId(postOnlyClientId)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(TEST_REDIRECT_URI)
				.scope(TEST_SCOPE)
				.build();

		when(registeredClientRepository.findByClientId(postOnlyClientId))
				.thenReturn(postOnlyClient);

		String basicAuth = Base64.getEncoder()
				.encodeToString((postOnlyClientId + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
							}})
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("code", "irrelevant-code")
					)
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_post_whenClientOnlyAllowsBasic_shouldFail() {
		String basicOnlyClientId = "basic-only-authcode-client";
		RegisteredClient basicOnlyClient = RegisteredClient.withId(basicOnlyClientId)
				.clientId(basicOnlyClientId)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(TEST_REDIRECT_URI)
				.scope(TEST_SCOPE)
				.build();

		when(registeredClientRepository.findByClientId(basicOnlyClientId))
				.thenReturn(basicOnlyClient);

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("client_id", basicOnlyClientId)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("code", "irrelevant-code")
					)
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_reusedCode_shouldFail() {
		try {
			when(userService.getCBUserByUsername("alice"))
					.thenReturn(new CBUser("1", "alice", "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

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
					.andExpect(jsonPath("$.access_token").exists());

			mockMvc.perform(post(TOKEN_ENDPOINT)
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("client_id", TEST_CLIENT_ID)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("code", code)
					)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_grant"));

		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	private static String sha256CodeChallenge(String codeVerifier) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
	}

	@Test
	void testAuthorizationCode_pkce_correctVerifier_success() {
		try {
			String pkceClientId = "pkce-client";
			RegisteredClient pkceClient = RegisteredClient.withId(pkceClientId)
					.clientId(pkceClientId)
					.clientSecret("test")
					.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
					.redirectUri(TEST_REDIRECT_URI)
					.scope(TEST_SCOPE)
					.clientSettings(ClientSettings.builder().requireProofKey(true).build())
					.build();

			when(registeredClientRepository.findByClientId(pkceClientId))
					.thenReturn(pkceClient);
			when(userService.getCBUserByUsername("alice"))
					.thenReturn(new CBUser("1", "alice", "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

			String codeVerifier = "test-code-verifier-with-enough-entropy-1234567890";
			String codeChallenge = sha256CodeChallenge(codeVerifier);

			MvcResult result = mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + pkceClientId + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"code_challenge=" + codeChallenge + "&" +
							"code_challenge_method=S256" + "&" +
							"scope=" + TEST_SCOPE)
							.with(user("alice").password("password").roles("USER")))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=**"))
					.andReturn();

			String redirectUrl = result.getResponse().getRedirectedUrl();
			String code = UriComponentsBuilder.fromUriString(redirectUrl)
					.build()
					.getQueryParams()
					.getFirst("code");
			assertNotNull(code, "Authorization code should not be null");

			mockMvc.perform(post(TOKEN_ENDPOINT)
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("client_id", pkceClientId)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("code", code)
							.param("code_verifier", codeVerifier)
					)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists());

		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_pkce_wrongVerifier_shouldFail() {
		try {
			String pkceClientId = "pkce-client-wrong-verifier";
			RegisteredClient pkceClient = RegisteredClient.withId(pkceClientId)
					.clientId(pkceClientId)
					.clientSecret("test")
					.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
					.redirectUri(TEST_REDIRECT_URI)
					.scope(TEST_SCOPE)
					.clientSettings(ClientSettings.builder().requireProofKey(true).build())
					.build();

			when(registeredClientRepository.findByClientId(pkceClientId))
					.thenReturn(pkceClient);
			when(userService.getCBUserByUsername("alice"))
					.thenReturn(new CBUser("1", "alice", "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

			String codeVerifier = "test-code-verifier-with-enough-entropy-1234567890";
			String codeChallenge = sha256CodeChallenge(codeVerifier);

			MvcResult result = mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + pkceClientId + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"code_challenge=" + codeChallenge + "&" +
							"code_challenge_method=S256" + "&" +
							"scope=" + TEST_SCOPE)
							.with(user("alice").password("password").roles("USER")))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=**"))
					.andReturn();

			String redirectUrl = result.getResponse().getRedirectedUrl();
			String code = UriComponentsBuilder.fromUriString(redirectUrl)
					.build()
					.getQueryParams()
					.getFirst("code");
			assertNotNull(code, "Authorization code should not be null");

			mockMvc.perform(post(TOKEN_ENDPOINT)
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("client_id", pkceClientId)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("code", code)
							.param("code_verifier", "this-does-not-match-the-original-challenge")
					)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_grant"));

		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testAuthorizationCode_pkce_missingVerifier_shouldFail() {
		try {
			String pkceClientId = "pkce-client-missing-verifier";
			RegisteredClient pkceClient = RegisteredClient.withId(pkceClientId)
					.clientId(pkceClientId)
					.clientSecret("test")
					.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
					.redirectUri(TEST_REDIRECT_URI)
					.scope(TEST_SCOPE)
					.clientSettings(ClientSettings.builder().requireProofKey(true).build())
					.build();

			when(registeredClientRepository.findByClientId(pkceClientId))
					.thenReturn(pkceClient);
			when(userService.getCBUserByUsername("alice"))
					.thenReturn(new CBUser("1", "alice", "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

			String codeVerifier = "test-code-verifier-with-enough-entropy-1234567890";
			String codeChallenge = sha256CodeChallenge(codeVerifier);

			MvcResult result = mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
							"response_type=" + "code" + "&" +
							"client_id=" + pkceClientId + "&" +
							"redirect_uri=" + TEST_REDIRECT_URI + "&" +
							"code_challenge=" + codeChallenge + "&" +
							"code_challenge_method=S256" + "&" +
							"scope=" + TEST_SCOPE)
							.with(user("alice").password("password").roles("USER")))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrlPattern(TEST_REDIRECT_URI + "?code=**"))
					.andReturn();

			String redirectUrl = result.getResponse().getRedirectedUrl();
			String code = UriComponentsBuilder.fromUriString(redirectUrl)
					.build()
					.getQueryParams()
					.getFirst("code");
			assertNotNull(code, "Authorization code should not be null");

			mockMvc.perform(post(TOKEN_ENDPOINT)
							.param("grant_type", "authorization_code")
							.param("redirect_uri", TEST_REDIRECT_URI)
							.param("client_id", pkceClientId)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("code", code)
					)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_grant"));

		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}
}
