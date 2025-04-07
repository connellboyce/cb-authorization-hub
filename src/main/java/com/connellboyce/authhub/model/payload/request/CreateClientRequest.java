package com.connellboyce.authhub.model.payload.request;

import lombok.Data;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class CreateClientRequest {
	private String clientId;
	private String clientSecret;
	private List<String> redirectUris;
	private List<String> scopes;
	private List<String> grantTypes;

	public RegisteredClient toRegisteredClient() {
		RegisteredClient.Builder clientBuilder = RegisteredClient.withId(String.valueOf(UUID.randomUUID()))
				.clientId(clientId)
				.clientIdIssuedAt(Instant.now())
				.clientSecret(clientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
		for (String scope : scopes) {
			clientBuilder.scope(scope);
		}
		for (String redirectUri : redirectUris) {
			clientBuilder.redirectUri(redirectUri);
		}
		for (String grantType : grantTypes) {
			clientBuilder.authorizationGrantType(new AuthorizationGrantType(grantType));
		}

		return clientBuilder.build();
	}

}
