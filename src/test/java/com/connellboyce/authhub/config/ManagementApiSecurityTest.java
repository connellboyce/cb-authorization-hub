package com.connellboyce.authhub.config;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.repository.UserRepository;
import com.connellboyce.authhub.support.TestTokens;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.connellboyce.authhub.support.TestTokens.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ManagementApiSecurityTest {

	private static final String DEVELOPER_ID = "developer-1";
	private static final String USER_ID = "user-1";
	private static final String UNHANDLED_PATH = "/api/v1/applications/security-probe/unhandled";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JWKSource<SecurityContext> jwkSource;

	@MockBean
	UserRepository userRepository;

	TestTokens tokens;

	@BeforeEach
	void setup() {
		tokens = new TestTokens(jwkSource);
		when(userRepository.findById(DEVELOPER_ID)).thenReturn(Optional.of(new CBUser(DEVELOPER_ID, "dev", "pass", Set.of("USER", "DEVELOPER"), "dev@example.com", "Dev", "Eloper")));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new CBUser(USER_ID, "user", "pass", Set.of("USER"), "user@example.com", "Us", "Er")));
	}

	@Test
	void missingToken_returns401WithBearerChallenge() throws Exception {
		mockMvc.perform(get(UNHANDLED_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
	}

	@Test
	void sessionWithoutToken_returns401() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(
				new UsernamePasswordAuthenticationToken("dev", null, List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")))));

		mockMvc.perform(get(UNHANDLED_PATH).session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authorizedRequest_createsNoSession() throws Exception {
		MvcResult result = mockMvc.perform(get(UNHANDLED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(tokens.userToken(DEVELOPER_ID, "dev", ALL_SCOPES))))
				.andExpect(status().isNotFound())
				.andReturn();

		assertNull(result.getRequest().getSession(false));
		assertNull(result.getResponse().getCookie("JSESSIONID"));
	}

	@Test
	void writeWithoutCsrfToken_isNotRejectedForCsrf() throws Exception {
		mockMvc.perform(post(UNHANDLED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(tokens.userToken(DEVELOPER_ID, "dev", ALL_SCOPES))))
				.andExpect(status().isNotFound());
	}

	@Test
	void missingScope_returns403InsufficientScope() throws Exception {
		mockMvc.perform(post(UNHANDLED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(tokens.userToken(DEVELOPER_ID, "dev", APPLICATIONS_READ))))
				.andExpect(status().isForbidden())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("insufficient_scope")));
	}

	@Test
	void readScopeOnDelete_returns403InsufficientScope() throws Exception {
		mockMvc.perform(delete("/api/v1/applications/app-1").header(HttpHeaders.AUTHORIZATION, bearer(tokens.userToken(DEVELOPER_ID, "dev", APPLICATIONS_READ))))
				.andExpect(status().isForbidden())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("insufficient_scope")));
	}

	@Test
	void nonDeveloper_returns403() throws Exception {
		mockMvc.perform(get(UNHANDLED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(tokens.userToken(USER_ID, "user", ALL_SCOPES))))
				.andExpect(status().isForbidden());
	}

	@Test
	void deletedUser_returns401InvalidToken() throws Exception {
		mockMvc.perform(get(UNHANDLED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(tokens.userToken("deleted-user", "ghost", ALL_SCOPES))))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
	}

	@Test
	void clientCredentialsToken_returns401InvalidToken() throws Exception {
		Instant now = Instant.now();
		String clientToken = tokens.encode(JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(DEVELOPER_ID)
				.issuedAt(now)
				.expiresAt(now.plus(1, ChronoUnit.HOURS))
				.claim("scope", ALL_SCOPES)
				.build());

		mockMvc.perform(get(UNHANDLED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
	}

	@Test
	void expiredToken_returns401InvalidToken() throws Exception {
		Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
		String expired = tokens.encode(tokens.userClaims(DEVELOPER_ID, "dev", ALL_SCOPES)
				.issuedAt(past)
				.expiresAt(past.plus(1, ChronoUnit.HOURS))
				.build());

		mockMvc.perform(get(UNHANDLED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(expired)))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
	}

	@Test
	void tamperedToken_returns401InvalidToken() throws Exception {
		String token = tokens.userToken(DEVELOPER_ID, "dev", ALL_SCOPES);
		String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

		mockMvc.perform(get(UNHANDLED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
	}

	@Test
	void foreignIssuer_returns401InvalidToken() throws Exception {
		String foreign = tokens.encode(tokens.userClaims(DEVELOPER_ID, "dev", ALL_SCOPES).issuer("https://evil.example.com").build());

		mockMvc.perform(get(UNHANDLED_PATH).header(HttpHeaders.AUTHORIZATION, bearer(foreign)))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
	}

	@Test
	void userRegistrationEndpoint_remainsPublic() throws Exception {
		mockMvc.perform(post("/api/v1/user").contentType("application/json").content("{}"))
				.andExpect(result -> {
					int status = result.getResponse().getStatus();
					if (status == 401 || status == 403) {
						throw new AssertionError("Expected /api/v1/user to remain public but got " + status);
					}
				});
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
