package com.connellboyce.authhub.model;

import java.util.Set;

public record ClientMetadata(
		String clientName,
		Set<String> redirectUris,
		Set<String> grantTypes,
		String tokenEndpointAuthMethod,
		Set<String> scopes
) {
}
