package com.connellboyce.authhub.grant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public enum TokenType {
	ACCESS_TOKEN(
			"urn:ietf:params:oauth:token-type:access_token",
			"urn:connellboyce:params:oauth:token-type:access_token"
	),
	ID_TOKEN(
			"urn:ietf:params:oauth:token-type:id_token",
			"urn:connellboyce:params:oauth:token-type:id_token"
	),
	REFRESH_TOKEN(
			"urn:ietf:params:oauth:token-type:refresh_token",
			"urn:connellboyce:params:oauth:token-type:refresh_token"
	);

	private final List<String> potentialValues;

	TokenType(String... values) {
		this.potentialValues = Collections.unmodifiableList(Arrays.asList(values));
	}

	public List<String> getPotentialValues() {
		return potentialValues;
	}

	public boolean matches(String value) {
		return potentialValues.contains(value);
	}

	public static Optional<TokenType> from(String value) {
		return Arrays.stream(values())
				.filter(type -> type.matches(value))
				.findFirst();
	}
}