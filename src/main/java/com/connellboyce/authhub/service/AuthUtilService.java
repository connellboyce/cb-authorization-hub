package com.connellboyce.authhub.service;

import org.springframework.security.core.Authentication;

import java.util.Optional;

public interface AuthUtilService {
	Optional<String> getUserIdFromAuthentication(Authentication authentication);
}
