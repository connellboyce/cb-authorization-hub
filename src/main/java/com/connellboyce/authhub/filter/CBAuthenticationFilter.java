package com.connellboyce.authhub.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class CBAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

	public CBAuthenticationFilter(AuthenticationManager authenticationManager) {
		super(authenticationManager);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
		if (!request.getMethod().equals("POST")) {
			throw new AuthenticationException("Authentication method not supported: " + request.getMethod()) {};
		}

		UsernamePasswordAuthenticationToken authRequest = getAuthRequest(request);
		setDetails(request, authRequest);
		return this.getAuthenticationManager().authenticate(authRequest);
	}

	private UsernamePasswordAuthenticationToken getAuthRequest(HttpServletRequest request) {
		String username = obtainUsername(request);
		String password = obtainPassword(request);
		//TODO require non-empty username and password
		return new UsernamePasswordAuthenticationToken(username, password);
	}
}
