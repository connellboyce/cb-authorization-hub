package com.connellboyce.authhub.config;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;
import java.util.function.Function;

@Configuration
public class OidcUserInfoConfig {

	@Bean
	public Function<OidcUserInfoAuthenticationContext, OidcUserInfo> oidcUserInfoMapper(UserService userService) {
		return (context) -> {
			Authentication principal = context.getAuthentication();
			if (!(principal.getPrincipal() instanceof JwtAuthenticationToken)) {
				return new OidcUserInfo(Map.of());
			}
			String username = ((JwtAuthenticationToken) principal.getPrincipal()).getToken().getClaimAsString("username");
			if (username == null || username.isEmpty()) {
				return new OidcUserInfo(Map.of());
			}
			CBUser user = userService.getCBUserByUsername(username);
			return new OidcUserInfo(Map.of(
					"sub", user.getId(),
					"username", user.getUsername(),
					"name", user.getFirstName() + " " + user.getLastName(),
					"email", user.getEmail(),
					"preferred_username", user.getUsername(),
					"given_name", user.getFirstName(),
					"family_name", user.getLastName()
			));
		};
	}
}
