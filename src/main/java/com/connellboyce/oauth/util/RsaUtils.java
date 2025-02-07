package com.connellboyce.oauth.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class RsaUtils {

	/**
	 * Temporary method to generate an RSA Key Pair for JWT Signing.
	 * This will be replaced with an actual key pair at a later date.
	 *
	 * @return newly generated 2048-bit RSA key pair
	 */
	public static KeyPair generateRsaKey() {
		KeyPair keyPair;
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			keyPair = keyPairGenerator.generateKeyPair();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		return keyPair;
	}

}
