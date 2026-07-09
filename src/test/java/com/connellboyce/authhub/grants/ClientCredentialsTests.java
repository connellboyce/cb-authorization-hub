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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

	// --- Client authentication method enforcement ---
	// Spring's ClientSecretAuthenticationProvider must reject a client authenticating
	// with a method it isn't registered for (RFC 6749 3.2.1). A previous production
	// defect (a misplaced custom AuthenticationProvider) silently bypassed this check
	// for every client regardless of its registered ClientAuthenticationMethod.

	@Test
	void testClientCredentials_basic_whenClientOnlyAllowsPost_shouldFail() {
		String postOnlyClientId = "post-only-client";
		RegisteredClient postOnlyClient = RegisteredClient.withId(postOnlyClientId)
				.clientId(postOnlyClientId)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope(TEST_SCOPE)
				.build();

		Mockito.when(registeredClientRepository.findByClientId(postOnlyClientId))
				.thenReturn(postOnlyClient);

		String basicAuth = Base64.getEncoder()
				.encodeToString((postOnlyClientId + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

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
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_post_whenClientOnlyAllowsBasic_shouldFail() {
		String basicOnlyClientId = "basic-only-client";
		RegisteredClient basicOnlyClient = RegisteredClient.withId(basicOnlyClientId)
				.clientId(basicOnlyClientId)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope(TEST_SCOPE)
				.build();

		Mockito.when(registeredClientRepository.findByClientId(basicOnlyClientId))
				.thenReturn(basicOnlyClient);

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("client_id", basicOnlyClientId)
							.param("client_secret", TEST_CLIENT_SECRET)
							.param("scope", TEST_SCOPE))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_basic_expiredClientSecret_shouldFail() {
		String expiredSecretClientId = "expired-secret-client";
		RegisteredClient expiredSecretClient = RegisteredClient.withId(expiredSecretClientId)
				.clientId(expiredSecretClientId)
				.clientSecret("test")
				.clientSecretExpiresAt(Instant.now().minusSeconds(60))
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope(TEST_SCOPE)
				.build();

		Mockito.when(registeredClientRepository.findByClientId(expiredSecretClientId))
				.thenReturn(expiredSecretClient);

		String basicAuth = Base64.getEncoder()
				.encodeToString((expiredSecretClientId + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

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
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	// --- Malformed HTTP Basic Authorization header handling ---

	@Test
	void testClientCredentials_basic_malformedBase64_shouldFail() {
		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic not-valid-base64!!");
							}})
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("scope", TEST_SCOPE))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_request"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_basic_missingColon_shouldFail() {
		String noColon = Base64.getEncoder()
				.encodeToString("just-a-client-id-no-secret".getBytes(StandardCharsets.UTF_8));

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + noColon);
							}})
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("scope", TEST_SCOPE))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_request"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	@Test
	void testClientCredentials_basic_emptySecretPortion_shouldFail() {
		String emptySecret = Base64.getEncoder()
				.encodeToString((TEST_CLIENT_ID + ":").getBytes(StandardCharsets.UTF_8));

		try {
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + emptySecret);
							}})
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("scope", TEST_SCOPE))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_request"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}

	// --- RFC 6749 Appendix B: Basic auth credentials are percent-decoded after
	// base64-decoding, but POST body params are not given a second decode pass.
	// A client secret containing URL-reserved characters (e.g. '+', which decodes
	// to a space) will therefore authenticate successfully over client_secret_post
	// but fail over client_secret_basic. This is spec-correct Spring Authorization
	// Server behavior, not a bug -- but it means client secrets must be restricted
	// to a URL-safe charset if client_secret_basic is supported. This test documents
	// that behavioral difference so a future change to secret generation/validation
	// doesn't silently reintroduce it.
	@Test
	void testClientCredentials_secretWithReservedCharacter_behavesDifferentlyBetweenBasicAndPost() {
		BCryptPasswordEncoder realEncoder = new BCryptPasswordEncoder();
		String specialCharClientId = "special-char-client";
		String secretWithPlus = "sec+ret";
		String encodedSecret = realEncoder.encode(secretWithPlus);

		Mockito.when(passwordEncoder.matches(Mockito.anyString(), Mockito.eq(encodedSecret)))
				.thenAnswer(invocation -> realEncoder.matches(invocation.getArgument(0), encodedSecret));

		RegisteredClient specialCharClient = RegisteredClient.withId(specialCharClientId)
				.clientId(specialCharClientId)
				.clientSecret(encodedSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope(TEST_SCOPE)
				.build();

		Mockito.when(registeredClientRepository.findByClientId(specialCharClientId))
				.thenReturn(specialCharClient);

		try {
			// POST: the raw secret is used as-is -> matches.
			mockMvc.perform(post(TOKEN_ENDPOINT)
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("client_id", specialCharClientId)
							.param("client_secret", secretWithPlus)
							.param("scope", TEST_SCOPE))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.access_token").exists());

			// Basic: '+' is percent-decoded to a space, corrupting the secret -> fails.
			String basicAuth = Base64.getEncoder()
					.encodeToString((specialCharClientId + ":" + secretWithPlus).getBytes(StandardCharsets.UTF_8));

			mockMvc.perform(post(TOKEN_ENDPOINT)
							.headers(new HttpHeaders() {{
								add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
							}})
							.contentType(MediaType.APPLICATION_FORM_URLENCODED)
							.param("grant_type", "client_credentials")
							.param("scope", TEST_SCOPE))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.access_token").doesNotExist())
					.andExpect(jsonPath("$.error").value("invalid_client"));
		} catch (Exception e) {
			fail("Exception during test execution: " + e.getMessage());
		}
	}
}
