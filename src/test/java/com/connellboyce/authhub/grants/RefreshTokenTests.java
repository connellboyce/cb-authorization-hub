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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class RefreshTokenTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RegisteredClientRepository registeredClientRepository;

	@MockBean
	private PasswordEncoder passwordEncoder;

	@MockBean
	private OAuth2AuthorizationService authorizationService;

	private final String TOKEN_ENDPOINT = "/oauth2/token";
	private final String TEST_CLIENT_ID = "client";
	private final String TEST_CLIENT_SECRET = "secret";

	@BeforeEach
	void setup() {
		Mockito.when(passwordEncoder.matches(Mockito.matches(TEST_CLIENT_SECRET), Mockito.any())).thenReturn(true);

		RegisteredClient mockClient = RegisteredClient.withId(TEST_CLIENT_ID)
				.clientId(TEST_CLIENT_ID)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.redirectUri("http://localhost:8080/callback")
				.scope("offline_access")
				.tokenSettings(TokenSettings.builder()
						.reuseRefreshTokens(false)
						.build())
				.build();

		Mockito.when(registeredClientRepository.findByClientId(TEST_CLIENT_ID))
				.thenReturn(mockClient);

		OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
				"valid-refresh-token",
				Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(600)
		);

		Authentication principal = new UsernamePasswordAuthenticationToken(
				"user", "password", AuthorityUtils.createAuthorityList("ROLE_USER"));

		OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(mockClient)
				.principalName("user")
				.attribute(Principal.class.getName(), principal)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.token(refreshToken)
				.attribute(OAuth2ParameterNames.CLIENT_ID, TEST_CLIENT_ID)
				.build();

		Mockito.when(authorizationService.findByToken("valid-refresh-token", OAuth2TokenType.REFRESH_TOKEN))
				.thenReturn(authorization);
	}

	@Test
	void testRefreshToken_post_invalidToken() throws Exception {
		mockMvc.perform(post(TOKEN_ENDPOINT)
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", TEST_CLIENT_SECRET)
						.param("refresh_token", "invalid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_grant"))
				.andExpect(jsonPath("$.access_token").doesNotExist())
				.andExpect(jsonPath("$.id_token").doesNotExist())
				.andExpect(jsonPath("$.refresh_token").doesNotExist())
				.andExpect(jsonPath("$.token_type").doesNotExist())
				.andExpect(jsonPath("$.expires_in").doesNotExist());
	}

	@Test
	void testRefreshToken_basic_invalidToken() throws Exception {
		String basicAuth = Base64.getEncoder()
				.encodeToString((TEST_CLIENT_ID + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(post(TOKEN_ENDPOINT)
						.headers(new HttpHeaders() {{
							add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
						}})
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("refresh_token", "invalid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_grant"))
				.andExpect(jsonPath("$.access_token").doesNotExist())
				.andExpect(jsonPath("$.id_token").doesNotExist())
				.andExpect(jsonPath("$.refresh_token").doesNotExist())
				.andExpect(jsonPath("$.token_type").doesNotExist())
				.andExpect(jsonPath("$.expires_in").doesNotExist());
	}

	@Test
	void testRefreshToken_post_invalidCredentials() throws Exception {
		mockMvc.perform(post(TOKEN_ENDPOINT)
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", "wrong")
						.param("refresh_token", "invalid"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("invalid_client"))
				.andExpect(jsonPath("$.access_token").doesNotExist())
				.andExpect(jsonPath("$.id_token").doesNotExist())
				.andExpect(jsonPath("$.refresh_token").doesNotExist())
				.andExpect(jsonPath("$.token_type").doesNotExist())
				.andExpect(jsonPath("$.expires_in").doesNotExist());
	}

	@Test
	void testRefreshToken_basic_invalidCredentials() throws Exception {
		String basicAuth = Base64.getEncoder()
				.encodeToString((TEST_CLIENT_ID + ":" + "wrong").getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(post(TOKEN_ENDPOINT)
						.headers(new HttpHeaders() {{
							add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
						}})
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("refresh_token", "invalid"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("invalid_client"))
				.andExpect(jsonPath("$.access_token").doesNotExist())
				.andExpect(jsonPath("$.id_token").doesNotExist())
				.andExpect(jsonPath("$.refresh_token").doesNotExist())
				.andExpect(jsonPath("$.token_type").doesNotExist())
				.andExpect(jsonPath("$.expires_in").doesNotExist());
	}

	@Test
	void testRefreshToken_post_validToken() throws Exception {
		mockMvc.perform(post(TOKEN_ENDPOINT)
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", TEST_CLIENT_SECRET)
						.param("refresh_token", "valid-refresh-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").exists())
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.expires_in").exists())
				.andExpect(jsonPath("$.refresh_token").exists());
	}

	@Test
	void testRefreshToken_basic_validToken() throws Exception {
		String basicAuth = Base64.getEncoder()
				.encodeToString((TEST_CLIENT_ID + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(post(TOKEN_ENDPOINT)
						.headers(new HttpHeaders() {{
							add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
						}})
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("refresh_token", "valid-refresh-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").exists())
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.expires_in").exists())
				.andExpect(jsonPath("$.refresh_token").exists());
	}

	// --- Client authentication method enforcement ---
	// A client registered for only one of client_secret_basic/client_secret_post
	// must be rejected when it attempts the other. See ClientCredentialsTests for
	// the production defect (a misplaced custom AuthenticationProvider) that used
	// to silently bypass this check for every grant type.

	@Test
	void testRefreshToken_basic_whenClientOnlyAllowsPost_shouldFail() throws Exception {
		String postOnlyClientId = "post-only-refresh-client";
		RegisteredClient postOnlyClient = RegisteredClient.withId(postOnlyClientId)
				.clientId(postOnlyClientId)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.scope("offline_access")
				.build();

		Mockito.when(registeredClientRepository.findByClientId(postOnlyClientId))
				.thenReturn(postOnlyClient);

		String basicAuth = Base64.getEncoder()
				.encodeToString((postOnlyClientId + ":" + TEST_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(post(TOKEN_ENDPOINT)
						.headers(new HttpHeaders() {{
							add(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth);
						}})
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("refresh_token", "invalid"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.access_token").doesNotExist())
				.andExpect(jsonPath("$.error").value("invalid_client"));
	}

	@Test
	void testRefreshToken_post_whenClientOnlyAllowsBasic_shouldFail() throws Exception {
		String basicOnlyClientId = "basic-only-refresh-client";
		RegisteredClient basicOnlyClient = RegisteredClient.withId(basicOnlyClientId)
				.clientId(basicOnlyClientId)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.scope("offline_access")
				.build();

		Mockito.when(registeredClientRepository.findByClientId(basicOnlyClientId))
				.thenReturn(basicOnlyClient);

		mockMvc.perform(post(TOKEN_ENDPOINT)
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("client_id", basicOnlyClientId)
						.param("client_secret", TEST_CLIENT_SECRET)
						.param("refresh_token", "invalid"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.access_token").doesNotExist())
				.andExpect(jsonPath("$.error").value("invalid_client"));
	}

	@Test
	void testRefreshToken_clientMissingGrant_shouldFail() throws Exception {
		String noRefreshGrantClientId = "no-refresh-grant-client";
		RegisteredClient mockClient = RegisteredClient.withId(noRefreshGrantClientId)
				.clientId(noRefreshGrantClientId)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("offline_access")
				.build();

		Mockito.when(registeredClientRepository.findByClientId(noRefreshGrantClientId))
				.thenReturn(mockClient);

		// The refresh token must resolve to a real authorization owned by this same
		// client, otherwise the provider fails on the token/ownership check before it
		// ever reaches the "does this client support refresh_token" check.
		OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
				"no-grant-refresh-token",
				Instant.now().minusSeconds(60),
				Instant.now().plusSeconds(600)
		);

		OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(mockClient)
				.principalName("user")
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.token(refreshToken)
				.build();

		Mockito.when(authorizationService.findByToken("no-grant-refresh-token", OAuth2TokenType.REFRESH_TOKEN))
				.thenReturn(authorization);

		mockMvc.perform(post(TOKEN_ENDPOINT)
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "refresh_token")
						.param("client_id", noRefreshGrantClientId)
						.param("client_secret", TEST_CLIENT_SECRET)
						.param("refresh_token", "no-grant-refresh-token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.access_token").doesNotExist())
				.andExpect(jsonPath("$.error").value("unauthorized_client"));
	}
}
