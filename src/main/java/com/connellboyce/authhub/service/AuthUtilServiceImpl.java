package com.connellboyce.authhub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthUtilServiceImpl implements AuthUtilService {
	@Autowired
	private UserService userService;

	@Override
	public Optional<String> getUserIdFromAuthentication(Authentication authentication) {
		String username = null;

		Object principal = authentication.getPrincipal();
		if (principal instanceof Jwt jwt) {
			if (jwt.getClaimAsStringList("role").isEmpty()) {
				return Optional.empty();
			}
			username = jwt.getClaimAsString("sub");
		} else if (principal instanceof UserDetails user) {
			username = user.getUsername();
		} else {
			return Optional.empty();
		}

		return Optional.of(userService.getCBUserByUsername(username).getId());
	}
}
