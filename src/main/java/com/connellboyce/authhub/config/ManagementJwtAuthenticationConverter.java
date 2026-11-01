package com.connellboyce.authhub.config;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.HashSet;
import java.util.Set;

public class ManagementJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
	private final UserRepository userRepository;
	private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

	public ManagementJwtAuthenticationConverter(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		CBUser user = userRepository.findById(jwt.getSubject())
				.filter(found -> found.getUsername().equals(jwt.getClaimAsString("username")))
				.orElseThrow(() -> new InvalidBearerTokenException("Token subject is not an active user"));

		Set<GrantedAuthority> authorities = new HashSet<>(scopeAuthoritiesConverter.convert(jwt));
		user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

		return new JwtAuthenticationToken(jwt, authorities, user.getId());
	}
}
