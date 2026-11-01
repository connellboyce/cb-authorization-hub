package com.connellboyce.authhub.model.payload.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ClientMetadataRequest(
		String clientId,
		String clientSecret,
		String clientName,
		List<String> redirectUris,
		List<String> grantTypes,
		List<String> responseTypes,
		String tokenEndpointAuthMethod,
		String scope
) {
}
