package com.connellboyce.authhub.util;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.*;

class RsaUtilsTest {

	@Test
	void generateRsaKey_returnsValid2048BitRsaKeyPair() {
		KeyPair keyPair = RsaUtils.generateRsaKey();

		assertNotNull(keyPair);
		assertInstanceOf(RSAPublicKey.class, keyPair.getPublic());
		assertInstanceOf(RSAPrivateKey.class, keyPair.getPrivate());
		assertEquals(2048, ((RSAPublicKey) keyPair.getPublic()).getModulus().bitLength());
	}

	@Test
	void generateRsaKey_producesDistinctKeyPairsEachCall() {
		KeyPair first = RsaUtils.generateRsaKey();
		KeyPair second = RsaUtils.generateRsaKey();

		assertNotEquals(first.getPrivate(), second.getPrivate());
	}
}
