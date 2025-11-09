package com.connellboyce.authhub.grant;

import org.springframework.security.oauth2.core.AuthorizationGrantType;

public final class AdditionalGrantTypes {
	public static final AuthorizationGrantType CIBA =
			new AuthorizationGrantType("urn:openid:params:grant-type:ciba");

	private AdditionalGrantTypes() {}
}
