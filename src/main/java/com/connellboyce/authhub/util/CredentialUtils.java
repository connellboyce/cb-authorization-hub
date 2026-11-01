package com.connellboyce.authhub.util;

import java.security.SecureRandom;
import java.util.Base64;

public class CredentialUtils {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int CLIENT_ID_BYTES = 16;
	private static final int CLIENT_SECRET_BYTES = 32;

	public static String generateClientId() {
		return randomBase64Url(CLIENT_ID_BYTES);
	}

	public static String generateClientSecret() {
		return randomBase64Url(CLIENT_SECRET_BYTES);
	}

	private static String randomBase64Url(int byteLength) {
		byte[] values = new byte[byteLength];
		SECURE_RANDOM.nextBytes(values);
		return Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(values);
	}
}
