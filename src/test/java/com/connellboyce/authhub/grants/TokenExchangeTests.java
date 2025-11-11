package com.connellboyce.authhub.grants;

import com.connellboyce.authhub.grant.TokenType;
import com.connellboyce.authhub.model.ActorType;
import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.service.UserService;
import com.nimbusds.jose.util.ArrayUtils;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TokenExchangeTests {
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
	private final String TEST_SCOPE = "openid";
	private final String TEST_USER_ID = "1";
	private final String TEST_USERNAME = "alice";
	private final String TEST_REDIRECT_URI = "http://localhost:8080/callback";

	private String userLevelAccessToken;
	private String serviceLevelAccessToken;


	@BeforeEach
	void setup() throws Exception {
		when(passwordEncoder.matches(Mockito.matches(TEST_CLIENT_SECRET), Mockito.any())).thenReturn(true);

		RegisteredClient mockClient = RegisteredClient.withId(TEST_CLIENT_ID)
				.clientId(TEST_CLIENT_ID)
				.clientSecret("test")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
				.redirectUri(TEST_REDIRECT_URI)
				.scope(TEST_SCOPE)
				.scope("openid")
				.scope("profile")
				.scope("offline_access")
				.build();

		when(registeredClientRepository.findByClientId(TEST_CLIENT_ID))
				.thenReturn(mockClient);

		userLevelAccessToken = getUserLevelAccessToken();
		serviceLevelAccessToken = getServiceLevelAccessToken();
	}

	@Test
	void testTokenExchange_userDelegationByService_success() throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(TOKEN_ENDPOINT)
						.param("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.param("subject_token", userLevelAccessToken)
						.param("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("requested_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("scope", TEST_SCOPE)
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", TEST_CLIENT_SECRET))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").exists())
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.expires_in").isNumber())
				.andReturn();

		String tokenResult =  mvcResult.getResponse().getContentAsString()
				.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

		SignedJWT signedJWT = SignedJWT.parse(tokenResult);
		Map<String, Object> claims = signedJWT.getPayload().toJSONObject();
		assertEquals(TEST_USER_ID, claims.get("sub"), "Subject claim should match user ID from subject token");
		assertEquals(TEST_USERNAME, claims.get("username"), "Username claim should match username from subject token");
		assertEquals(List.of(TEST_SCOPE), claims.get("scope"), "Scope claim should match requested scope");
		assertEquals(TEST_CLIENT_ID, ((Map<String, Object>)claims.get("act")).get("sub"), "Actor claim should contain client ID of the exchanging client");
		assertEquals(ActorType.SERVICE.getValue(), ((Map<String, Object>)claims.get("act")).get("typ"), "Actor claim should be of service type");
		assertNull((Map<String, Object>)((Map<?, ?>) claims.get("act")).get("act"), "There should be no further nesting in the actor claim");
	}

	@Test
	void testTokenExchange_userDelegationByService_nestedExchanges_success() throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(TOKEN_ENDPOINT)
						.param("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.param("subject_token", userLevelAccessToken)
						.param("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("requested_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("scope", TEST_SCOPE)
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", TEST_CLIENT_SECRET))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").exists())
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.expires_in").isNumber())
				.andReturn();

		String tokenResult =  mvcResult.getResponse().getContentAsString()
				.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

		SignedJWT signedJWT = SignedJWT.parse(tokenResult);
		Map<String, Object> claims = signedJWT.getPayload().toJSONObject();
		assertEquals(TEST_USER_ID, claims.get("sub"), "Subject claim should match user ID from subject token");
		assertEquals(TEST_USERNAME, claims.get("username"), "Username claim should match username from subject token");
		assertEquals(List.of(TEST_SCOPE), claims.get("scope"), "Scope claim should match requested scope");
		assertEquals(TEST_CLIENT_ID, ((Map<String, Object>)claims.get("act")).get("sub"), "Actor claim should contain client ID of the exchanging client");
		assertEquals(ActorType.SERVICE.getValue(), ((Map<String, Object>)claims.get("act")).get("typ"), "Actor claim should be of service type");
		assertNull((Map<String, Object>)((Map<?, ?>) claims.get("act")).get("act"), "There should be no further nesting in the actor claim");

		MvcResult mvcResult2 = mockMvc.perform(post(TOKEN_ENDPOINT)
						.param("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.param("subject_token", tokenResult)
						.param("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("requested_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("scope", TEST_SCOPE)
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", TEST_CLIENT_SECRET))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").exists())
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.expires_in").isNumber())
				.andReturn();

		String tokenResult2 =  mvcResult2.getResponse().getContentAsString()
				.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

		SignedJWT signedJWT2 = SignedJWT.parse(tokenResult2);
		Map<String, Object> claims2 = signedJWT2.getPayload().toJSONObject();
		assertEquals(TEST_USER_ID, claims2.get("sub"), "Subject claim should match user ID from subject token");
		assertEquals(TEST_USERNAME, claims2.get("username"), "Username claim should match username from subject token");
		assertEquals(List.of(TEST_SCOPE), claims2.get("scope"), "Scope claim should match requested scope");
		Map<String, Object> act = (Map<String, Object>) claims2.get("act");
		assertEquals(TEST_CLIENT_ID, act.get("sub"), "Actor claim should contain client ID of the exchanging client");
		assertEquals(ActorType.SERVICE.getValue(), act.get("typ"), "Actor claim should be of service type");
		Map<String, Object> nestedAct = (Map<String, Object>) act.get("act");
		assertEquals(TEST_CLIENT_ID, nestedAct.get("sub"), "Actor claim should contain client ID of the exchanging client");
		assertEquals(ActorType.SERVICE.getValue(), nestedAct.get("typ"), "Actor claim should be of service type");
		assertNull( nestedAct.get("act"), "There should be no further nesting in the actor claim");
	}

	@Test
	void testTokenExchange_serviceDelegationByService_success() throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(TOKEN_ENDPOINT)
						.param("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.param("subject_token", serviceLevelAccessToken)
						.param("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("requested_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("scope", TEST_SCOPE)
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", TEST_CLIENT_SECRET))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").exists())
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.expires_in").isNumber())
				.andReturn();

		String tokenResult =  mvcResult.getResponse().getContentAsString()
				.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

		SignedJWT signedJWT = SignedJWT.parse(tokenResult);
		Map<String, Object> claims = signedJWT.getPayload().toJSONObject();
		assertEquals(TEST_CLIENT_ID, claims.get("sub"), "Subject claim should match client ID from subject token");
		assertNull(claims.get("username"), "Username claim should be null");
		assertEquals(List.of(TEST_SCOPE), claims.get("scope"), "Scope claim should match requested scope");
		assertEquals(TEST_CLIENT_ID, ((Map<String, Object>)claims.get("act")).get("sub"), "Actor claim should contain client ID of the exchanging client");
		assertEquals(ActorType.SERVICE.getValue(), ((Map<String, Object>)claims.get("act")).get("typ"), "Actor claim should be of service type");
		assertNull((Map<String, Object>)((Map<?, ?>) claims.get("act")).get("act"), "There should be no further nesting in the actor claim");
	}

	@Test
	void testTokenExchange_unsupportedSubjectTokenType() throws Exception {
		mockMvc.perform(post(TOKEN_ENDPOINT)
						.param("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.param("subject_token", serviceLevelAccessToken)
						.param("subject_token_type", "unsupported")
						.param("requested_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("scope", TEST_SCOPE)
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", TEST_CLIENT_SECRET))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.access_token").doesNotExist())
				.andExpect(jsonPath("$.token_type").doesNotExist())
				.andExpect(jsonPath("$.expires_in").doesNotExist())
				.andExpect(jsonPath("$.error_description").value("OAuth 2.0 Token Exchange parameter: subject_token_type"))
				.andExpect(jsonPath("$.error").value("unsupported_token_type"))
				.andExpect(jsonPath("$.error_uri").exists());
	}

	@Test
	void testTokenExchange_unsupportedRequestedTokenType() throws Exception {
		mockMvc.perform(post(TOKEN_ENDPOINT)
						.param("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.param("subject_token", serviceLevelAccessToken)
						.param("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("requested_token_type", "unsupported")
						.param("scope", TEST_SCOPE)
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", TEST_CLIENT_SECRET))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.access_token").doesNotExist())
				.andExpect(jsonPath("$.token_type").doesNotExist())
				.andExpect(jsonPath("$.expires_in").doesNotExist())
				.andExpect(jsonPath("$.error_description").value("OAuth 2.0 Token Exchange parameter: requested_token_type"))
				.andExpect(jsonPath("$.error").value("unsupported_token_type"))
				.andExpect(jsonPath("$.error_uri").exists());
	}

	@Test
	void testTokenExchange_invalidSubjectToken() throws Exception {
		mockMvc.perform(post(TOKEN_ENDPOINT)
						.param("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
						.param("subject_token", "invalid")
						.param("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("requested_token_type", "urn:ietf:params:oauth:token-type:access_token")
						.param("scope", TEST_SCOPE)
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", TEST_CLIENT_SECRET))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.access_token").doesNotExist())
				.andExpect(jsonPath("$.token_type").doesNotExist())
				.andExpect(jsonPath("$.expires_in").doesNotExist())
				.andExpect(jsonPath("$.error").value("invalid_grant"));
	}

	private String getUserLevelAccessToken() throws Exception {
		when(userService.getCBUserByUsername(TEST_USERNAME))
				.thenReturn(new CBUser(TEST_USER_ID, TEST_USERNAME, "password", Set.of("USER"), "test@email.com", "Alice", "Smith"));

		MvcResult result = mockMvc.perform(get(AUTHORIZE_ENDPOINT + "?" +
						"response_type=" + "code" + "&" +
						"client_id=" + TEST_CLIENT_ID + "&" +
						"redirect_uri=" + TEST_REDIRECT_URI + "&" +
						"scope=profile")
						.with(user(TEST_USERNAME).password("password").roles("USER")))
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

		MvcResult mvcResult = mockMvc.perform(post(TOKEN_ENDPOINT)
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
				.andExpect(jsonPath("$.scope").value("profile"))
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.expires_in").isNumber())
				.andReturn();

		return mvcResult.getResponse().getContentAsString()
				.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

	}

	private String getServiceLevelAccessToken() throws Exception {
		MvcResult mvcResult = mockMvc.perform(post(TOKEN_ENDPOINT)
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("grant_type", "client_credentials")
						.param("client_id", TEST_CLIENT_ID)
						.param("client_secret", TEST_CLIENT_SECRET)
						.param("scope", "profile"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").exists())
				.andExpect(jsonPath("$.id_token").doesNotExist())
				.andExpect(jsonPath("$.refresh_token").doesNotExist())
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.expires_in").isNumber())
				.andReturn();

		return mvcResult.getResponse().getContentAsString()
				.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
	}
}
