package com.connellboyce.authhub.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialUtilsTest {
	private static final String BASE64_URL_PATTERN = "^[A-Za-z0-9_-]+$";

	@Test
	void generateClientId_is16BytesOfBase64Url() {
		String clientId = CredentialUtils.generateClientId();

		assertEquals(22, clientId.length());
		assertTrue(clientId.matches(BASE64_URL_PATTERN));
	}

	@Test
	void generateClientSecret_is32BytesOfBase64Url() {
		String secret = CredentialUtils.generateClientSecret();

		assertEquals(43, secret.length());
		assertTrue(secret.matches(BASE64_URL_PATTERN));
	}

	@Test
	void generatedValues_areUniqueAcrossCalls() {
		Set<String> clientIds = new HashSet<>();
		Set<String> secrets = new HashSet<>();
		IntStream.range(0, 1000).forEach(i -> {
			clientIds.add(CredentialUtils.generateClientId());
			secrets.add(CredentialUtils.generateClientSecret());
		});

		assertEquals(1000, clientIds.size());
		assertEquals(1000, secrets.size());
	}
}
