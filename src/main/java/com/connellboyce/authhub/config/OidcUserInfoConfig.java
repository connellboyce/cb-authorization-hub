package com.connellboyce.authhub.config;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;

import java.util.Map;
import java.util.function.Function;

@Configuration
public class OidcUserInfoConfig {

	@Bean
	public Function<OidcUserInfoAuthenticationContext, OidcUserInfo> oidcUserInfoMapper(UserService userService) {
		return (context) -> {
			Authentication principal = context.getAuthentication();
			CBUser user = userService.getCBUserByUsername(principal.getName());
			return new OidcUserInfo(Map.of(
					"sub", user.getUsername(),
					"name", user.getFirstName() + " " + user.getLastName(),
					"email", user.getEmail(),
					"preferred_username", user.getUsername(),
					"given_name", user.getFirstName(),
					"family_name", user.getLastName()
			));
		};
	}
}
