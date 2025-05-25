package com.connellboyce.authhub.filter;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

public class AuthorizationRequestFilter implements Filter {
	private final Set<String> preservedParams;

	public AuthorizationRequestFilter(Set<String> parametersToCapture) {
		this.preservedParams = parametersToCapture;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpSession session = httpRequest.getSession();

		preservedParams.forEach(param -> {
			String value = httpRequest.getParameter(param);
			if (value != null) {
				session.setAttribute("auth_param_" + param, value);
			}
		});

		filterChain.doFilter(request, response);
	}
}
