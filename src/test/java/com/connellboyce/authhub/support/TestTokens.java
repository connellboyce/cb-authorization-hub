package com.connellboyce.authhub.support;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestTokens {
	public static final String ISSUER = "https://auth.connellboyce.com";
	public static final String CLIENTS_READ = "urn:cb:scope:authhub:clients#read";
	public static final String CLIENTS_WRITE = "urn:cb:scope:authhub:clients#write";
	public static final String APPLICATIONS_READ = "urn:cb:scope:authhub:applications#read";
	public static final String APPLICATIONS_WRITE = "urn:cb:scope:authhub:applications#write";
	public static final String SCOPES_READ = "urn:cb:scope:authhub:scopes#read";
	public static final String ALL_SCOPES = String.join(" ", CLIENTS_READ, CLIENTS_WRITE, APPLICATIONS_READ, APPLICATIONS_WRITE, SCOPES_READ);

	private final JwtEncoder encoder;

	public static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor developer(String subject, String... scopes) {
		List<GrantedAuthority> authorities = new ArrayList<>(List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")));
		Arrays.stream(scopes).map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope)).forEach(authorities::add);
		return SecurityMockMvcRequestPostProcessors.jwt()
				.jwt(jwt -> jwt.subject(subject).claim("username", subject))
				.authorities(authorities);
	}

	public TestTokens(JWKSource<SecurityContext> jwkSource) {
		this.encoder = new NimbusJwtEncoder(jwkSource);
	}

	public JwtClaimsSet.Builder userClaims(String subject, String username, String scope) {
		Instant now = Instant.now();
		return JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(subject)
				.issuedAt(now)
				.expiresAt(now.plus(1, ChronoUnit.HOURS))
				.claim("username", username)
				.claim("scope", scope);
	}

	public String userToken(String subject, String username, String scope) {
		return encode(userClaims(subject, username, scope).build());
	}

	public String encode(JwtClaimsSet claims) {
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
	}
}
