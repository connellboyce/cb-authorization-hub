package com.connellboyce.authhub.config;

import com.connellboyce.authhub.support.TestTokens;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ConfiguredIssuerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtDecoder jwtDecoder;

	@Autowired
	JWKSource<SecurityContext> jwkSource;

	@Test
	void discovery_advertisesConfiguredIssuerRegardlessOfRequestHost() throws Exception {
		mockMvc.perform(get("/.well-known/oauth-authorization-server"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.issuer").value("https://auth.connellboyce.com"))
				.andExpect(jsonPath("$.registration_endpoint").value("https://auth.connellboyce.com/api/v1/clients"));
	}

	@Test
	void jwtDecoder_rejectsTokenFromLocalhostIssuer() {
		TestTokens tokens = new TestTokens(jwkSource);
		String localToken = tokens.encode(tokens.userClaims("user-1", "user", "openid").issuer("http://localhost:2121").build());

		assertThrows(JwtValidationException.class, () -> jwtDecoder.decode(localToken));
	}
}
