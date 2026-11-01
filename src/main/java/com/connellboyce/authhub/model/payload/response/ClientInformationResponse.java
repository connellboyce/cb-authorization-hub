package com.connellboyce.authhub.model.payload.response;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ClientInformationResponse(
		String clientId,
		String clientSecret,
		Long clientIdIssuedAt,
		long clientSecretExpiresAt,
		String registrationClientUri,
		String clientName,
		List<String> redirectUris,
		List<String> grantTypes,
		List<String> responseTypes,
		String tokenEndpointAuthMethod,
		String scope
) {
	public static ClientInformationResponse from(MongoRegisteredClient client, String clientSecret, String registrationClientUri) {
		return new ClientInformationResponse(
				client.getClientId(),
				clientSecret,
				Optional.ofNullable(client.getClientIdIssuedAt()).map(Instant::getEpochSecond).orElse(null),
				0,
				registrationClientUri,
				client.getClientName(),
				client.getRedirectUris().stream().sorted().toList(),
				client.getAuthorizationGrantTypes().stream().sorted().toList(),
				client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue()) ? List.of("code") : List.of(),
				client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue())
						? ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue()
						: client.getClientAuthenticationMethods().iterator().next(),
				client.getScopes().isEmpty() ? null : String.join(" ", client.getScopes().stream().sorted().toList())
		);
	}
}
