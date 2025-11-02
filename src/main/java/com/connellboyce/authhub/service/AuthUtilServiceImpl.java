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
		Object principal = authentication.getPrincipal();
		if (principal instanceof Jwt jwt) {
			return jwt.getClaimAsString("username") != null ? Optional.ofNullable(jwt.getClaimAsString("sub")) : Optional.empty();
		} else if (principal instanceof UserDetails user) {
			String username = user.getUsername();
			return Optional.of(userService.getCBUserByUsername(username).getId());
		} else {
			return Optional.empty();
		}
	}
}
