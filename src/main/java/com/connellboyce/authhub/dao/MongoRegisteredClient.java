package com.connellboyce.authhub.dao;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Getter
@Setter
@Document(collection = "clients")
public class MongoRegisteredClient {
	@Id
	private String id;
	private String clientId;
	private String clientSecret;
	private Set<String> clientAuthenticationMethods;
	private Set<String> authorizationGrantTypes;
	private Set<String> redirectUris;
	private Set<String> scopes;
	private boolean requireAuthorizationConsent;
}
